# Estadísticas del proyecto SIGTM

> **Fecha:** 5 de octubre de 2026
> **Métrica:** líneas de código (LOC) = líneas **no vacías**.
> **Exclusiones:** `node_modules`, `target`, `dist`, `.git`, `build`, `.mvn`, `uploads`, `coverage`, `package-lock.json` y archivos minificados.

## Resumen por lenguaje (todo el código)

| Lenguaje | Archivos | Líneas de código | % |
|---|---:|---:|---:|
| TSX (React) | 36 | 11 585 | 51.5 % |
| **Java** | **101** | **8 574** | **38.1 %** |
| TS (servicios/utilidades) | 22 | 1 594 | 7.1 % |
| CSS | 2 | 414 | 1.8 % |
| XML (pom) / YML / HTML / JS | 6 | 325 | 1.5 % |
| **Total** | **167** | **22 492** | **100 %** |

### Vistas rápidas

- **Java (backend): 8 574 líneas → 38.1 %** del código fuente del proyecto.
- **Frontend (TSX + TS + CSS + JS + HTML): 13 630 líneas → 60.6 %.**
- **Configuración (XML/YML): ~1.3 %.**
- **Por número de archivos:** Java = **101 de 167** archivos de código (**60.5 %**), aunque con menos líneas por archivo (muchas entidades, repositorios y DTOs pequeños).

> Dato alternativo: si se cuentan **todas las líneas del repositorio** (incluyendo `.md`, binarios como `.pdf`/`.png`, etc.), Java representa ~**33.9 %**.

## Backend — Java por paquete (`src/main/java`)

| Paquete | Archivos | Líneas de código |
|---|---:|---:|
| `service` | 19 | 3 879 |
| `controller` | 17 | 1 844 |
| `model` | 22 | 1 007 |
| `repository` | 19 | 641 |
| `security` (incl. `jwt`) | 6 | 422 |
| `config` | 3 | 418 |
| `dto` | 10 | 232 |
| `exception` | 3 | 107 |
| raíz (`BackendApplication`) | 1 | 15 |
| **Subtotal `main`** | **100** | **8 565** |
| `test` | 1 | 9 |
| **Total Java** | **101** | **8 574** |

## Frontend — por carpeta (`src`)

| Carpeta | Archivos | Líneas de código |
|---|---:|---:|
| `pages` | 17 | 8 849 |
| `components` | 15 | 2 504 |
| `services` | 17 | 1 461 |
| raíz (`App`, `main`, `index.css`…) | 4 | 533 |
| `context` | 3 | 142 |
| `lib` | 2 | 48 |
| `hooks` | 1 | 38 |
| **Subtotal `src`** | **59** | **13 575** |

> El total del frontend (13 630) incluye además archivos de configuración fuera de `src` (`vite.config.ts`, `tailwind.config.js`, `postcss.config.js`, `index.html`).

## Conclusiones

- Proyecto **full-stack**: el **backend en Java/Spring Boot** concentra cerca de **4 de cada 10 líneas** de código.
- El **frontend en React/TypeScript** es la mayor parte (**> 50 %**), algo habitual en aplicaciones de gestión con paneles, tablas y formularios.
- Dentro del backend, la **lógica de negocio (`service`) es el bloque más grande**, seguido de los controladores (API REST) y el modelo de datos.
- Dentro del frontend, las **páginas** (`pages`) son, con diferencia, el bloque más grande.

### Notas

- Esta métrica es **por líneas**; otra forma de medir es por archivos (ahí Java pesa ~60 %) o por tiempo de desarrollo (no medible desde el código).
- Los **comentarios JavaDoc** que se añadieron al backend forman parte de esas ~8 570 líneas; el código productivo sin comentarios/espacios es menor.
