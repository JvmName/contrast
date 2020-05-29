import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

allprojects {
    group = "dev.jvmname"
    version = "0.0.1"

    repositories {
        google()
        mavenCentral()
        jcenter()

        // Workaround for potential name collisions with default module name (https://youtrack.jetbrains.com/issue/KT-36721)
        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
            val uniqueName = "${project.group}.${project.name}"

            kotlinExtension.targets.withType(KotlinNativeTarget::class.java) {
                compilations["main"].kotlinOptions.freeCompilerArgs += listOf("-module-name", uniqueName)
            }
        }
    }

}