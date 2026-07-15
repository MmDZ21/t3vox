plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.toosarax.ts3client"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.toosarax.ts3client"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Patched local ts3j: upstream jar minus LocalClientHandlerConnecting.class, which is
    // replaced by our fixed copy in app/src/main/java (Init1 puzzle arraycopy crash).
    implementation(files("libs/ts3j-db57d60-patched.jar"))
    implementation("commons-lang:commons-lang:2.6")
    implementation("org.ini4j:ini4j:0.5.4")
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    implementation("io.github.jaredmdobson:concentus:1.0.2")
    implementation("dnsjava:dnsjava:3.6.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

afterEvaluate {
    tasks.named<Test>("testDebugUnitTest").configure {
        filter {
            excludeTestsMatching("com.toosarax.ts3client.protocol.LiveConnectionDebugTest")
        }
    }

    tasks.register<JavaExec>("liveConnectDebug") {
        group = "verification"
        description = "Live TS3 connection debug (prints handshake steps to the console)"
        dependsOn("compileDebugUnitTestKotlin")
        val unitTest = tasks.named<Test>("testDebugUnitTest").get()
        classpath = unitTest.classpath
        mainClass.set("com.toosarax.ts3client.protocol.LiveConnectionDebugMainKt")
        isIgnoreExitValue = true
    }
}
