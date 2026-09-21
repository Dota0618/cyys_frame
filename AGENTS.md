# cyys-framework Rules

Spring Boot backend migration workspace under `F:\git\frame\cyys-framework\`.

## Scope

- Writable root: `F:\git\frame\cyys-framework\`
- Read-only inputs:
  - `F:\git\frame\cyys_monomer\`
  - `F:\git\frame\docs\`

## Maven Commands

- Build all backend modules:
  - `mvn -s F:\git\frame\cyys-framework\backend\.m2\settings.xml clean install`
- Run the executable backend module:
  - `mvn -s F:\git\frame\cyys-framework\backend\.m2\settings.xml -pl cyys-business spring-boot:run`
- Run the packaged jar:
  - `java -jar F:\git\frame\cyys-framework\backend\cyys-business\target\cyys-business.jar`

## Layout Rules

- `backend\` root: only [pom.xml](file:///F:/git/frame/cyys-framework/backend/pom.xml)
- `backend\.m2\`: only `settings.xml` and `repo\**`
- `backend\logs\`: the only place for redirected build/run log files
- `backend\cyys-common\`, `backend\cyys-admin\`, `backend\cyys-business\`: source code and module `pom.xml`
- `migrations\`: versioned SQL migrations
- `scripts\`: scripts
- `deploy\`: deployment files
- `admin-ui\`: frontend files

## Constraints

- Latest user scope (2026-09-21): restructure as subordinate institutions within ONE management system, not independent tenants. Institutions form a hierarchy; consolidate roles and user authorization, with organization-based data ranges. The top-right organization control is a data filter, not a switch between independent permission systems. Preserve existing business data. Code-generator work is paused; hide/disable its entry while the organization model changes. Unit notices remain enabled; system notices are centrally maintained shared content without read receipts. Holidays remain omitted and guide pages paused.
- Keep verification proportional: compile/typecheck during development; defer page-operation/browser tests and full regression to final acceptance, as requested on 2026-09-20. Do not repeatedly run tests after small edits.
- Password reset: administrators do not re-enter their own password; require the independent platform reset permission and only ask for the target account's new password and confirmation. Personal password changes still verify the current password.
- Unit selection (2026-09-21 approved): use only the top-right work-unit selector. Do not repeat selectors, switch-unit shortcuts or selection prompts in pages or forms. Keep unit-bound requests gated until a valid unit is selected; switching still clears old page state and protects unsaved work.
- Form layout (2026-09-21 approved): ordinary edit fields use horizontal labels and controls via shared compactFormProps. Use verticalFormItemProps for wide content such as rich text/multiline bodies, multiple selection, checkbox trees and attachments. Apply the same convention in code-generator output.
- Data permissions (latest 2026-09-21 objective supersedes the earlier per-unit membership design): roles grant menus/actions system-wide. A user has a home institution/department and one data-range configuration across business resources. Consolidate platform/unit role management; support authorized institution descendants without requiring duplicate membership or roles. Keep independent user:data-permission authorization and atomic profile/range saves. Existing tenant-oriented paths are transitional work to remove, not the target architecture.

- Save development checkpoints with local Git commits; do not create additional source archive snapshots. Review the diff and record completed checks and remaining work before committing. Do not push unless requested. Existing ignored documentation, local configuration, logs and build outputs remain excluded. This workflow was approved by the user on 2026-09-20.

- Use `-s F:\git\frame\cyys-framework\backend\.m2\settings.xml` for Maven commands in this project.
- Maven `target\` directories are normal build output. Do not reorganize them unless the user asks.
- Do not create new top-level directories under `cyys-framework\` without user approval.
- Do not write temporary files to the framework root, `backend\` root, or `backend\.m2\`.

## Development Entry

- The local framework migration is recorded in [final acceptance](docs/migration/final-acceptance.md). The selected basic data now remains in the existing MySQL test database; do not erase it to satisfy empty-database test guards. Stop the local Docker API before any future same-database test work.
- Preserve the user's approved scheme A: admin packages are `com.cyys.admin.<feature>.<responsibility>`; project business packages are `com.cyys.modules.<feature>.<responsibility>`. Use controller/model/dto/service/mapper only when actual code needs them. Common contains technical capabilities and does not query admin business tables.
- Controllers call Services and use independent input/output DTO files. Services own use cases, transactions and business constraints; do not add interface/Impl, Manager, Facade or Converter layers by default.
- Preserve the approved validation boundary: input shape at the API, authorization/ownership/state/concurrency in the Service and database. Never convert identity/configuration failures into a default owner, unrestricted access or success. Missing internal invariants must fail visibly.
- Consult the [development guide index](docs/development/README.md) only for the relevant task. Do not preload or invent a full guide library. New resources must pass their actual operation and isolation checks before being exposed; successful compilation alone is insufficient.
- Before frontend work, read [admin-ui/AGENTS.md](admin-ui/AGENTS.md) for the official template and project Ant Design CLI skill entry.
- Ask the user before establishing each new development rule or changing an approved rule's scope. The current approval covers the proposed scheme A correction and migration constraints; it does not preapprove future component guides.
