
val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val exposed_version: String by project
val h2_version: String by project
plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("app.cash.sqldelight") version "2.0.0"
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.github.johnrengelman.shadow") version "7.1.2"

}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    google()
    mavenCentral()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation ("io.ktor:ktor-client-gson:$ktor_version")

    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-caching-headers-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-auto-head-response-jvm")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-host-common-jvm:2.3.7")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.7")
    implementation("io.ktor:ktor-server-sessions-jvm:2.3.7")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.7")
    //jwt
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    // database
    implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
    implementation("app.cash.sqldelight:primitive-adapters:2.0.0-alpha05")

    //kotlin inject
    ksp("me.tatarka.inject:kotlin-inject-compiler-ksp:0.6.3")
    implementation("me.tatarka.inject:kotlin-inject-runtime:0.6.3")

    //kotlin time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    implementation("io.arrow-kt:arrow-core:1.2.0")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.7")

    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    implementation("org.xerial:sqlite-jdbc:3.34.0") // Replace with the latest version

}

//task fatJar(type: Jar) {
//    manif«est {
//        attributes 'Main-Class': mainClassName
//    }
//    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
//    with jar
//}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.example.ApplicationKt"
    }
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }

    docker {
        jreVersion.set(JavaVersion.VERSION_18)
        localImageName.set("sample-docker-image")
        imageTag.set("0.0.1-preview")
        portMappings.set(listOf(
            io.ktor.plugin.features.DockerPortMapping(
                80,
                8080,
                io.ktor.plugin.features.DockerPortMappingProtocol.TCP
            )
        ))

        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
                appName = provider { "ktor-app" },
                username = providers.environmentVariable("DOCKER_HUB_USERNAME"),
                password = providers.environmentVariable("DOCKER_HUB_PASSWORD")
            )
        )
    }
}
