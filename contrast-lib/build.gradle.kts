import dev.jvmname.contrast.build.standardConfiguration

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

standardConfiguration()

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
        }
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.amshove.kluent:kluent:1.61")
                implementation("io.mockk:mockk-common:1.10.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                dependsOn(jvmMain)
                implementation("androidx.recyclerview:recyclerview:1.2.0-alpha03")
            }
        }
    }

    android {
        publishLibraryVariants("release")
    }
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}