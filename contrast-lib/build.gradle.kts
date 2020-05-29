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