import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.apollo)
}

// local.properties laden – das Client-Secret bleibt lokal und wird NICHT eingecheckt.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String =
    localProps.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace = "de.bibeltv.mediathek"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.bibeltv.mediathek"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true

        // App-v1 läuft gegen BETA (Entscheidung des Product Owners).
        buildConfigField("String", "VIDEOHUB_GRAPHQL_URL", "\"https://videohub.beta.bibeltv.de/graphql\"")
        buildConfigField("String", "KEYCLOAK_TOKEN_URL", "\"https://auth.beta.bibeltv.de/realms/Videohub/protocol/openid-connect/token\"")
        buildConfigField("String", "VIDEOHUB_CLIENT_ID", "\"video-hub-app\"")
        // M2M-Token (client_credentials) – KEIN Nutzer-Login. Secret im BuildConfig ist DEV-Übergang
        // gegen Beta; vor Release durch eigenen Keycloak Public-Client + PKCE ersetzen.
        buildConfigField("String", "VIDEOHUB_CLIENT_SECRET", "\"${secret("VIDEOHUB_CLIENT_SECRET")}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

apollo {
    service("videohub") {
        packageName.set("de.bibeltv.mediathek.graphql")
        // Custom-Scalars als String behandeln (ISO-DateTime / freies JSON)
        mapScalar("DateTime", "kotlin.String")
        mapScalar("Json", "kotlin.String")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // GraphQL (Apollo) gegen VideoHub Beta
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.okhttp)

    // Bilder (imgix-CDN)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Player (Media3 / ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)

    // Paging (Browse / Search über die 20.000+ Videos)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Einstellungen (Theme / eigene Zeiten) persistent
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
