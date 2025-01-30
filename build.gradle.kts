plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "dev.jombi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.dv8tion:JDA:5.2.2") {
        exclude("club.minnced", "opus-java-natives")
    }
    runtimeOnly(files("opus-java-natives-1.1.1.jar"))
    implementation("club.minnced:jda-ktx:0.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")

    implementation("ch.qos.logback:logback-classic:1.5.15")
    
    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.soywiz.korge:korge-core:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
