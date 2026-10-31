# multiplatformUtils

Shared Kotlin Multiplatform utilities used by the `todo` and `fiteoApp` applications.

## Status

This repository is being extracted from the applications. The shared module is built as a Gradle project module and is consumed by each application through a Git submodule at `multiplatformUtils/`.

## Development

The root project in this repository is the `multiplatformUtils` Gradle module source. It is included by the consuming application's Gradle settings:

```kotlin
include(":multiplatformUtils")
```

The module currently expects the consuming build to provide a compatible version catalog named `libs` with the aliases referenced by `build.gradle.kts`.

## Platforms

- Android
- Desktop JVM
- Kotlin common source set

## License

Apache License 2.0. See [LICENSE](LICENSE).
