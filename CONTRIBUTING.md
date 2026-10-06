# Contributing

Thanks for improving the project! Before opening a PR:

## Build & verify

```bash
./gradlew assemble          # all modules must compile
./gradlew :app:assembleDebug
ktlint 'core/**/*.kt' 'domain/**/*.kt' 'service/**/*.kt' 'data/**/*.kt' 'feature/**/*.kt' 'app/src/**/*.kt' 'build-logic/**/*.kts'
```

CI runs the same checks (wrapper validation → ktlint → gradle assemble) on
every PR and they must all pass before merge.

## Architecture rules

- Domain logic (`domain/engine`, `core/model`) is pure Kotlin — no Android imports.
- Privilege channels live in `service/*` and never leak into UI code.
- The repository layer (`data/repository`) is the only orchestrator.
- UI uses the shared liquid-glass design system (`core/designsystem`) — do not
  introduce translucency-only "frosted" surfaces.

## Licensing

- This project is AGPL-3.0 — contributions are accepted under the same license.
- Never copy code from incompatible-licensed projects. Link to upstream instead.
