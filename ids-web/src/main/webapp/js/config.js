// Ruta base de la API. Si el panel y la API estan en el mismo servidor
// (GlassFish local o detras de Nginx) no hace falta cambiarla.
window.IDS_CONFIG = {
  apiBase: '/ids-api/api/v1',
  // Reporte JSP (vive en la aplicacion ids-api)
  reporteUrl: '/ids-api/reportes/disponibilidad',
  refrescoSegundos: 30
};
