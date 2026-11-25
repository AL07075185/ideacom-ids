/* ideacom IDS - utilidades comunes del panel */
'use strict';

const IDS = (() => {
  const CLAVE = 'ids.sesion';

  // ------------------------------------------------------------ sesión
  function sesion() {
    try {
      const s = JSON.parse(sessionStorage.getItem(CLAVE));
      if (s && s.token && new Date(s.expira) > new Date()) return s;
    } catch (e) { /* sesión corrupta */ }
    return null;
  }

  function guardarSesion(datos) { sessionStorage.setItem(CLAVE, JSON.stringify(datos)); }
  function cerrarSesionLocal() { sessionStorage.removeItem(CLAVE); }
  function esAdmin() { const s = sesion(); return !!s && s.rol === 'ADMIN'; }

  /** Redirige al login si no hay sesión. Si soloAdmin, al dashboard. */
  function requiereSesion(soloAdmin) {
    const s = sesion();
    if (!s) { location.replace('index.html'); throw new Error('sin sesión'); }
    if (soloAdmin && s.rol !== 'ADMIN') { location.replace('dashboard.html'); throw new Error('sin permiso'); }
    return s;
  }

  // ------------------------------------------------------------ API
  async function api(metodo, ruta, cuerpo) {
    const s = sesion();
    const opciones = { method: metodo, headers: { 'Accept': 'application/json' } };
    if (s) opciones.headers['Authorization'] = 'Bearer ' + s.token;
    if (cuerpo !== undefined) {
      opciones.headers['Content-Type'] = 'application/json';
      opciones.body = JSON.stringify(cuerpo);
    }
    let r;
    try {
      r = await fetch(window.IDS_CONFIG.apiBase + ruta, opciones);
    } catch (e) {
      throw new Error('No hay comunicación con la API');
    }
    return procesar(r, ruta);
  }

  /** Envía texto tal cual (CSV o JSON leído de un archivo) y devuelve la respuesta JSON. */
  async function enviarTexto(metodo, ruta, texto, tipo) {
    const s = sesion();
    let r;
    try {
      r = await fetch(window.IDS_CONFIG.apiBase + ruta, {
        method: metodo,
        headers: { 'Accept': 'application/json', 'Content-Type': tipo + '; charset=UTF-8',
          'Authorization': 'Bearer ' + (s ? s.token : '') },
        body: texto
      });
    } catch (e) {
      throw new Error('No hay comunicación con la API');
    }
    return procesar(r, ruta);
  }

  async function procesar(r, ruta) {
    if (r.status === 401 && !ruta.startsWith('/auth/login')) {
      cerrarSesionLocal();
      location.replace('index.html?expirada=1');
      throw new Error('Sesión expirada');
    }
    if (r.status === 204) return null;
    const texto = await r.text();
    let datos = null;
    try { datos = texto ? JSON.parse(texto) : null; } catch (e) { datos = null; }
    if (!r.ok) throw new Error((datos && datos.mensaje) || ('Error HTTP ' + r.status));
    return datos;
  }

  /** Descarga un archivo de la API (con el token) y lo guarda en el equipo. */
  async function descargar(ruta, nombrePorDefecto) {
    const s = sesion();
    let r;
    try {
      r = await fetch(window.IDS_CONFIG.apiBase + ruta, { headers: { 'Authorization': 'Bearer ' + (s ? s.token : '') } });
    } catch (e) {
      throw new Error('No hay comunicación con la API');
    }
    if (!r.ok) { await procesar(r, ruta); return; }
    const m = /filename="([^"]+)"/.exec(r.headers.get('Content-Disposition') || '');
    guardarArchivo(await r.blob(), m ? m[1] : nombrePorDefecto);
  }

  function guardarArchivo(blob, nombre) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }

  const get = (ruta) => api('GET', ruta);
  const post = (ruta, cuerpo) => api('POST', ruta, cuerpo === undefined ? {} : cuerpo);
  const put = (ruta, cuerpo) => api('PUT', ruta, cuerpo);
  const del = (ruta) => api('DELETE', ruta);

  // ------------------------------------------------------------ formato
  function esc(v) {
    if (v === null || v === undefined) return '';
    return String(v).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  function fecha(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-MX', { dateStyle: 'short', timeStyle: 'medium' });
  }

  function hace(iso) {
    if (!iso) return 'nunca';
    const s = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
    if (s < 60) return 'hace ' + Math.max(s, 0) + ' s';
    if (s < 3600) return 'hace ' + Math.floor(s / 60) + ' min';
    if (s < 86400) return 'hace ' + Math.floor(s / 3600) + ' h';
    return 'hace ' + Math.floor(s / 86400) + ' d';
  }

  const ESTADOS = {
    EN_LINEA: ['En línea', 'success', '#198754'],
    SIN_INTERNET: ['Sin internet', 'warning', '#e0a800'],
    APAGADO: ['Apagado', 'secondary', '#6c757d'],
    DESCONECTADO: ['Desconectado', 'danger', '#dc3545'],
    SIN_AGENTE: ['Sin agente', 'info', '#0dcaf0'],
    DESCONOCIDO: ['Sin reportar', 'light', '#adb5bd']
  };

  function estado(e) { return ESTADOS[e] || [e, 'light', '#adb5bd']; }

  function badgeEstado(e) {
    const [texto, clase] = estado(e);
    const extra = clase === 'light' ? ' text-dark border' : (clase === 'warning' || clase === 'info' ? ' text-dark' : '');
    return `<span class="badge text-bg-${clase}${extra}">${esc(texto)}</span>`;
  }

  function badgeRegistro(r) {
    const m = { PENDIENTE: 'warning text-dark', APROBADO: 'success', RECHAZADO: 'danger' };
    return `<span class="badge text-bg-${m[r] || 'secondary'}">${esc(r)}</span>`;
  }

  const TIPOS = { PC: 'bi-pc-display', SERVIDOR: 'bi-hdd-rack', RED: 'bi-router', CAMARA: 'bi-camera-video',
    NVR: 'bi-hdd-network', IMPRESORA: 'bi-printer', OTRO: 'bi-box' };
  function iconoTipo(t) { return `<i class="bi ${TIPOS[t] || 'bi-box'}" title="${esc(t)}"></i>`; }

  const EVENTOS = {
    REGISTRO: 'Alta del equipo', APROBADO: 'Alta aprobada', RECHAZADO: 'Alta rechazada', ARRANQUE: 'Agente iniciado',
    APAGADO: 'Apagado', RECONEXION: 'Reconexión', DESCONEXION_INESPERADA: 'Desconexión inesperada',
    SIN_INTERNET: 'Perdio internet', INTERNET_RESTABLECIDO: 'Internet restablecido',
    SIN_CONEXION_SERVIDOR: 'Periodo sin comunicación', CAMBIO_IP: 'Cambio de IP'
  };
  function nombreEvento(t) { return EVENTOS[t] || t; }

  // ------------------------------------------------------------ interfaz
  function aviso(mensaje, tipo = 'success') {
    let cont = document.getElementById('avisos');
    if (!cont) {
      cont = document.createElement('div');
      cont.id = 'avisos';
      cont.className = 'toast-container position-fixed bottom-0 end-0 p-3';
      document.body.appendChild(cont);
    }
    const el = document.createElement('div');
    el.className = `toast align-items-center text-bg-${tipo} border-0`;
    el.setAttribute('role', 'alert');
    el.innerHTML = `<div class="d-flex"><div class="toast-body">${esc(mensaje)}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    cont.appendChild(el);
    const t = new bootstrap.Toast(el, { delay: 4000 });
    el.addEventListener('hidden.bs.toast', () => el.remove());
    t.show();
  }

  const error = (e) => aviso(e.message || String(e), 'danger');

  /** Barra de navegacion comun. */
  function barra(activa) {
    const s = sesion();
    const items = [
      ['dashboard.html', 'bi-speedometer2', 'Dashboard'],
      ['dispositivos.html', 'bi-pc-display', 'Dispositivos'],
      ['mapa.html', 'bi-diagram-3', 'Mapa'],
      ['incidencias.html', 'bi-life-preserver', 'Incidencias']
    ];
    if (s && s.rol === 'ADMIN') items.push(['admin.html', 'bi-gear', 'Administración']);
    const enlaces = items.map(([href, icono, texto]) =>
      `<li class="nav-item"><a class="nav-link${href === activa ? ' active' : ''}" href="${href}">
        <i class="bi ${icono}"></i> ${texto}</a></li>`).join('');
    document.getElementById('barra').innerHTML = `
      <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container-fluid">
          <a class="navbar-brand fw-semibold" href="dashboard.html"><i class="bi bi-activity"></i> ideacom IDS</a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menu">
            <span class="navbar-toggler-icon"></span></button>
          <div class="collapse navbar-collapse" id="menu">
            <ul class="navbar-nav me-auto">${enlaces}</ul>
            <button class="btn btn-outline-info btn-sm me-3" id="btnReporte" title="Reporte imprimible de disponibilidad">
              <i class="bi bi-printer"></i> Reporte</button>
            <span class="navbar-text me-3 small"><i class="bi bi-person-circle"></i> ${esc(s ? s.nombre : '')}
              <span class="badge text-bg-secondary ms-1">${esc(s ? s.rol : '')}</span></span>
            <button class="btn btn-outline-light btn-sm" id="btnSalir"><i class="bi bi-box-arrow-right"></i> Salir</button>
          </div>
        </div>
      </nav>`;
    document.getElementById('btnReporte').addEventListener('click', abrirReporte);
    document.getElementById('btnSalir').addEventListener('click', async () => {
      try { await post('/auth/logout'); } catch (e) { /* se cierra igual */ }
      cerrarSesionLocal();
      location.replace('index.html');
    });
  }

  /**
   * Abre el reporte JSP en una pestaña nueva. La pestaña se abre antes de pedir
   * el ticket para que el navegador no la bloquee como ventana emergente.
   */
  async function abrirReporte() {
    const ventana = window.open('', '_blank');
    try {
      const r = await post('/reportes/ticket');
      const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
      const url = window.IDS_CONFIG.reporteUrl + '?ticket=' + encodeURIComponent(r.ticket)
        + '&horas=24&tz=' + encodeURIComponent(tz);
      if (ventana) ventana.location = url; else location.href = url;
    } catch (e) {
      if (ventana) ventana.close();
      error(e);
    }
  }

  /** Llena un <select> con opciones [{valor, texto}]. */
  function opciones(select, lista, vacio) {
    select.innerHTML = (vacio !== undefined ? `<option value="">${esc(vacio)}</option>` : '') +
      lista.map(o => `<option value="${esc(o.valor)}">${esc(o.texto)}</option>`).join('');
  }

  function parametro(nombre) { return new URLSearchParams(location.search).get(nombre); }

  return { sesion, guardarSesion, cerrarSesionLocal, esAdmin, requiereSesion, api, get, post, put, del,
    enviarTexto, descargar, guardarArchivo,
    esc, fecha, hace, estado, badgeEstado, badgeRegistro, iconoTipo, nombreEvento, aviso, error, barra,
    opciones, parametro, ESTADOS };
})();
