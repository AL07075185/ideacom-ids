'use strict';
(() => {
  IDS.requiereSesion();
  IDS.barra('dashboard.html');

  function metrica(titulo, valor, icono, color, enlace) {
    const contenido = `<div class="card tarjeta-metrica shadow-sm h-100"><div class="card-body d-flex align-items-center gap-3">
        <i class="bi ${icono} fs-2 text-${color}"></i>
        <div><div class="valor">${IDS.esc(valor)}</div><div class="etiqueta">${IDS.esc(titulo)}</div></div>
      </div></div>`;
    return `<div class="col-6 col-md-4 col-xl">${enlace ? `<a href="${enlace}" class="text-decoration-none text-reset">${contenido}</a>` : contenido}</div>`;
  }

  async function cargar() {
    try {
      const r = await IDS.get('/dashboard/resumen');
      const e = r.porEstado || {};
      const disp = r.disponibilidad24h === null || r.disponibilidad24h === undefined ? '—' : r.disponibilidad24h.toFixed(1) + '%';
      document.getElementById('metricas').innerHTML =
        metrica('Equipos aprobados', r.totalDispositivos, 'bi-pc-display', 'primary', 'dispositivos.html') +
        metrica('En línea', e.EN_LINEA || 0, 'bi-check-circle', 'success') +
        metrica('Sin internet', e.SIN_INTERNET || 0, 'bi-wifi-off', 'warning') +
        metrica('Desconectados', e.DESCONECTADO || 0, 'bi-x-octagon', 'danger') +
        metrica('Apagados', e.APAGADO || 0, 'bi-power', 'secondary') +
        metrica('Disponibilidad 24 h', disp, 'bi-graph-up', 'info') +
        metrica('Incidencias abiertas', r.incidenciasAbiertas, 'bi-life-preserver', 'danger', 'incidencias.html');

      const aviso = document.getElementById('avisoPendientes');
      aviso.classList.toggle('d-none', !r.pendientes);
      document.getElementById('numPendientes').textContent = r.pendientes;

      const filas = (r.ultimosEventos || []).map(ev => `<tr>
          <td class="text-nowrap">${IDS.fecha(ev.fechaHora)}</td>
          <td>${IDS.esc(ev.dispositivo)}</td>
          <td>${IDS.esc(IDS.nombreEvento(ev.tipo))}</td>
          <td>${ev.estadoResultante ? IDS.badgeEstado(ev.estadoResultante) : ''}</td>
          <td class="small text-secondary">${IDS.esc(ev.detalle)}</td></tr>`).join('');
      document.getElementById('eventos').innerHTML = filas || '<tr><td colspan="5" class="text-secondary">Sin eventos todavia.</td></tr>';
      document.getElementById('actualizado').textContent = 'Actualizado ' + new Date().toLocaleTimeString('es-MX');
    } catch (e) {
      IDS.error(e);
    }
  }

  cargar();
  setInterval(cargar, window.IDS_CONFIG.refrescoSegundos * 1000);
})();
