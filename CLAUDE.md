# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ADA is a car dealership system (TPI Programación II): vehicle stock (automóviles, camionetas, motocicletas), sales tickets (boletos), employees, clients, a public frontoffice catalog, and an authenticated `/admin`.

- Java 17, Maven (`pom.xml` has `maven.compiler.release` 17 and no dependencies yet).
- The framework and the database are **not decided yet**. Don't add frameworks, dependencies, or database-specific code unless the user picks them. Ask first.

## Business rules: source of truth

- The full business rules (RN01–RN32) and design decisions (D01–D31) live in **GitHub Issue #1** of `concesionariaADA/ADA` (`gh issue view 1`). `README.md` has only part of the RN list and lags behind the Issue.
- Issues #2–#20 hold the work units. Each one lists its scope, pending decisions or blockers, and related Issues. Read the Issue and the RN/D entries it cites before implementing it.
- A pending decision (D##) is the user's call. Don't make it yourself in code.

## Conventions

- Identifiers (classes, methods, variables) are in **Spanish**, following the RN domain language, e.g. `Vehiculo`, `Unidad`, `Boleto`, `registrarVenta()`.
- Git: one branch per Issue (`issue-<N>-<slug>`) from `main`, then a PR into `main`. Use Conventional Commits and reference the Issue (`#N`).
