plugins {
    java
    kotlin("jvm") version "1.5.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.vorpal-research.science")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ru.spbstu:kotlin-wheels-jvm:0.0.1.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}