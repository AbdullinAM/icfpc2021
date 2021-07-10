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
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ru.spbstu:jackson-module-ktuples:0.0.0.6")
    implementation("ru.spbstu:kotlin-wheels-jvm:0.0.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
    implementation("io.github.libktx:ktx-box2d:1.10.0-b1")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:1.9.14")
    implementation("com.badlogicgames.gdx:gdx-box2d:1.9.14")
    testImplementation(kotlin("test"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.spbstu.icpfc2021.MainKt")
}