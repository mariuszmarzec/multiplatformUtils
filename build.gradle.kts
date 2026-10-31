import org.jetbrains.compose.ComposePlugin.DesktopComponentsDependencies
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.kotlin.multiplatform.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.atomicfu")
}

compose.resources {
    packageOfResClass = "com.marzec.resource.generated.resources"
}

atomicfu {
    // This module only uses kotlinx.atomicfu.locks; bytecode transformation is not needed for JVM/Android.
    // Disable the legacy transform task that assumes the old KMP androidMainClasses task.
    transformJvm = false
}

kotlin {
    android {
        namespace = "com.marzec"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JVM_17)
        }
        androidResources {
            enable = true
        }
    }
    jvm("desktop") {
        testRuns["test"].executionTask.configure {
            useJUnit()
            systemProperty("java.awt.headless", "true")
        }
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation(compose.components.resources)
                api(libs.kotlinStdlib)
                api(libs.coroutineCore)
                api(libs.ktorClient)
                api(libs.okHttpClientLogger)
                api(libs.ktorSerialization)
                api(libs.ktorContentNegotiation)
                api(libs.serializationJson)
                api(libs.quickMvi)
                api(libs.quickMvi.compose)
                api(libs.quickMvi.navigation)
                implementation(libs.kotlinDateTime)
                implementation(libs.compose.icons)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(kotlin("test"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
                implementation(libs.ktorClientMock)
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.core:core-ktx:1.3.1")
                implementation(libs.androidxAppCompat)
                implementation(libs.androidMaterial)
                implementation(libs.activityCompose)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.uiTooling)
                implementation(compose.foundation)
                api(libs.ktorOkHttpClient)
                implementation(libs.imageLoader)
                implementation(libs.exoPlayer)
                // data store
                api(libs.datastore.preferences)
                api(libs.datastore)
            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(libs.ktorOkHttpClient)
                api(libs.vlc)
                implementation(compose.desktop.currentOs)
                @OptIn(ExperimentalComposeLibrary::class)
                api(DesktopComponentsDependencies.animatedImage)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JVM_17)
        freeCompilerArgs.addAll(
            "-Xallow-jvm-ir-dependencies",
            "-Xskip-prerelease-check"
        )
    }
}
