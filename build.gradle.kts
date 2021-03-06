plugins {
    java
    kotlin("jvm") version "1.5.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://maven.vorpal-research.science") }
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ru.spbstu:jackson-module-ktuples:0.0.0.6")
    implementation("ru.spbstu:kotlin-wheels-jvm:0.0.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")

    implementation("com.github.breandan:kaliningraph:0.1.7")

    testImplementation(kotlin("test"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    val mainClassName = project.findProperty("mainClass") as? String ?: "ru.spbstu.icpfc2021.IntervalFuzzerKt"
    mainClass.set(mainClassName)
}
