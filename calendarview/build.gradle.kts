plugins {
    id("com.android.library")
}

android {
    namespace = "com.haibin.calendarview"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets["main"].java.srcDir("../CalendarView/calendarview/src/main/java")
    sourceSets["main"].res.srcDir("../CalendarView/calendarview/src/main/res")
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager:viewpager:1.0.0")
}
