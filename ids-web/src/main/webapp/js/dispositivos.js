'use strict';
(() => {
  IDS.requiereSesion();
  IDS.barra('dispositivos.html');
  const admin = IDS.esAdmin();
  document.querySelectorAll('.solo-admin').forEach(el => el.classList.toggle('d-none', !admin));

  const modalDetalle = new bootstrap.Modal('#modalDetalle');
  const modalEditar = new bootstrap.Modal('#modalEditar');
  const modalImportar = new bootstrap.Modal('#modalImportar');
  let idDetalle = null;
  const $ = (id) => document.getElementById(id);
  let lista = [];
  let catalogos = null;

  const filtro = $('filtroRegistro');
  if (IDS.parametro('registro')) filtro.value = IDS.parametro('registro');

  async function cargar() {
    try {
      const reg = filtro.value;
      lista = await IDS.get('/dispositivos' + (reg ? '?registro=' + reg : ''));
      pintar();
    } catch (e) { IDS.error(e); }
  }

  function pintar() {
    const q = $('buscar').value.trim().toLowerCase();
    const visibles = lista.filter(d => !q || [d.nombre, d.ip, d.ipv6, d.mac, d.hostname, d.soNombre]
      .some(v => v && v.toLowerCase().includes(q)));
    $('tabla').innerHTML = visibles.map(d => {
      let acciones = `<button class="btn btn-sm btn-outline-secondary" data-ver="${d.id}" title="Ver detalle"><i class="bi bi-eye"></i></button>`;
      if (admin) {
        acciones += ` <button class="btn btn-sm btn-outline-primary" data-editar="${d.id}" title="Editar"><i class="bi bi-pencil"></i></button>`;
        if (d.estadoRegistro !== 'APROBADO') acciones += ` <button class="btn btn-sm btn-success" data-aprobar="${d.id}" title="Aprobar"><i class="bi bi-check-lg"></i></button>`;
        if (d.estadoRegistro === 'PENDIENTE') acciones += ` <button class="btn btn-sm btn-outline-warning" data-rechazar="${d.id}" title="Rechazar"><i class="bi bi-slash-circle"></i></button>`;
        acciones += ` <button class="btn btn-sm btn-outline-danger" data-eliminar="${d.id}" title="Eliminar"><i class="bi bi-trash"></i></button>`;
      }
      return `<tr>
        <td>${IDS.badgeEstado(d.estadoActual)}</td>
        <td class="fw-semibold">${IDS.esc(d.nombre)}<div class="small text-secondary">${IDS.esc(d.hostname || '')}</div></td>
        <td>${IDS.iconoTipo(d.tipo)} <span class="small">${IDS.esc(d.tipo)}</span></td>
        <td class="codigo small">${IDS.esc(d.ip || '—')}${d.ipv6 ? `<div class="text-secondary" title="IPv6">${IDS.esc(d.ipv6)}</div>` : ''}</td>
        <td class="small">${IDS.esc(d.soNombre || (d.conAgente ? '' : 'Sin agente'))}</td>
        <td class="small">${IDS.esc(d.departamento || '—')}</td>
        <td class="small">${IDS.esc(d.responsable || '—')}</td>
        <td class="small" title="${IDS.esc(IDS.fecha(d.ultimoLatido))}">${d.conAgente ? IDS.hace(d.ultimoLatido) : '—'}</td>
        <td>${IDS.badgeRegistro(d.estadoRegistro)}</td>
        <td class="text-end text-nowrap">${acciones}</td></tr>`;
    }).join('') || '<tr><td colspan="10" class="text-secondary">No hay dispositivos con ese filtro.</td></tr>';
  }

  // ------------------------------------------------------------ detalle
  async function verDetalle(id) {
    try {
      const [d, eventos, d24, d7] = await Promise.all([
        IDS.get('/dispositivos/' + id), IDS.get('/dispositivos/' + id + '/eventos?limite=100'),
        IDS.get('/dispositivos/' + id + '/disponibilidad?horas=24'), IDS.get('/dispositivos/' + id + '/disponibilidad?horas=168')]);
      $('detTitulo').innerHTML = `${IDS.iconoTipo(d.tipo)} ${IDS.esc(d.nombre)} ${IDS.badgeEstado(d.estadoActual)}`;
      const campos = [
        ['Sitio', d.sitio], ['Hostname', d.hostname], ['IP', d.ip], ['IPv6', d.ipv6], ['MAC', d.mac],
        ['Sistema', [d.soNombre, d.soVersion].filter(Boolean).join(' ')], ['Arquitectura', d.arquitectura],
        ['CPU', d.cpuModelo], ['RAM', d.ramMb ? (d.ramMb / 1024).toFixed(1) + ' GB' : null],
        ['Disco', d.discoGb ? d.discoGb + ' GB' : null], ['Departamento', d.departamento],
        ['Responsable', d.responsable], ['Conectado a', d.conectadoA], ['Ubicación', d.ubicacion],
        ['Internet', d.internetOk == null ? null : (d.internetOk ? 'Sí' : 'No')],
        ['Último reporte', d.ultimoLatido ? IDS.fecha(d.ultimoLatido) + ' (' + IDS.hace(d.ultimoLatido) + ')' : null],
        ['Agente', d.versionAgente], ['Registro', d.estadoRegistro], ['Alta', IDS.fecha(d.fechaAlta)], ['Notas', d.notas]];
      $('detDatos').innerHTML = campos.filter(([, v]) => v).map(([k, v]) =>
        `<dt class="col-sm-4">${k}</dt><dd class="col-sm-8">${IDS.esc(v)}</dd>`).join('');
      $('disp24').textContent = d24.porcentaje == null ? '—' : d24.porcentaje.toFixed(1) + ' %';
      $('disp7d').textContent = d7.porcentaje == null ? '—' : d7.porcentaje.toFixed(1) + ' %';
      $('detEventos').innerHTML = eventos.map(ev => `<tr>
          <td class="text-nowrap small">${IDS.fecha(ev.fechaHora)}</td>
          <td class="small">${IDS.esc(IDS.nombreEvento(ev.tipo))}</td>
          <td>${ev.estadoResultante ? IDS.badgeEstado(ev.estadoResultante) : ''}</td>
          <td class="small text-secondary">${IDS.esc(ev.detalle)}${ev.fechaFin ? ' (hasta ' + IDS.fecha(ev.fechaFin) + ')' : ''}</td></tr>`).join('')
        || '<tr><td colspan="4" class="text-secondary">Sin eventos.</td></tr>';
      $('detIncidencia').href = 'incidencias.html?dispositivo=' + d.id;
      idDetalle = d.id;
      modalDetalle.show();
    } catch (e) { IDS.error(e); }
  }

  // ------------------------------------------------------------ edicion
  async function cargarCatalogos() {
    if (catalogos) return catalogos;
    const [departamentos, usuarios, sitios, todos] = await Promise.all([
      IDS.get('/catalogos/departamentos'), IDS.get('/catalogos/usuarios'), IDS.get('/catalogos/sitios'),
      IDS.get('/dispositivos?registro=APROBADO')]);
    catalogos = { departamentos, usuarios: usuarios.filter(u => u.activo), sitios, todos };
    return catalogos;
  }

  async function abrirEdicion(id) {
    try {
      catalogos = null;
      const c = await cargarCatalogos();
      const d = id ? await IDS.get('/dispositivos/' + id) : null;
      const manual = !d || !d.conAgente;
      document.querySelectorAll('#formEditar .manual').forEach(el => el.classList.toggle('d-none', !manual));
      $('edSitio').disabled = !!d;
      IDS.opciones($('edSitio'), c.sitios.map(s => ({ valor: s.id, texto: s.nombre })));
      IDS.opciones($('edDepartamento'), c.departamentos.map(x => ({ valor: x.id, texto: x.nombre })), '(sin departamento)');
      IDS.opciones($('edResponsable'), c.usuarios.map(u => ({ valor: u.id, texto: u.nombre + ' (' + u.email + ')' })), '(sin responsable)');
      IDS.opciones($('edConectado'), c.todos.filter(x => !d || x.id !== d.id)
        .map(x => ({ valor: x.id, texto: x.nombre + (x.ip ? ' - ' + x.ip : '') })), '(ninguno)');
      $('edTitulo').textContent = d ? 'Editar ' + d.nombre : 'Agregar equipo sin agente';
      $('edAyuda').textContent = d && d.conAgente
        ? 'Los datos técnicos (IP, SO, hardware) los actualiza el agente automáticamente.'
        : 'Equipos como switches, routers, cámaras o impresoras se agregan a mano para que aparezcan en el mapa.';
      $('edId').value = d ? d.id : '';
      $('edNombre').value = d ? d.nombre : '';
      $('edTipo').value = d ? d.tipo : 'RED';
      $('edSitio').value = d ? d.idSitio : (c.sitios[0] ? c.sitios[0].id : '');
      $('edDepartamento').value = d && d.idDepartamento ? d.idDepartamento : '';
      $('edResponsable').value = d && d.idResponsable ? d.idResponsable : '';
      $('edConectado').value = d && d.idConectadoA ? d.idConectadoA : '';
      $('edUbicacion').value = d ? (d.ubicacion || '') : '';
      $('edIp').value = d ? (d.ip || '') : '';
      $('edIpv6').value = d ? (d.ipv6 || '') : '';
      $('edMac').value = d ? (d.mac || '') : '';
      $('edNotas').value = d ? (d.notas || '') : '';
      modalEditar.show();
    } catch (e) { IDS.error(e); }
  }

  const numero = (v) => v === '' ? null : Number(v);

  $('formEditar').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const id = $('edId').value;
    const cuerpo = {
      nombre: $('edNombre').value, tipo: $('edTipo').value, ubicacion: $('edUbicacion').value,
      notas: $('edNotas').value, idSitio: numero($('edSitio').value), idDepartamento: numero($('edDepartamento').value),
      idResponsable: numero($('edResponsable').value), idConectadoA: numero($('edConectado').value),
      ip: $('edIp').value, ipv6: $('edIpv6').value, mac: $('edMac').value
    };
    try {
      if (id) await IDS.put('/dispositivos/' + id, cuerpo);
      else await IDS.post('/dispositivos', cuerpo);
      modalEditar.hide();
      IDS.aviso('Dispositivo guardado');
      cargar();
    } catch (e) { IDS.error(e); }
  });

  // ------------------------------------------------------------ acciones
  $('tabla').addEventListener('click', async (ev) => {
    const b = ev.target.closest('button');
    if (!b) return;
    const d = b.dataset;
    try {
      if (d.ver) return verDetalle(d.ver);
      if (d.editar) return abrirEdicion(d.editar);
      if (d.aprobar) { await IDS.post('/dispositivos/' + d.aprobar + '/aprobar'); IDS.aviso('Equipo aprobado'); }
      if (d.rechazar) {
        if (!confirm('¿Rechazar el alta? El agente quedará bloqueado hasta que lo apruebes.')) return;
        await IDS.post('/dispositivos/' + d.rechazar + '/rechazar'); IDS.aviso('Alta rechazada', 'warning');
      }
      if (d.eliminar) {
        if (!confirm('¿Eliminar el dispositivo y todo su historial?')) return;
        await IDS.del('/dispositivos/' + d.eliminar); IDS.aviso('Dispositivo eliminado');
      }
      cargar();
    } catch (e) { IDS.error(e); }
  });

  // ------------------------------------------------------------ exportar
  document.querySelectorAll('[data-exportar]').forEach(el => el.addEventListener('click', async (ev) => {
    ev.preventDefault();
    const formato = el.dataset.exportar;
    const reg = filtro.value;
    try {
      await IDS.descargar('/datos/dispositivos?formato=' + formato + (reg ? '&registro=' + reg : ''),
        'ids-dispositivos.' + formato);
    } catch (e) { IDS.error(e); }
  }));

  $('detExportar').addEventListener('click', async () => {
    try { await IDS.descargar('/datos/dispositivos/' + idDetalle + '/eventos?formato=csv&dias=30', 'ids-historial.csv'); }
    catch (e) { IDS.error(e); }
  });

  // ------------------------------------------------------------ importar
  let contenidoImportar = null;
  let tipoImportar = null;

  $('btnImportar').addEventListener('click', () => {
    $('archivoImportar').value = '';
    $('resultadoImportar').innerHTML = '';
    $('btnRevisar').disabled = true;
    $('btnAplicarImportacion').disabled = true;
    contenidoImportar = null;
    modalImportar.show();
  });

  $('archivoImportar').addEventListener('change', async (ev) => {
    const archivo = ev.target.files[0];
    $('resultadoImportar').innerHTML = '';
    $('btnAplicarImportacion').disabled = true;
    if (!archivo) { $('btnRevisar').disabled = true; return; }
    contenidoImportar = await archivo.text();
    tipoImportar = /\.json$/i.test(archivo.name) || contenidoImportar.trim().startsWith('[') ? 'application/json' : 'text/csv';
    $('btnRevisar').disabled = false;
  });

  function pintarResultado(r) {
    const resumen = `<div class="d-flex flex-wrap gap-2 mb-2">
        <span class="badge text-bg-${r.simulado ? 'info' : 'success'}">${r.simulado ? 'Vista previa' : 'Importación terminada'}</span>
        <span class="badge text-bg-light border">${r.filas} fila(s)</span>
        <span class="badge text-bg-success">${r.creados} nuevo(s)</span>
        <span class="badge text-bg-primary">${r.actualizados} actualizado(s)</span>
        <span class="badge text-bg-${r.errores ? 'danger' : 'light border'}">${r.errores} con error</span></div>`;
    const avisos = (r.avisos || []).length
      ? `<div class="alert alert-warning py-2 small mb-2">${r.avisos.map(IDS.esc).join('<br>')}</div>` : '';
    const ACC = { CREAR: ['Crear', 'success'], ACTUALIZAR: ['Actualizar', 'primary'], ERROR: ['Error', 'danger'] };
    const filas = (r.detalles || []).map(d => `<tr><td>${d.fila}</td><td>${IDS.esc(d.nombre || '')}</td>
        <td><span class="badge text-bg-${ACC[d.accion][1]}">${ACC[d.accion][0]}</span></td>
        <td class="small text-secondary">${IDS.esc(d.mensaje || '')}</td></tr>`).join('');
    $('resultadoImportar').innerHTML = resumen + avisos + `<div class="table-responsive" style="max-height:300px">
        <table class="table table-sm tabla-compacta"><thead class="table-light"><tr><th>Fila</th><th>Nombre</th><th>Acción</th><th>Detalle</th></tr></thead>
        <tbody>${filas}</tbody></table></div>`;
  }

  async function enviarImportacion(simular) {
    try {
      const r = await IDS.enviarTexto('POST', '/datos/dispositivos/importar?simular=' + simular, contenidoImportar, tipoImportar);
      pintarResultado(r);
      const validas = r.creados + r.actualizados;
      $('btnAplicarImportacion').disabled = !simular || validas === 0;
      $('btnAplicarImportacion').textContent = r.errores ? `Importar ${validas} fila(s) válidas` : 'Importar';
      if (!simular) { IDS.aviso('Importación terminada'); cargar(); }
    } catch (e) {
      $('resultadoImportar').innerHTML = `<div class="alert alert-danger py-2">${IDS.esc(e.message)}</div>`;
    }
  }

  $('btnRevisar').addEventListener('click', () => enviarImportacion(true));
  $('btnAplicarImportacion').addEventListener('click', () => enviarImportacion(false));

  $('lnkPlantilla').addEventListener('click', (ev) => {
    ev.preventDefault();
    const csv = '\ufeffnombre,tipo,sitio,departamento,responsable,conectadoA,ubicacion,ip,ipv6,mac,notas\r\n'
      + 'Router-ISP,RED,,,,,Site principal,192.168.1.1,,,Router del proveedor\r\n'
      + 'Switch-01,RED,,Sistemas,,Router-ISP,Rack A,192.168.1.2,,AA:BB:CC:00:00:02,\r\n'
      + 'Cámara Entrada,CAMARA,,Seguridad,,Switch-01,Acceso principal,192.168.1.21,,AA:BB:CC:00:00:21,\r\n';
    IDS.guardarArchivo(new Blob([csv], { type: 'text/csv' }), 'plantilla-dispositivos.csv');
  });

  $('btnNuevo').addEventListener('click', () => abrirEdicion(null));
  filtro.addEventListener('change', cargar);
  $('buscar').addEventListener('input', pintar);

  cargar();
  setInterval(cargar, window.IDS_CONFIG.refrescoSegundos * 1000);
})();
