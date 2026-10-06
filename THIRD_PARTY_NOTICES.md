# Third Party Notices

This file lists the third-party components this project depends on, their
licenses and copyright holders. Their license texts and notices are preserved
as required.

## Runtime dependencies

| Component | Version | License | Copyright |
|---|---|---|---|
| Android Liquid Glass (backdrop) | 2.0.1 | Apache-2.0 | © Kyant — https://github.com/kyant0/AndroidLiquidGlass |
| kyant shapes | 1.2.1 | Apache-2.0 | © Kyant — https://github.com/kyant0/Shapes |
| Shizuku (api + provider) | 13.1.5 | Apache-2.0 | © RikkaApps — https://github.com/RikkaApps/Shizuku |
| AndroidX (core, activity, lifecycle, compose, navigation, datastore) | per catalog | Apache-2.0 | © The Android Open Source Project |
| Material Components (Compose Material 3, icons) | per BOM | Apache-2.0 | © The Android Open Source Project |
| Kotlin / kotlinx.coroutines | 2.4.10 / 1.11.0 | Apache-2.0 | © JetBrains s.r.o. |
| kotlinx.serialization JSON | 1.11.0 | Apache-2.0 | © JetBrains s.r.o. |

## Build toolchain

| Component | License | Copyright |
|---|---|---|
| Android Gradle Plugin | Apache-2.0 | © The Android Open Source Project |
| Gradle | Apache-2.0 | © Gradle Inc. / the Gradle team |
| ktlint (CI only) | MIT | © ktlint contributors |

## Notes

- The liquid glass rendering stack (backdrop + shapes) is used as published
  Maven Central artifacts; no modifications are made to them.
- Shizuku is used as a library; the privileged server component is **not**
  bundled — users run the upstream Shizuku application separately, which keeps
  this repository free of any embedded privileged-code redistribution
  obligations.
- This project's own code is licensed under GNU AGPL-3.0 (see `LICENSE`).
  It is an original implementation; no source code from any third-party
  project is included in this repository.
