import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val javaVer = JavaVersion.VERSION_16

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://dl.bintray.com/serpro69/maven-release-candidates/") }
}

val ktorVersion = "1.5.4"
val logbackVersion = "1.2.3"
val mongockVersion = "4.3.8"
val coroutinesVersion = "1.5.0-RC"

plugins {
    val kotlinVersion = "1.5.0-RC" // 1.5.0-RC
    application
    idea
    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

apply(plugin = "io.spring.dependency-management")


allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-devtools")


    implementation("io.zipkin.brave:brave-instrumentation-mongodb:5.13.3")
    implementation("io.micrometer:micrometer-core:1.6.6")

    implementation("me.paulschwarz:spring-dotenv:2.3.0")
    implementation("org.aspectj:aspectjweaver:1.9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")




    implementation("com.github.cloudyrock.mongock:mongock-spring-v5:$mongockVersion")
    implementation("com.github.cloudyrock.mongock:mongodb-springdata-v3-driver:$mongockVersion")

    implementation("org.ta4j:ta4j-core:0.14")
}

springBoot {
    mainClass.set("com.leantrace.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xskip-prerelease-check")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
