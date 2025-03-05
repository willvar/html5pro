android {
  namespace = "cn.willvar.html5pro"
  compileSdk = 35
  defaultConfig {
    applicationId = "cn.willvar.html5pro"
    minSdk = 26
    targetSdk = 35
    versionCode = 10000
    versionName = "1.0.0"
  }
  buildFeatures {
    buildConfig = true
  }
  buildTypes {
    debug {
      resValue("string", "distribution_platform", "debug")
    }
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  flavorDimensions += "platform"
  productFlavors {
    create("official") {
      dimension = "platform"
      resValue("string", "distribution_platform", "official")
    }
    create("huawei") {
      dimension = "platform"
      resValue("string", "distribution_platform", "huawei")
    }
    create("xiaomi") {
      dimension = "platform"
      resValue("string", "distribution_platform", "xiaomi")
    }
  }
  productFlavors.all {
    val flavors = productFlavors.filter { it.name != "official" }.joinToString(",") { it.name }
    buildConfigField("String", "PRODUCT_FLAVORS", "\"$flavors\"")
  }
}
dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.webkit)
  implementation(libs.material)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.okhttp)
}
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}