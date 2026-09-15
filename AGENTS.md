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

- Use `-s F:\git\frame\cyys-framework\backend\.m2\settings.xml` for Maven commands in this project.
- Maven `target\` directories are normal build output. Do not reorganize them unless the user asks.
- Do not create new top-level directories under `cyys-framework\` without user approval.
- Do not write temporary files to the framework root, `backend\` root, or `backend\.m2\`.

## Current Migration Entry

- The user approved scheme A (feature first, responsibility second) and this correction on 2026-09-14. Before migration work, read [temporary execution rules](docs/migration/execution-rules.md) and [actual progress](docs/migration/progress.md).
- Keep the phase order in `F:\git\frame\docs\task.md`. A successful build is not phase acceptance. G1 must pass before management CRUD is enabled.
- For later feature development, consult the [development guide index](docs/development/README.md) only for the relevant task. Do not preload or invent a full guide library.
- Before frontend work, read [admin-ui/AGENTS.md](admin-ui/AGENTS.md) for the official template and project Ant Design CLI skill entry.
- Ask the user before establishing each new development rule or changing an approved rule's scope. The current approval covers the proposed scheme A correction and migration constraints; it does not preapprove future component guides.
- Remove the temporary migration entry and execution rules when the migration is accepted and transition items are resolved. Keep necessary acceptance evidence and approved development guides.
