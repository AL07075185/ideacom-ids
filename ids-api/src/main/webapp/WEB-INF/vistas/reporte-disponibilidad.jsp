<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ids" uri="http://ideacom.mx/ids/tags" %>
<%--
  Vista del reporte de disponibilidad (MVC del lado del servidor).
  Controlador: ReporteServlet · Modelo: Reporte (request attribute "reporte").
  Usa JSTL (c, fmt) y las etiquetas personalizadas de /WEB-INF/ids.tld.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reporte de disponibilidad - ideacom IDS</title>
  <link rel="icon" href="${ctx}/favicon.svg" type="image/svg+xml">
  <style>
    * { box-sizing: border-box; }
    body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif; color: #212529;
           margin: 0; padding: 24px; background: #f4f6f9; font-size: 14px; }
    .hoja { max-width: 1100px; margin: 0 auto; background: #fff; padding: 28px 32px; border-radius: 10px;
            box-shadow: 0 1px 3px rgba(0,0,0,.08); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
             border-bottom: 2px solid #0d6efd; padding-bottom: 12px; margin-bottom: 16px; }
    h1 { font-size: 22px; margin: 0 0 4px; }
    h2 { font-size: 16px; margin: 28px 0 8px; }
    .meta { color: #6c757d; font-size: 13px; line-height: 1.6; }
    .filtros { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
    select, button { font: inherit; padding: 6px 10px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; }
    button { cursor: pointer; }
    button.primario { background: #0d6efd; color: #fff; border-color: #0d6efd; }
    .resumen { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
    .tarjeta { background: #f8f9fa; border-radius: 8px; padding: 12px 14px; }
    .tarjeta .valor { font-size: 22px; font-weight: 600; }
    .tarjeta .etiqueta { color: #6c757d; font-size: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th { text-align: left; background: #f1f3f5; padding: 7px 8px; border-bottom: 1px solid #dee2e6; }
    td { padding: 6px 8px; border-bottom: 1px solid #eef0f2; vertical-align: top; }
    .num { text-align: right; white-space: nowrap; }
    .vacio { color: #6c757d; font-style: italic; padding: 10px 0; }
    .estado { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 12px; white-space: nowrap; }
    .estado-EN_LINEA { background: #d1e7dd; color: #0a3622; }
    .estado-SIN_INTERNET { background: #fff3cd; color: #664d03; }
    .estado-APAGADO { background: #e2e3e5; color: #41464b; }
    .estado-DESCONECTADO { background: #f8d7da; color: #58151c; }
    .estado-SIN_AGENTE { background: #cff4fc; color: #055160; }
    .estado-DESCONOCIDO { background: #f8f9fa; color: #6c757d; border: 1px solid #dee2e6; }
    .pct { font-weight: 600; }
    .pct-bien { color: #198754; } .pct-aviso { color: #b58100; } .pct-mal { color: #dc3545; } .pct-nd { color: #adb5bd; }
    .prioridad-ALTA { color: #dc3545; font-weight: 600; }
    footer { margin-top: 28px; color: #adb5bd; font-size: 12px; text-align: center; }
    @media print {
      body { background: #fff; padding: 0; }
      .hoja { box-shadow: none; padding: 0; max-width: none; }
      .no-imprimir { display: none !important; }
      tr { page-break-inside: avoid; }
    }
  </style>
</head>
<body>
<div class="hoja">
  <header>
    <div>
      <h1>Reporte de disponibilidad</h1>
      <div class="meta">
        <c:out value="${reporte.sitio}"/> · <c:out value="${reporte.periodo}"/><br>
        Generado el <c:out value="${reporte.generado}"/> (<c:out value="${reporte.zona}"/>) por <c:out value="${reporte.usuario}"/>
      </div>
    </div>
    <div class="meta" style="text-align:right"><strong>ideacom IDS</strong><br>Monitoreo de dispositivos</div>
  </header>

  <form class="filtros no-imprimir" method="get" action="${ctx}/reportes/disponibilidad">
    <input type="hidden" name="tz" value="<c:out value='${reporte.zona}'/>">
    <label>Periodo
      <select name="horas">
        <option value="24" ${reporte.horas == 24 ? 'selected' : ''}>Últimas 24 horas</option>
        <option value="168" ${reporte.horas == 168 ? 'selected' : ''}>Últimos 7 días</option>
        <option value="720" ${reporte.horas == 720 ? 'selected' : ''}>Últimos 30 días</option>
      </select>
    </label>
    <label>Sitio
      <select name="sitio">
        <option value="">Todos</option>
        <c:forEach items="${reporte.sitios}" var="s">
          <option value="${s.id}" ${s.id == reporte.idSitio ? 'selected' : ''}><c:out value="${s.nombre}"/></option>
        </c:forEach>
      </select>
    </label>
    <button type="submit">Actualizar</button>
    <button type="button" class="primario" onclick="window.print()">Imprimir o guardar PDF</button>
  </form>

  <section class="resumen">
    <div class="tarjeta"><div class="valor"><fmt:formatNumber value="${reporte.totalEquipos}"/></div>
      <div class="etiqueta">Equipos en el reporte</div></div>
    <div class="tarjeta"><div class="valor"><ids:porcentaje valor="${reporte.promedio}"/></div>
      <div class="etiqueta">Disponibilidad promedio</div></div>
    <div class="tarjeta"><div class="valor"><fmt:formatNumber value="${reporte.totalDesconexiones}"/></div>
      <div class="etiqueta">Desconexiones inesperadas</div></div>
    <div class="tarjeta"><div class="valor"><c:out value="${reporte.tiempoCaidoTotal}"/></div>
      <div class="etiqueta">Tiempo total sin servicio</div></div>
  </section>

  <h2>Dispositivos</h2>
  <c:choose>
    <c:when test="${empty reporte.filas}">
      <p class="vacio">No hay dispositivos aprobados para este filtro.</p>
    </c:when>
    <c:otherwise>
      <table>
        <thead>
          <tr><th>Dispositivo</th><th>Tipo</th><th>IP</th><th>Departamento</th><th>Estado actual</th>
              <th class="num">Disponibilidad</th><th class="num">Desconexiones</th><th class="num">Sin servicio</th>
              <th>Último reporte</th></tr>
        </thead>
        <tbody>
          <c:forEach items="${reporte.filas}" var="f">
            <tr>
              <td><strong><c:out value="${f.nombre}"/></strong>
                <c:if test="${empty param.sitio}"><br><span class="meta"><c:out value="${f.sitio}"/></span></c:if></td>
              <td><c:out value="${f.tipo}"/></td>
              <td><c:out value="${empty f.ip ? '—' : f.ip}"/></td>
              <td><c:out value="${f.departamento}"/></td>
              <td><ids:estado valor="${f.estado}"/></td>
              <td class="num"><ids:porcentaje valor="${f.disponibilidad}"/></td>
              <td class="num"><c:out value="${f.desconexiones}"/></td>
              <td class="num"><c:out value="${f.tiempoCaido}"/></td>
              <td><c:out value="${f.ultimoReporte}"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>

  <h2>Incidencias abiertas</h2>
  <c:choose>
    <c:when test="${empty reporte.incidencias}">
      <p class="vacio">Sin incidencias abiertas.</p>
    </c:when>
    <c:otherwise>
      <table>
        <thead><tr><th>#</th><th>Título</th><th>Dispositivo</th><th>Prioridad</th><th>Estado</th><th>Levantada</th></tr></thead>
        <tbody>
          <c:forEach items="${reporte.incidencias}" var="i">
            <tr>
              <td><c:out value="${i.id}"/></td>
              <td><c:out value="${i.titulo}"/></td>
              <td><c:out value="${i.dispositivo}"/></td>
              <td class="prioridad-${i.prioridad}"><c:out value="${i.prioridad}"/></td>
              <td><c:out value="${i.estado == 'EN_PROCESO' ? 'En proceso' : 'Abierta'}"/></td>
              <td><c:out value="${i.fecha}"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>

  <h2>Fallas y apagados en el periodo</h2>
  <c:choose>
    <c:when test="${empty reporte.eventos}">
      <p class="vacio">Sin desconexiones ni apagados en el periodo.</p>
    </c:when>
    <c:otherwise>
      <table>
        <thead><tr><th>Fecha</th><th>Dispositivo</th><th>Evento</th><th>Estado</th><th>Detalle</th></tr></thead>
        <tbody>
          <c:forEach items="${reporte.eventos}" var="e">
            <tr>
              <td style="white-space:nowrap"><c:out value="${e.fecha}"/></td>
              <td><c:out value="${e.dispositivo}"/></td>
              <td><c:out value="${e.evento}"/></td>
              <td><c:if test="${not empty e.estado}"><ids:estado valor="${e.estado}"/></c:if></td>
              <td class="meta"><c:out value="${e.detalle}"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
      <c:if test="${fn:length(reporte.eventos) >= 50}"><p class="meta">Se muestran los 50 eventos más recientes.</p></c:if>
    </c:otherwise>
  </c:choose>

  <footer>La disponibilidad excluye los apagados normales; solo cuentan como falla las desconexiones inesperadas.</footer>
</div>
</body>
</html>
