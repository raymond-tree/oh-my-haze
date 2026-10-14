import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ohmyhaze"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ohmyhaze"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Never persist signing secrets in Gradle properties or source control.
    val uploadStorePath = providers.environmentVariable("OH_MY_HAZE_KEYSTORE_PATH").orNull
    val uploadStorePassword = providers.environmentVariable("OH_MY_HAZE_STORE_PASSWORD").orNull
    val uploadKeyAlias = providers.environmentVariable("OH_MY_HAZE_KEY_ALIAS").orNull
    val uploadKeyPassword = providers.environmentVariable("OH_MY_HAZE_KEY_PASSWORD").orNull
    val uploadValues = listOf(uploadStorePath, uploadStorePassword, uploadKeyAlias, uploadKeyPassword)
    require(uploadValues.all { it == null } || uploadValues.all { !it.isNullOrBlank() }) {
        "Release signing requires all four OH_MY_HAZE signing environment variables. See docs/android-release.md."
    }
    val uploadStore = uploadStorePath?.let { path ->
        val externalFile = File(path)
        require(externalFile.isAbsolute && externalFile.isFile) {
            "OH_MY_HAZE_KEYSTORE_PATH must identify an existing absolute keystore path."
        }
        require(!externalFile.canonicalFile.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) {
            "The upload keystore must be stored outside the Git repository."
        }
        externalFile
    }
    signingConfigs {
        create("release") {
            storeFile = uploadStore
            storePassword = uploadStorePassword
            keyAlias = uploadKeyAlias
            keyPassword = uploadKeyPassword
        }
    }
    val validateReleaseSigningEnvironment = tasks.register("validateReleaseSigningEnvironment") {
        group = "verification"
        description = "Refuse release packaging without the external upload signing credentials."
        doLast {
            check(uploadValues.all { !it.isNullOrBlank() }) {
                "Release signing credentials are missing. Run scripts/android-release.sh build in your terminal."
            }
        }
    }
    tasks.configureEach {
        if (name in setOf("bundleRelease", "assembleRelease", "packageRelease", "packageReleaseBundle", "signReleaseBundle")) {
            dependsOn(validateReleaseSigningEnvironment)
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            // The validation task also blocks packaging when AGP considers
            // an incomplete signing config unsigned.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.espresso.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
