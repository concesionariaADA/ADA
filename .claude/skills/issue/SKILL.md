---
name: issue
description: Start work on a GitHub Issue of concesionariaADA/ADA — load its scope plus the cited business rules (RN) and decisions (D) from Issue #1, check blockers, and create the working branch from dev. Use as /issue <N>.
disable-model-invocation: true
---

Start work on Issue #$ARGUMENTS.

1. Read the Issue: `gh issue view $ARGUMENTS --repo concesionariaADA/ADA --comments`.
2. Read Issue #1 (`gh issue view 1 --repo concesionariaADA/ADA`) and extract only the RN## and D## entries that Issue #$ARGUMENTS cites. Quote them verbatim.
3. Check "Decisiones pendientes y bloqueos" and "Issues relacionadas". For each blocking Issue, check whether it is closed (`gh issue view <M> --json state,title`). If an open blocker or an unresolved D## affects the scope, list it and ask the user before writing code.
4. Check `git status`. If the tree is clean, create and switch to the branch `<name>/<short-kebab-slug-of-title>` from an up-to-date `dev` (`git fetch origin` first). `<name>` comes from the GitHub login (`gh api user -q .login`) using the table in `CLAUDE.md`: javierduartepy → `javier`, JuliAqno → `juli`, amaroisi → `amaro`; if the login is not listed, ask the user. If the tree isn't clean, stop and report.
5. Summarize for the user: scope, cited RN/D (verbatim), blockers, and a proposed implementation outline. Wait for confirmation before implementing.

Conventions: identifiers in Spanish; Conventional Commits referencing `#$ARGUMENTS`; the PR targets `dev` (only `dev` is merged into `main`).
