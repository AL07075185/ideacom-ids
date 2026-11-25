<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Reporte no disponible - ideacom IDS</title>
  <link rel="icon" href="${pageContext.request.contextPath}/favicon.svg" type="image/svg+xml">
  <style>
    body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif; background: #f4f6f9;
           display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
    .caja { background: #fff; padding: 28px 32px; border-radius: 10px; max-width: 420px; text-align: center;
            box-shadow: 0 1px 3px rgba(0,0,0,.08); }
    h1 { font-size: 18px; margin-top: 0; }
    p { color: #6c757d; }
  </style>
</head>
<body>
<div class="caja">
  <h1>No se pudo abrir el reporte</h1>
  <p><c:out value="${mensaje}"/></p>
</div>
</body>
</html>
