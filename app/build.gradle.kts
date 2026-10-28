plugins {
    id("com.android.application")
}

android {
    namespace = "com.minhminhhungyen.kidstv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minhminhhungyen.kidstv"
        minSdk = 26
        // targetSdk 30: báo thức chính xác (AlarmManager) không cần quyền riêng trên Android 12 của TV.
        targetSdk = 30
        versionCode = 1
        versionName = "1.0.0"
    }

    // Khóa ký cố định, nằm trong repo riêng tư: bản cài sau cài đè được bản trước mà giữ nguyên cấu hình.
    signingConfigs {
        create("kids") {
            storeFile = file("kids-tv.jks")
            storePassword = "mmhykids"
            keyAlias = "kidstv"
            keyPassword = "mmhykids"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("kids")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("kids")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Tên tệp đầu ra: MMHY-KidsTV-debug.apk / MMHY-KidsTV-release.apk
base {
    archivesName.set("MMHY-KidsTV")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // org.json thật cho bài kiểm tra đọc thời tiết (bản trong android.jar chỉ là khung rỗng).
    testImplementation("org.json:json:20240303")
}
