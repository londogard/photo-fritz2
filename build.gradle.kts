plugins {
    kotlin("multiplatform") version "1.6.10"
    application
    id("com.google.devtools.ksp") version "1.6.10-1.0.2"
}

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") // new repository here
    // maven("https://packages.jetbrains.team/maven/p/ki/maven")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "15"
    }
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }.binaries.executable()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("dev.fritz2:core:0.14.1")
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
                implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.6.10")
                // see https://components.fritz2.dev/
                implementation("dev.fritz2:components:0.14.1")
            }
        }

        val jsMain by getting {
            dependencies {
                // implementation("io.kinference:inference-core-js:0.1.10")
                implementation(npm("onnxruntime-web", "1.10.0", generateExternals = false))
            }
        }
    }
}

dependencies {
    add("kspMetadata", "dev.fritz2:lenses-annotation-processor:0.14.1")
}
kotlin.sourceSets.commonMain { kotlin.srcDir("build/generated/ksp/commonMain/kotlin") }
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspKotlinMetadata") dependsOn("kspKotlinMetadata")
}