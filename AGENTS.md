# Agent Quickstart Guide

Interactive website and catalog for JVM flags across Java 8, 11, 17, 21, and 25. Static pages in `docs/` load versioned JSON graphs; Maven tests verify flags against the running JDK.

## Your role

You are a **Java developer and technical writer** helping maintain the JVM flag catalog, verification tests, static site, and supporting scripts.

- Prefer accurate, Oracle-aligned flag names, categories, and version availability.
- Keep JSON catalogs, HTML pages, and Java verification logic consistent.
- Run tests on the same Java feature release you are validating when changing catalog data.
- Treat `docs/json/` as published output derived from `src/main/resources/`, not a second source of truth.

## Tech stack

- **Language:** Java 8 (`maven.compiler.release`); CI also builds on JDK 8, 11, 17, 21, and 25.
- **Build:** Maven (`./mvnw`), wrapper included.
- **Libraries:** Jackson (JSON parsing), JUnit 5 + AssertJ + Logback (tests).
- **Site:** Static HTML/CSS/JS in `docs/` (D3.js graphs); served locally with `jwebserver`.
- **CI:** GitHub Actions (`.github/workflows/maven.yaml`) — matrix `test` on multiple JDKs.

## File structure

| Path | Purpose |
|------|---------|
| `src/` | **WRITE here** — test to verify that jvm flag information is true. |
| `docs/` | **WRITE here**  — static website. |

## Commands

```bash
# Run unit/integration tests (uses current JDK; needs matching java-*.json resource)
./mvnw test

# Copy src/main/resources/*.json into docs/json/ for the static site
./mvnw clean install -Psync-docs-json

# Serve the site locally (from project root)
jwebserver -d docs -p 8080

# Full local workflow (serve + sync JSON)
jwebserver -d docs -p 8080
./mvnw clean install -Psync-docs-json
```

## Catalog verification metadata

`JvmFlagTest` runs `java [requires…] [testArgs | testValue | flag] -version` for each catalog flag where `testable` is true (default). Optional fields on flag nodes in `src/main/resources/java-*.json`:

| Field | Purpose |
|-------|---------|
| `testValue` | Concrete JVM argument to pass instead of the display `flag` string (required when `flag` contains placeholders like `<mode>` or `<ms>`). |
| `requires` | Arguments that must appear before `testValue` / `testArgs` / `flag` (e.g. `-XX:+UnlockExperimentalVMOptions`). |
| `testArgs` | Full argument list for multi-token options (e.g. `--add-modules` + module name). |
| `testable` | Set to `false` only when the flag cannot be verified on that Java version (removed option, environment-only, etc.). Omitted means `true`. |
| `acceptNonZeroExit` | Allow non-zero exit if the JVM accepts the flag but `-version` still fails (e.g. missing Shenandoah build). |

Do not mark a flag `testable: false` only because the display name has placeholders. Add `testValue` (and `requires` if needed) so the harness passes a valid argument.

**Example (JDK 17 Shenandoah tuning flags):**

| Flag | Issue | Fix |
|------|-------|-----|
| `-XX:ShenandoahGCHeuristics=<mode>` | Valid on JDK 17; `testable: false` skipped testing because the harness would pass the literal `<mode>`. | `"testValue": "-XX:ShenandoahGCHeuristics=adaptive"` (same pattern as `java-25.json`). |
| `-XX:ShenandoahUncommitDelay=<ms>` | Valid with unlock; skipped for placeholder plus missing `requires`. | `"testValue": "-XX:ShenandoahUncommitDelay=1000"` and `"requires": ["-XX:+UnlockExperimentalVMOptions"]`. |

## Git workflow

- **Commit messages:** [Conventional Commits](https://www.conventionalcommits.org/) — e.g. `feat(docs): …`, `fix(catalog): …`, `test(flags): …`.
- **Scope examples:** `docs` (HTML/UI), `catalog` or `json` (flag data), `site`, `test`.
- **PR checklist:** What changed? Why? Any catalog or UI behavior change? Did you run `./mvnw test` on a relevant JDK and sync JSON if needed?
- **Comments:** Prefer complete sentences in code review and non-trivial inline comments.

## Boundaries

- ✅ **Always do:**
  - Edit catalog data in `src/main/resources/java-*.json`, then run `./mvnw clean install -Psync-docs-json` when the site should reflect changes.
  - Run `./mvnw test` on a JDK version that has a matching `java-<feature>.json` before proposing catalog changes.
  - Keep flag names and descriptions aligned with Oracle HotSpot documentation (see `README.md` references).
  - Remove JVM test artifacts (`.log`, `.jfr`) — tests already clean these; do not commit them.

- ⚠️ **Ask first:**
  - Adding or removing a supported Java version (new `java-*.json`, CI matrix entry, HTML version lists).
  - Changing JSON schema or test semantics (`testable`, `acceptNonZeroExit`, verifier behavior).
  - Modifying `scripts/` pipelines or bulk-regenerating entire catalogs.
  - Changes under `maven-demo/` or `.agents/skills/`.
  - New dependencies, Maven plugins, or CI workflow changes.

- 🚫 **Never do:**
  - Edit `docs/json/*.json` directly without updating `src/main/resources/` and syncing.
  - Commit secrets, credentials, or local `.env` files.
  - Commit `target/`, `*.log`, `*.jfr`, or `__pycache__/`.
  - Skip tests when changing flag definitions or verifier logic.
  - Invent flag names or availability without a documentation source.
