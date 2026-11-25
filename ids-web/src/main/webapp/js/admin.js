'use strict';
(() => {
  const yo = IDS.requiereSesion(true);
  IDS.barra('admin.html');
  const $ = (id) => document.getElementById(id);

  // ------------------------------------------------------------ sitios
  async function sitios() {
    const lista = await IDS.get('/catalogos/sitios');
    $('tSitios').innerHTML = lista.map(s => `<tr>
        <td>${IDS.esc(s.nombre)}<div class="small text-secondary">${IDS.esc(s.direccion || '')}</div></td>
        <td><span class="codigo">${IDS.esc(s.codigoAlta)}</span>
          <button class="btn btn-link btn-sm p-0 ms-1" data-copiar="${IDS.esc(s.codigoAlta)}" title="Copiar"><i class="bi bi-clipboard"></i></button></td>
        <td>${s.dispositivos}</td>
        <td><div class="form-check form-switch mb-0"><input class="form-check-input" type="checkbox" data-sitio-activo="${s.id}"
          data-nombre="${IDS.esc(s.nombre)}" data-direccion="${IDS.esc(s.direccion || '')}" ${s.activo ? 'checked' : ''}></div></td>
        <td class="text-end"><button class="btn btn-sm btn-outline-secondary" data-regenerar="${s.id}" title="Generar código nuevo">
          <i class="bi bi-arrow-repeat"></i></button></td></tr>`).join('');
  }

  $('formSitio').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      await IDS.post('/catalogos/sitios', { nombre: $('sNombre').value, direccion: $('sDireccion').value });
      ev.target.reset(); IDS.aviso('Sitio creado'); sitios();
    } catch (e) { IDS.error(e); }
  });

  $('tSitios').addEventListener('click', async (ev) => {
    const b = ev.target.closest('button');
    if (!b) return;
    try {
      if (b.dataset.copiar) { await navigator.clipboard.writeText(b.dataset.copiar); IDS.aviso('Código copiado'); }
      if (b.dataset.regenerar) {
        if (!confirm('¿Generar un código nuevo? El anterior dejará de servir para registrar agentes (los ya registrados siguen funcionando).')) return;
        await IDS.post('/catalogos/sitios/' + b.dataset.regenerar + '/codigo'); IDS.aviso('Código regenerado'); sitios();
      }
    } catch (e) { IDS.error(e); }
  });

  $('tSitios').addEventListener('change', async (ev) => {
    const el = ev.target;
    if (!el.dataset.sitioActivo) return;
    try {
      await IDS.put('/catalogos/sitios/' + el.dataset.sitioActivo,
        { nombre: el.dataset.nombre, direccion: el.dataset.direccion, activo: el.checked });
      IDS.aviso('Sitio actualizado');
    } catch (e) { IDS.error(e); el.checked = !el.checked; }
  });

  // ------------------------------------------------------------ departamentos
  async function deptos() {
    const lista = await IDS.get('/catalogos/departamentos');
    $('lDeptos').innerHTML = lista.map(d => `<li class="list-group-item d-flex justify-content-between align-items-center py-1">
        ${IDS.esc(d.nombre)}<button class="btn btn-sm btn-link text-danger" data-borrar="${d.id}" title="Eliminar"><i class="bi bi-trash"></i></button></li>`).join('')
      || '<li class="list-group-item text-secondary">Sin departamentos</li>';
  }

  $('formDepto').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try { await IDS.post('/catalogos/departamentos', { nombre: $('dNombre').value }); ev.target.reset(); deptos(); }
    catch (e) { IDS.error(e); }
  });

  $('lDeptos').addEventListener('click', async (ev) => {
    const b = ev.target.closest('button[data-borrar]');
    if (!b || !confirm('¿Eliminar el departamento? Los equipos quedarán sin departamento.')) return;
    try { await IDS.del('/catalogos/departamentos/' + b.dataset.borrar); deptos(); } catch (e) { IDS.error(e); }
  });

  // ------------------------------------------------------------ usuarios
  async function usuarios() {
    const lista = await IDS.get('/catalogos/usuarios');
    $('tUsuarios').innerHTML = lista.map(u => {
      const propio = u.id === yo.id;
      return `<tr>
        <td>${IDS.esc(u.nombre)}${propio ? ' <span class="badge text-bg-light border">tú</span>' : ''}</td>
        <td>${IDS.esc(u.email)}</td>
        <td><select class="form-select form-select-sm" data-rol="${u.id}" style="width:auto" ${propio ? 'disabled' : ''}>
          <option ${u.rol === 'USUARIO' ? 'selected' : ''}>USUARIO</option><option ${u.rol === 'ADMIN' ? 'selected' : ''}>ADMIN</option></select></td>
        <td><div class="form-check form-switch mb-0"><input class="form-check-input" type="checkbox" data-activo="${u.id}"
          ${u.activo ? 'checked' : ''} ${propio ? 'disabled' : ''}></div></td>
        <td class="small">${IDS.fecha(u.fechaAlta)}</td>
        <td class="text-end"><button class="btn btn-sm btn-outline-secondary" data-clave="${u.id}"><i class="bi bi-key"></i> Contraseña</button></td>
      </tr>`;
    }).join('');
  }

  $('formUsuario').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      await IDS.post('/catalogos/usuarios', { nombre: $('uNombre').value, email: $('uEmail').value,
        password: $('uPassword').value, rol: $('uRol').value });
      ev.target.reset(); IDS.aviso('Usuario creado'); usuarios();
    } catch (e) { IDS.error(e); }
  });

  $('tUsuarios').addEventListener('change', async (ev) => {
    const el = ev.target;
    try {
      if (el.dataset.rol) await IDS.put('/catalogos/usuarios/' + el.dataset.rol, { rol: el.value });
      if (el.dataset.activo) await IDS.put('/catalogos/usuarios/' + el.dataset.activo, { activo: el.checked });
      IDS.aviso('Usuario actualizado');
    } catch (e) { IDS.error(e); usuarios(); }
  });

  $('tUsuarios').addEventListener('click', async (ev) => {
    const b = ev.target.closest('button[data-clave]');
    if (!b) return;
    const nueva = prompt('Nueva contraseña (mínimo 8 caracteres, con letra y número):');
    if (!nueva) return;
    try { await IDS.put('/catalogos/usuarios/' + b.dataset.clave, { password: nueva }); IDS.aviso('Contraseña actualizada'); }
    catch (e) { IDS.error(e); }
  });

  // ------------------------------------------------------------ respaldo
  $('btnRespaldo').addEventListener('click', async () => {
    try { await IDS.descargar('/datos/respaldo?eventos=' + $('chkEventos').checked, 'ids-respaldo.json'); }
    catch (e) { IDS.error(e); }
  });

  let respaldo = null;
  $('archivoRespaldo').addEventListener('change', async (ev) => {
    const archivo = ev.target.files[0];
    respaldo = null;
    $('resultadoRestaurar').innerHTML = '';
    $('btnRestaurar').disabled = true;
    if (!archivo) return;
    try {
      const texto = await archivo.text();
      const datos = JSON.parse(texto);
      if (!datos || !datos.version || !Array.isArray(datos.dispositivos)) throw new Error('No es un respaldo de ideacom IDS');
      respaldo = { texto, datos };
      $('btnRestaurar').disabled = false;
    } catch (e) {
      $('resultadoRestaurar').innerHTML = `<div class="alert alert-danger py-2 small">Archivo no válido: ${IDS.esc(e.message)}</div>`;
    }
  });

  $('btnRestaurar').addEventListener('click', async () => {
    if (!respaldo) return;
    const d = respaldo.datos;
    const n = (x) => (x || []).length;
    const resumen = `Respaldo del ${IDS.fecha(d.generado)}:\n- ${n(d.sitios)} sitio(s)\n- ${n(d.departamentos)} departamento(s)\n`
      + `- ${n(d.usuarios)} usuario(s)\n- ${n(d.dispositivos)} dispositivo(s)\n- ${n(d.incidencias)} incidencia(s)\n`
      + `- ${n(d.eventos)} evento(s)\n\nSe fusionará con los datos actuales (no se borra nada). ¿Continuar?`;
    if (!confirm(resumen)) return;
    $('btnRestaurar').disabled = true;
    try {
      const r = await IDS.enviarTexto('POST', '/datos/respaldo/restaurar', respaldo.texto, 'application/json');
      const filas = r.secciones.map(x => `<tr><td>${IDS.esc(x.nombre)}</td><td>${x.creados}</td><td>${x.actualizados}</td>
          <td>${x.omitidos}</td></tr>`).join('');
      const avisos = (r.avisos || []).length
        ? `<div class="alert alert-warning py-2 small mt-2">${r.avisos.map(IDS.esc).join('<br>')}</div>` : '';
      $('resultadoRestaurar').innerHTML = `<table class="table table-sm small mb-0"><thead class="table-light">
          <tr><th>Sección</th><th>Creados</th><th>Actualizados</th><th>Omitidos</th></tr></thead><tbody>${filas}</tbody></table>${avisos}`;
      IDS.aviso('Respaldo restaurado');
      Promise.all([sitios(), deptos(), usuarios()]).catch(IDS.error);
    } catch (e) {
      IDS.error(e);
    } finally {
      $('btnRestaurar').disabled = false;
    }
  });

  Promise.all([sitios(), deptos(), usuarios()]).catch(IDS.error);
})();
