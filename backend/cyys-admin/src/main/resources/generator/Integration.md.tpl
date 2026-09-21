# Generated module

Template version: 2026-09-21.1. Feature: `@@FEATURE@@`; table: `@@TABLE@@`.

1. Review the generated files and compare against the current project before copying. The generator never writes into the running application's source tree or executes generated SQL.
2. For a new table, rename `PENDING__@@FEATURE@@.sql` to the next unused Flyway version in `migrations/business/`. Use the existing database charset/collation. Do not run a separate schema engine. Existing-table mode emits no table DDL and verifies all configured fields against current metadata.
3. Review `permissions.sql.review`: it creates a named page menu with its button permissions underneath. Select the intended parent directory through menu management, and explicitly grant the directory, page menu and permitted buttons through the role checkbox tree. Navigation also requires the registered local route and read permission. No generated permission grants everyone access. No default accounts are created.
4. Copy Java sources into cyys-business, frontend service/page into admin-ui. Register this route in `config/routes.ts`:

```ts
{ path: '/business/@@FEATURE@@', name: @@TITLE@@, access: 'canRead@@CLASS@@', component: './business/@@FEATURE@@' },
```

Add `canRead@@CLASS@@: canReadInSelectedUnit('@@FEATURE@@:read')` to `src/access.ts`, and `'/business/@@FEATURE@@': access.canRead@@CLASS@@` to IdentityBoundary's route permission map. Review the route-related lint baseline; do not blindly regenerate it.

5. Compile backend, typecheck/build frontend, then validate actual A/B isolation, permission revocation, create/update/delete, associated department/parent checks, optimistic conflicts and page switching on authorized test data before enabling the route. A successful preview alone is not release acceptance.
6. `choices` returns only authorized rows, maximum 500. It supports selectors/tree rendering; a consuming business must recheck its chosen ID at save time. Trees have immutable parents/departments; child insertion and parent deletion lock the same parent. A missing authorized ancestor is displayed as a root without loading inaccessible data. Larger trees need a concrete hierarchical-query design.
7. Dictionary fields validate active options on writes and allow retaining an unchanged historical code. Decimal values are strings in the frontend and exact decimal(18,2) in MySQL. Date/datetime use strict JSON formats; the deployment uses a consistent business timezone.
8. File relationships, rich text, workflow transitions, cross-record uniqueness and business-specific side effects need explicit domain implementation. They are not inferred from column names. Do not add generic arbitrary-table operations or bypass data policy to make a template work.
9. Keep generator-config.json with the module's development materials. Import it into the generator to make a new preview; compare differences before applying later generations.

Generated sources are ordinary editable project code, with independent DTO files and feature-first packages. After hand edits, this package is not an authority to overwrite them.
