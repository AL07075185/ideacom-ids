'use strict';
(() => {
  IDS.requiereSesion();
  IDS.barra('mapa.html');

  const FORMAS = { PC: 'box', SERVIDOR: 'hexagon', RED: 'diamond', CAMARA: 'triangle', NVR: 'square',
    IMPRESORA: 'ellipse', OTRO: 'dot' };
  const nodos = new vis.DataSet();
  const aristas = new vis.DataSet();
  let datos = { nodos: [], aristas: [] };

  document.getElementById('leyenda').innerHTML = Object.entries(IDS.ESTADOS)
    .map(([, [texto, , color]]) => `<span><i style="background:${color}"></i>${texto}</span>`).join('');

  function opciones(arbol) {
    return {
      layout: arbol ? { hierarchical: { direction: 'UD', sortMethod: 'directed', levelSeparation: 110, nodeSpacing: 160 } }
                    : { hierarchical: false },
      physics: arbol ? false : { stabilization: { iterations: 200 } },
      nodes: { font: { size: 13, multi: false }, borderWidth: 2, margin: 10, shadow: false },
      edges: { dashes: [6, 6], color: { color: '#9aa4b2' }, width: 1.5, smooth: false },
      interaction: { hover: true }
    };
  }

  const red = new vis.Network(document.getElementById('red'), { nodes: nodos, edges: aristas }, opciones(true));

  function aNodo(n) {
    const color = IDS.estado(n.estado)[2];
    const forma = FORMAS[n.tipo] || 'dot';
    const conTexto = forma === 'box' || forma === 'ellipse';
    return {
      id: n.id, shape: forma, size: 22,
      label: n.nombre + (n.ip ? '\n' + n.ip : ''),
      title: `${n.nombre} - ${IDS.estado(n.estado)[0]}`,
      color: { background: conTexto ? '#ffffff' : color, border: color,
        highlight: { background: conTexto ? '#f1f5ff' : color, border: '#0d6efd' } },
      font: { color: '#212529' }
    };
  }

  async function cargar() {
    try {
      datos = await IDS.get('/dispositivos/mapa');
      nodos.update(datos.nodos.map(aNodo));
      const ids = new Set(datos.nodos.map(n => n.id));
      nodos.getIds().filter(id => !ids.has(id)).forEach(id => nodos.remove(id));
      aristas.clear();
      aristas.add(datos.aristas.map(a => ({ id: a.desde + '-' + a.hacia, from: a.desde, to: a.hacia })));
      if (!datos.nodos.length) {
        document.getElementById('panelNodo').innerHTML =
          '<p class="text-secondary mb-0">Aún no hay equipos aprobados para mostrar.</p>';
      }
    } catch (e) { IDS.error(e); }
  }

  red.on('click', (p) => {
    if (!p.nodes.length) return;
    const n = datos.nodos.find(x => x.id === p.nodes[0]);
    if (!n) return;
    document.getElementById('panelNodo').innerHTML = `
      <h6 class="mb-2">${IDS.iconoTipo(n.tipo)} ${IDS.esc(n.nombre)}</h6>
      <p class="mb-2">${IDS.badgeEstado(n.estado)}</p>
      <dl class="row dl-compacta small mb-3">
        <dt class="col-4">IP</dt><dd class="col-8 codigo">${IDS.esc(n.ip || '—')}</dd>
        <dt class="col-4">Tipo</dt><dd class="col-8">${IDS.esc(n.tipo)}</dd>
        <dt class="col-4">Sitio</dt><dd class="col-8">${IDS.esc(n.sitio)}</dd>
      </dl>
      <a class="btn btn-sm btn-outline-danger" href="incidencias.html?dispositivo=${n.id}">
        <i class="bi bi-life-preserver"></i> Levantar incidencia</a>`;
  });

  document.getElementById('dispArbol').addEventListener('change', () => red.setOptions(opciones(true)));
  document.getElementById('dispLibre').addEventListener('change', () => red.setOptions(opciones(false)));

  cargar();
  setInterval(cargar, window.IDS_CONFIG.refrescoSegundos * 1000);
})();
