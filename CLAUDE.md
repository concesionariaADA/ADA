# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ADA is a car dealership system (TPI Programación II): vehicle stock (automóviles, camionetas, motocicletas), sales tickets (boletos), employees, clients, a public frontoffice catalog, and an authenticated `/admin`.

- Java 17, Maven (`pom.xml` has `maven.compiler.release` 17).
- Database: PostgreSQL 17 in Docker (`docker-compose.yml`, with pgAdmin). Settings live in a local `.env` copied from `.env.example` and are read through `dotenv-java` (`com.ada.persistencia.ConexionBaseDatos`). Approved dependencies: PostgreSQL JDBC driver and `dotenv-java`.
- Stack (Issue #1, D03a/D03b/D04): plain classic Java with no frameworks (no Spring). The backend uses the JDK `HttpServer` and plain JDBC. The frontend in `frontend/` is HTML + Bootstrap 5 + vanilla JS (no React or Node), styled after meucci.com.ar (#14, #25). A library is allowed only if it stays isolated in one clearly identified place; ask before adding any dependency.

## Business rules: source of truth

- The full business rules (RN01–RN32) and design decisions (D01–D31) live in **GitHub Issue #1** of `concesionariaADA/ADA` (`gh issue view 1`). `README.md` has only part of the RN list and lags behind the Issue.
- Issues #2–#20 hold the work units. Each one lists its scope, pending decisions or blockers, and related Issues. Read the Issue and the RN/D entries it cites before implementing it.
- A pending decision (D##) is the user's call. Don't make it yourself in code.

## Conventions

- Identifiers (classes, methods, variables) are in **Spanish**, following the RN domain language, e.g. `Vehiculo`, `Unidad`, `Boleto`, `registrarVenta()`.
- Git branches:
  - `main`: stable branch. It only receives merges from `dev`.
  - `dev`: integration (pre-load) branch where the team's work is combined and tested before it reaches `main`.
  - Working branches are named `<name>/<topic>` (for example `javier/registros-bd`), created from an up-to-date `dev`, with a PR into `dev`. Names by GitHub login: `javier` (javierduartepy), `juli` (JuliAqno), `amaro` (amaroisi).
  - Flow: `<name>/<topic>` → PR into `dev` → `dev` merged into `main` once verified.
- Use Conventional Commits and reference the Issue (`#N`).
