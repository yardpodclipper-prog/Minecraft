plugins {
    id("fabric-loom") version "1.8.12"
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            runDir = "run/client"
        }
        named("server") {
            server()
            configName = "Fabric Server"
            runDir = "run/server"
        }
        create("dev") {
            client()
            configName = "Fabric Dev"
            runDir = "run/dev"
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.115.6+1.21.1")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("runClientDev") {
    group = "fabric"
    description = "Alias for loom dev client run."
    dependsOn("runDev")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
