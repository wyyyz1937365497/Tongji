plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.haibin.calendarview"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Standard source sets are used since we are now in the correct directory
    
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager:viewpager:1.0.0")
    testImplementation("junit:junit:4.13.2")
}
