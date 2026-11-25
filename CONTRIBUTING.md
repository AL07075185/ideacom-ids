# Cómo contribuir a ideacom IDS

Gracias por tu interés. Este repositorio sigue un flujo con dos ramas principales y una rama por cada cambio.

## 1. Clonar el repositorio

```bash
git clone git@github.com:AL07075185/ideacom-ids.git
cd ideacom-ids
git checkout develop
```

## 2. Elegir o crear un issue

Todo cambio parte de un issue. Si no existe, créalo con la plantilla **Requerimiento**, **Tarea** o **Error** y llena las secciones Descripción, Análisis y Solución.

## 3. Crear una rama desde develop

| Tipo de cambio | Nombre de la rama |
|---|---|
| Funcionalidad nueva | `feature/<id>-<descripcion-corta>` (ej. `feature/rf-pw-06-mapa`) |
| Corrección | `fix/<id>-<descripcion-corta>` |
| Documentación | `docs/<tema>` |

```bash
git checkout -b feature/rf-pw-06-mapa develop
```

## 4. Programar, probar y hacer commit

```bash
mvn -B clean verify                 # todas las pruebas deben pasar
git add -A
git commit -m "Agrega el color por estado al mapa de red (RF-PW-06)"
git push -u origin feature/rf-pw-06-mapa
```

Reglas: mensajes en español y en imperativo; sin credenciales, archivos `agente.properties`, carpetas `target/` ni datos reales.

## 5. Abrir el pull request hacia develop

```bash
gh pr create --base develop --fill
```

En la descripción escribe `Closes #<número>` para que el issue se cierre al fusionar.

## 6. Esperar la revisión y el merge

- Drone CI debe terminar en verde.
- Se revisa el cambio y se fusiona con **Merge pull request** (sin borrar la rama).
- Las versiones se liberan integrando `develop` en `master` con un tag.
