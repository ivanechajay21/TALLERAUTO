## Descripción

<!-- ¿Qué cambia este PR y por qué? Explica el contexto en 2-4 líneas. -->

## Tipo de cambio

- [ ] `feat` — nueva funcionalidad
- [ ] `fix` — corrección de error
- [ ] `docs` — documentación
- [ ] `test` — pruebas
- [ ] `refactor` — refactorización
- [ ] `chore` — configuración / mantenimiento

## Módulo afectado

<!-- Marca lo que aplique -->

- [ ] Autenticación / Seguridad
- [ ] Clientes / Vehículos
- [ ] Recepción / Citas
- [ ] Órdenes de trabajo / Mi trabajo
- [ ] Inventario
- [ ] Facturación
- [ ] Reportes
- [ ] Usuarios / Roles
- [ ] Auditoría / Configuración
- [ ] Infraestructura (Docker / BD / CI)

## ¿Cómo probarlo?

<!-- Pasos concretos para que el revisor verifique el cambio. -->

1.
2.
3.

## Checklist

- [ ] La rama está actualizada con `main`
- [ ] Probé los cambios localmente
- [ ] `cd backend && ./mvnw -Dtest=ProductoServiceTest,RolServiceTest,ProductoControllerTest,RolControllerTest test` en verde
- [ ] `cd frontend && npm test` en verde
- [ ] No incluí `.env`, contraseñas ni tokens
- [ ] No incluí `node_modules/`, `target/` ni `dist/`
- [ ] Actualicé la documentación si cambió el comportamiento
- [ ] El título del PR es descriptivo

## Evidencia

<!-- Capturas, logs o videos (opcional). -->

## Issue relacionado

<!-- Closes #123 (si aplica) -->
