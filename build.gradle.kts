plugins {
    java
    kotlin("jvm") version "1.5.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://maven.vorpal-research.science") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ru.spbstu:jackson-module-ktuples:0.0.0.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12")
    testImplementation(kotlin("test"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}