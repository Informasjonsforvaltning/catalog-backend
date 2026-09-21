# AGENTS.md

Guidance for humans and AI agents working in this repository.

## Stack

- Kotlin / Java / Spring Boot / Maven (`finalName=app`)
- PostgreSQL + JPA + Flyway (`ddl-auto: validate` in every profile)
- Jena for RDF; springdoc for OpenAPI (no hand-maintained `specification.yaml`)
- Tests: JUnit `@Tag("unit")` → Surefire, `@Tag("integration")` → Failsafe; JaCoCo merges to `./target/site/jacoco/jacoco.xml`

```bash
mvn spring-boot:run
mvn clean verify          # ktlint + unit + integration (Testcontainers Postgres)
mvn ktlint:format
```

Local API docs: http://localhost:8080/swagger-ui/index.html

## Role vocabulary

JWT claim `authorities` (comma-delimited, no prefix). SpEL constants live in `security/Authorities.kt`:

| Authority | Meaning |
|---|---|
| `system:root:admin` | Platform admin (sees all catalogs on `/catalogs/count`) |
| `organization:{catalogId}:admin` | Org admin for that catalog |
| `organization:{catalogId}:write` | Create / patch / delete / publish / unpublish |
| `organization:{catalogId}:read` | Read JSON APIs for that catalog |

Public (no token): `GET /graphs/**`, actuator `/ping` `/ready` `/prometheus`, swagger/OpenAPI.

## Architecture

Feature-first packaging with an enforced dependency direction:

```
no.fdk.catalogbackend
├── config/ security/ exception/     # cross-cutting
├── core/                            # mechanism only — never names a concrete type or table
│   ├── model/ spi/ persistence/ service/
│   ├── publication/ rdf/ web/
└── resource/<type>/                 # one self-contained package per resource type
```

**Invariant (enforced by `ArchitectureTest`):**

- `core..` must not depend on `resource..`
- Resource types must not depend on each other
- Only `resource..` may declare `@Entity` / `@Table`
- Controllers live in `core.web` or `resource..`
- Core reaches storage through `ResourceStore`, never `*Repository`

Core owns CRUD/patch/publish/RDF *pipelines*. Features own meaning: domain fields, validation, RDF vocabulary/mapping, harvest metadata (`ResourceTypeMetadata`), and the physical table.

`ResourceType` is a value class (`ResourceType("INFORMATION_MODEL")`), declared as a constant in the feature package — **not** an enum in core. Adding a type requires **zero core edits**.

Startup: `ResourceRegistry` fails fast if a type is missing store / metadata / mapper / RDF writer, or if two types claim the same `pathSegment`.

## Canonical core column block

Every resource-type table **must** start from this block (timestamps are `WITH TIME ZONE` — Hibernate maps `Instant` to `TIMESTAMP_UTC`):

```sql
CREATE TABLE <table_name> (
    id             VARCHAR(255) NOT NULL PRIMARY KEY,
    catalog_id     VARCHAR(50)  NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_date TIMESTAMP WITH TIME ZONE,
    created        TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified  TIMESTAMP WITH TIME ZONE,
    uri            VARCHAR(500),
    data           JSONB
);
CREATE INDEX idx_<table_name>_catalog_id ON <table_name> (catalog_id);
CREATE INDEX idx_<table_name>_catalog_published ON <table_name> (catalog_id, published);
```

`publishedDate` is the instant of the **first** successful publish; it is retained across unpublish/republish. Postgres `timestamptz` is microsecond-precision — writers truncate `Instant.now()` to micros before persist.

Type-specific columns/indexes may be added after this block. `@MappedSuperclass CatalogResourceEntity` plus `ddl-auto: validate` catches type drift at startup.

## Adding a resource type

Checklist — one package, one migration, zero core edits:

1. Create `resource/<type>/` (and optionally `resource/<type>/rdf/`).
2. Declare `val MY_TYPE = ResourceType("MY_TYPE")` in that package.
3. Add Flyway migration `V<n>__create_<table>.sql` using the canonical column block above.
4. Persistence (four declarative lines):
   - `@Entity @Table` entity extending `CatalogResourceEntity`
   - repository extending `CatalogResourceRepository<Entity>`
   - `@Component` store extending `JpaResourceStore<Entity>(MY_TYPE, repository, Entity::class, ::Entity)`
5. Implement beans:
   - `ResourceTypeMetadata` — `pathSegment`, `identifierHost`, `dataSourceType`, `harvestDataType`
   - `ResourceMapper<Values, Dto>` — jsonb payload ↔ DTO
   - `ResourceRdfWriter` — write published entities into a Jena `Model` (URI resources, not blank nodes, for catalog and resource)
6. Thin `@RestController` under `/catalogs/{catalogId}/{pathSegment}` that delegates to `CatalogResourceOperations` (CRUD + publish/unpublish). Do **not** extend a Spring MVC base class for `@PreAuthorize` — use delegation.
7. Golden RDF fixtures under `src/test/resources` and isomorphism tests; contract/auth tests as needed.
8. Run `mvn clean verify`. If a contribution is missing, `ResourceRegistry` fails at context startup.

Publish/harvest wiring is automatic: first publish in a catalog for that type creates a harvest-admin datasource; subsequent publishes/unpublishes/patches (when published) trigger harvest via `HarvestCatalogEvent` after commit.

### Public RDF routes

- Catalog graph: `GET /graphs/catalogs/{catalogId}/{pathSegment}`
- Resource graph: `GET /graphs/{pathSegment}/{id}`

Minted identifier IRIs use type-specific hosts from `ApplicationProperties` / metadata — not the `/graphs` API base.

## Conventions

- Immutable JSON Patch paths: `/id`, `/catalogId`, `/created`, `/lastModified`, `/published`, `/publishedDate`, `/uri`
- Harvest-admin calls swallow failures (`runCatching`) so a harvest outage never fails the user request; RestTemplate has finite connect/read timeouts
- Prefer `@Transactional` on service methods that own the unit of work, not on `CatalogResourceOperations`
- Test packages mirror main packages; use the fake resource type under `testsupport/fake` for type-agnostic core tests (it is component-scanned into integration tests)

## Deferred (do not invent here)

- catalog-history-service integration
- Pagination (siblings return `List<T>`; add in `ResourceStore` / service / operations when datasets migrate)
- RDF import
- Cross-type feeds beyond `/catalogs/count`
