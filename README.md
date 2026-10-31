# multiplatformUtils

Shared Kotlin Multiplatform utilities used by [todo](https://github.com/mariuszmarzec/todo) and [fiteoApp](https://github.com/mariuszmarzec/fiteoApp).

This repository is consumed as a **Git submodule**, not as a separately published Maven artifact. Both applications pin an exact commit of this repository.

## Integration

The submodule is checked out at the application root as `multiplatformUtils/`. The consuming Gradle project includes it as:

```kotlin
include(":multiplatformUtils")
```

The module build script uses the consuming application's version catalog (`libs`). Keep the aliases referenced by `build.gradle.kts` available in both applications.

Clone an application with its shared module:

```bash
git clone --recurse-submodules <application-repository>
```

For an existing checkout:

```bash
git submodule sync --recursive
git submodule update --init --recursive
```

## Application-owned resources

The shared module intentionally does not contain application assets or a concrete `ResourceLoaderImpl`.

Each consuming application's `:common` module owns its Compose resources and provides platform implementations of `com.marzec.resource.ResourceLoader` (Android and Desktop). This keeps application-specific files, such as feature-toggle defaults and media assets, in the application that owns them while sharing the resource-loading contract.

## Platforms

- Kotlin common
- Android
- Desktop JVM

## Build and tests

Run the module tests through either consuming application's Gradle build:

```bash
./gradlew :multiplatformUtils:desktopTest
```

The full application CI should also run the Android and application-specific tests.

## License

Apache-2.0. See [LICENSE](LICENSE).
