'use strict';
(() => {
  IDS.requiereSesion();
  IDS.barra('incidencias.html');
  const admin = IDS.esAdmin();
  const $ = (id) => document.getElementById(id);
  const modal = new bootstrap.Modal('#modalNueva');
  let usuarios = [];

  const PRIORIDAD = { BAJA: 'secondary', MEDIA: 'warning text-dark', ALTA: 'danger' };
  const ESTADO = { ABIERTA: 'danger', EN_PROCESO: 'primary', CERRADA: 'success' };

  function selectEstado(i) {
    return `<select class="form-select form-select-sm" data-estado="${i.id}" style="width:auto">` +
      ['ABIERTA', 'EN_PROCESO', 'CERRADA'].map(e => `<option ${e === i.estado ? 'selected' : ''}>${e}</option>`).join('') + '</select>';
  }

  function selectAsignar(i) {
    return `<select class="form-select form-select-sm" data-asignar="${i.id}" style="width:auto"><option value="0">(sin asignar)</option>` +
      usuarios.map(u => `<option value="${u.id}" ${u.id === i.idAsignadaA ? 'selected' : ''}>${IDS.esc(u.nombre)}</option>`).join('') + '</select>';
  }

  async function cargar() {
    try {
      const estado = $('filtroEstado').value;
      const lista = await IDS.get('/incidencias' + (estado ? '?estado=' + estado : ''));
      $('tabla').innerHTML = lista.map(i => `<tr>
          <td>${i.id}</td>
          <td><div class="fw-semibold">${IDS.esc(i.titulo)}</div><div class="small text-secondary">${IDS.esc(i.descripcion || '')}</div></td>
          <td class="small">${IDS.esc(i.dispositivo || '—')}</td>
          <td><span class="badge text-bg-${PRIORIDAD[i.prioridad]}">${i.prioridad}</span></td>
          <td>${admin ? selectEstado(i) : `<span class="badge text-bg-${ESTADO[i.estado]}">${i.estado}</span>`}</td>
          <td class="small">${IDS.esc(i.creadaPor)}</td>
          <td class="small">${admin ? selectAsignar(i) : IDS.esc(i.asignadaA || '—')}</td>
          <td class="small text-nowrap">${IDS.fecha(i.fechaCreacion)}${i.fechaCierre ? '<div class="text-success">Cerrada ' + IDS.fecha(i.fechaCierre) + '</div>' : ''}</td>
        </tr>`).join('') || '<tr><td colspan="8" class="text-secondary">No hay incidencias.</td></tr>';
    } catch (e) { IDS.error(e); }
  }

  async function abrirNueva(idDispositivo) {
    try {
      const equipos = await IDS.get('/dispositivos?registro=APROBADO');
      IDS.opciones($('inDispositivo'), equipos.map(d => ({ valor: d.id, texto: d.nombre + (d.ip ? ' - ' + d.ip : '') })), '(general, sin equipo)');
      $('inDispositivo').value = idDispositivo || '';
      $('formNueva').reset();
      $('inDispositivo').value = idDispositivo || '';
      modal.show();
    } catch (e) { IDS.error(e); }
  }

  $('formNueva').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      const disp = $('inDispositivo').value;
      await IDS.post('/incidencias', {
        titulo: $('inTitulo').value, descripcion: $('inDescripcion').value,
        prioridad: $('inPrioridad').value, idDispositivo: disp ? Number(disp) : null
      });
      modal.hide();
      IDS.aviso('Incidencia registrada');
      history.replaceState(null, '', 'incidencias.html');
      cargar();
    } catch (e) { IDS.error(e); }
  });

  $('tabla').addEventListener('change', async (ev) => {
    const el = ev.target;
    try {
      if (el.dataset.estado) await IDS.put('/incidencias/' + el.dataset.estado, { estado: el.value });
      if (el.dataset.asignar) await IDS.put('/incidencias/' + el.dataset.asignar, { idAsignadaA: Number(el.value) });
      IDS.aviso('Incidencia actualizada');
      cargar();
    } catch (e) { IDS.error(e); }
  });

  document.querySelectorAll('[data-exportar]').forEach(el => el.addEventListener('click', async (ev) => {
    ev.preventDefault();
    const estado = $('filtroEstado').value;
    try {
      await IDS.descargar('/datos/incidencias?formato=' + el.dataset.exportar + (estado ? '&estado=' + estado : ''),
        'ids-incidencias.' + el.dataset.exportar);
    } catch (e) { IDS.error(e); }
  }));

  $('btnNueva').addEventListener('click', () => abrirNueva(null));
  $('filtroEstado').addEventListener('change', cargar);

  (async () => {
    if (admin) {
      try { usuarios = (await IDS.get('/catalogos/usuarios')).filter(u => u.activo); } catch (e) { IDS.error(e); }
    }
    await cargar();
    const desde = IDS.parametro('dispositivo');
    if (desde) abrirNueva(Number(desde));
  })();
})();
