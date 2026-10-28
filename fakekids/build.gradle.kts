plugins {
    id("com.android.application")
}

// ỨNG DỤNG GIẢ chỉ dùng để thử trên máy ảo CI: mang đúng tên gói của ứng dụng YouTube trên TV (com.google.android.youtube.tv) để kiểm tra
// launcher mở/chặn đúng. KHÔNG cài lên TV thật (APK này không nằm trong thư mục giao cho Sếp).
android {
    namespace = "com.minhminhhungyen.fakekids"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.google.android.youtube.tv"
        minSdk = 26
        targetSdk = 30
        versionCode = 1
        versionName = "0.0-fake"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

base {
    archivesName.set("FakeYTKids")
}
