# SIGTM — Sistema Integral de Gestión de Taller Mecánico

Aplicación **full-stack** para digitalizar la operación de un taller mecánico: recepción de vehículos, órdenes de trabajo, inventario de repuestos, facturación, agenda de citas, reportes y control de accesos por rol.

- **Backend:** Java 21 + Spring Boot (API REST + JWT) + MySQL
- **Frontend:** React + TypeScript + Vite + Tailwind CSS
- **Despliegue:** Docker Compose (MySQL, phpMyAdmin, backend, frontend)

---

## Características

| Módulo | Descripción |
|---|---|
| **Autenticación** | Login con **JWT**, recuperación de contraseña por correo (SMTP) |
| **Recepción** | Recepción digital del vehículo con inspección de daños y fotografías |
| **Órdenes de Trabajo** | Apertura, asignación de ítems (mano de obra/repuestos), estados |
| **Mis Trabajos** | Vista del mecánico: ítems asignados y cambio de estado |
| **Clientes y Vehículos** | Fichas, historial clínico por unidad |
| **Agenda y Citas** | Programación por fecha/bahía |
| **Inventario** | Repuestos, stock, movimientos (entradas/salidas) y alertas de stock bajo |
| **Facturación** | Facturas por orden de trabajo y registro de pagos |
| **Reportes** | Dashboard e informes exportables a **PDF** y **Excel** |
| **Usuarios / Roles** | CRUD de usuarios y matriz de **Roles y Permisos** |
| **Auditoría** | Bitácora de acciones del sistema |
| **Configuración** | Datos del taller, catálogos y correo saliente |

---

## Tecnologías

**Backend**
- Java 21, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security
- **JWT** (`jjwt`), contraseñas con **BCrypt**
- Bean Validation, Lombok, MySQL 8
- Maven (wrapper incluido)

**Frontend**
- React 19, TypeScript, Vite
- Tailwind CSS v4, lucide-react (iconos), recharts (gráficos)
- axios, jsPDF + jspdf-autotable (PDF), xlsx (Excel)
- Vitest + Testing Library (pruebas)

---

## Requisitos

- **Docker** y **Docker Compose** (recomendado), **o**
- **JDK 21+**, **Node.js 20+**, **MySQL 8** (para desarrollo local)

---

## Puesta en marcha con Docker

1. **Clona** el repositorio:
   ```bash
   git clone https://github.com/ivanechajay21/TALLERAUTO.git
   cd TALLERAUTO
   ```

2. **Crea tu archivo de entorno** a partir de la plantilla:
   ```bash
   cp .env.example .env      # Windows: copy .env.example .env
   ```
   Ajusta al menos `JWT_SECRET` y, si usarás recuperación de contraseña, las `MAIL_*`.

3. **Levanta los servicios**:
   ```bash
   docker compose up -d --build
   ```

4. **Accede**:

   | Servicio | URL |
   |---|---|
   | Frontend | http://localhost:3000 |
   | Backend (API) | http://localhost:8080/api |
   | phpMyAdmin | http://localhost:8081 |
   | MySQL | `localhost:3306` |

> En el primer arranque el backend **siembra** roles, permisos, usuarios y datos de ejemplo.

---

## Credenciales de prueba (seed)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `gerente` | `gerente123` | Gerente |
| `recepcion` | `recepcion123` | Recepcionista |
| `mecanico` | `mecanico123` | Mecánico |
| `almacen` | `almacen123` | Almacenero |

> ⚠️ Son credenciales **de desarrollo**. Cámbialas en producción.

---

## Desarrollo local (sin Docker)

**Backend**
```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

**Frontend** (el servidor de Vite hace proxy de `/api` → `http://localhost:8080`)
```bash
cd frontend
npm install
npm run dev
```

---

## Pruebas

**Backend** (JUnit 5 + Mockito; sin base de datos):
```bash
cd backend
./mvnw -Dtest=ProductoServiceTest,RolServiceTest,ProductoControllerTest,RolControllerTest test
```
| Suite | Capa | Pruebas |
|---|---|---:|
| `ProductoServiceTest` | Servicio (inventario) | 12 |
| `RolServiceTest` | Servicio (roles + salvaguarda del ADMIN) | 13 |
| `ProductoControllerTest` | Web (HTTP + permisos) | 10 |
| `RolControllerTest` | Web (roles + rol ADMIN) | 11 |
| **Total** | | **46** |

**Frontend** (Vitest + Testing Library):
```bash
cd frontend
npm test
```
| Suite | Pruebas |
|---|---:|
| `Pagination.test.tsx` | 5 |
| `usePagination.test.ts` | 4 |
| `Login.test.tsx` | 3 |
| `Reports.test.tsx` | 4 |
| `Inventory.test.tsx` | 3 |
| `Vehicles.test.tsx` | 4 |
| **Total** | **23** |

> **69 pruebas** en total (46 backend + 23 frontend).

---

## Estructura del proyecto

```
TALLERAUTO/
├── backend/                 # API REST (Spring Boot)
│   └── src/main/java/com/sigtm/backend/
│       ├── controller/      # Endpoints REST
│       ├── service/         # Lógica de negocio
│       ├── repository/      # Acceso a datos (Spring Data JPA)
│       ├── model/           # Entidades
│       ├── dto/             # Objetos de transferencia
│       ├── security/        # JWT, filtros y configuración de seguridad
│       ├── config/          # Configuración y datos iniciales
│       └── exception/       # Manejo global de errores
├── frontend/                # SPA (React + Vite)
│   └── src/
│       ├── pages/           # Pantallas
│       ├── components/      # Componentes (UI compartida, Sidebar, Header…)
│       ├── services/        # Cliente HTTP y servicios por módulo
│       ├── context/         # Estado global (permisos, ajustes, tema)
│       └── hooks/           # Hooks reutilizables
├── docs/                    # Documentación técnica
├── docker-compose.yml
└── .env.example
```

---

## Seguridad

- **JWT** sin estado; los permisos se **recargan desde la base de datos** en cada petición.
- **Contraseñas con BCrypt** (nunca en texto plano).
- Autorización por permiso `MODULO:ACCION` (`@PreAuthorize`) en los endpoints; las operaciones sobre roles quedan reservadas al rol **ADMIN**.
- El rol **Administrador conserva permisos críticos** que no se pueden quitar (evita que el sistema se quede sin acceso).
- Validación de entrada (`@Valid`) y manejo global de excepciones.

> El archivo `.env` **no** se versiona. Usa `.env.example` como plantilla.

---

## Documentación

- [`API.md`](./API.md) — API REST y funciones del frontend (endpoints por módulo y permisos).
- [`docs/ARQUITECTURA.md`](./docs/ARQUITECTURA.md) — evaluación de arquitectura (MVC, DAO, SOLID, seguridad) y pruebas.
- [`docs/ESTADISTICAS.md`](./docs/ESTADISTICAS.md) — estadísticas de código por lenguaje y paquete.

---

## Licencia

Uso educativo / demostrativo. Ajusta esta sección según la licencia que quieras aplicar.
