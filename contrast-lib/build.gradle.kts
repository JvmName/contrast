import dev.jvmname.contrast.build.standardConfiguration

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

standardConfiguration()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("androidx.recyclerview:recyclerview:1.1.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }

}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}