plugins {
    kotlin("jvm") version "1.3.72"
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation("com.android.tools.build:gradle:4.0.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
}