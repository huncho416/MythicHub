plugins {
    id("java")
    id("application")
}

group = "mythic.hub"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Minestom core - stable working version (supports 1.20.6+)
    implementation("com.github.Minestom:Minestom:3172039f39")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Database dependencies
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")

    // Velocity proxy support
    implementation("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")

    // Configuration
    implementation("org.yaml:snakeyaml:2.2")

    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("mythic.hub.MythicHubServer")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}