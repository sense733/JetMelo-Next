import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val gitOutput: (List<String>) -> String? = { arguments ->
    runCatching {
        providers.exec { commandLine("git", *arguments.toTypedArray()) }.standardOutput.asText.get().trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

val appVersionName = gitOutput(listOf("describe", "--tags", "--dirty"))?.removePrefix("v") ?: "0.0.0-dev"
val appVersionCode = gitOutput(listOf("rev-list", "--count", "HEAD"))?.toIntOrNull()?.coerceAtLeast(1) ?: 1

android {
    namespace = "com.rcmiku.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jetmelo.next"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        register("release") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true

            fun credential(envName: String, propertyName: String): String? =
                System.getenv(envName)
                    ?: (project.findProperty(propertyName) as? String)
                    ?: localProperties.getProperty(propertyName)

            val storeFilePath = credential("RELEASE_STORE_FILE", "release.storeFile")
            val storePasswordVal = credential("RELEASE_STORE_PASSWORD", "release.storePassword")
            val keyAliasVal = credential("RELEASE_KEY_ALIAS", "release.keyAlias")
            val keyPasswordVal = credential("RELEASE_KEY_PASSWORD", "release.keyPassword")

            if (!storeFilePath.isNullOrBlank() && !storePasswordVal.isNullOrBlank() && !keyAliasVal.isNullOrBlank() && !keyPasswordVal.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            } else {
                logger.warn("未配置 release 签名密钥，release 产物将回落到 debug 签名，不可用于发布")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.version", // https://stackoverflow.com/a/58956288
                "META-INF/**/LICENSE.txt",
                "DebugProbesKt.bin", // https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
                "kotlin-tooling-metadata.json"
            )
            pickFirsts += "META-INF/androidx.compose.ui_ui.version" // For Layout Inspector
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobufKotlinLite.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
                register("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.ktor.serialization.json)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.reorderable)
    implementation(project(":ncmapi"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}