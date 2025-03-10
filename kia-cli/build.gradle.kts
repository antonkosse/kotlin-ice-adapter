import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    val picocliVersion = "4.7.4"
    val logbackVersion = "1.4.11"
    kapt("info.picocli:picocli-codegen:$picocliVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("com.github.faforever:JJsonRpc:37669e0fed")
    implementation("info.picocli:picocli:$picocliVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation(project(":kia-lib"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "com.faforever.ice.KiaApplication"))
        }
    }
}
