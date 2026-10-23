plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  id("com.google.gms.google-services") version "4.4.4" apply false
}

// Opt-in, separate debug package for first-run QA. It cannot access the player's Room data or cloud.
val isolatedNovaQa = providers.gradleProperty("NOVA_ISOLATED_QA").orElse("false").get().toBoolean()
val hasFirebaseConfig = file("google-services.json").isFile && !isolatedNovaQa
if (hasFirebaseConfig) apply(plugin = "com.google.gms.google-services")
val productionRewardedAdUnit = providers.gradleProperty("ADMOB_REWARDED_AD_UNIT_ID").orElse("ca-app-pub-1910491454764587/7433492171").get()
// IDs must match products created by the account owner in Play Console; blank keeps checkout off.
val vipProductId = providers.gradleProperty("PLAY_VIP_SUBSCRIPTION_ID").orElse("").get()
val autoSellProductId = providers.gradleProperty("PLAY_AUTOSELL_PRODUCT_ID").orElse("").get()
val purchaseVerificationUrl = providers.gradleProperty("PLAY_PURCHASE_VERIFICATION_URL").orElse("").get()
listOf(vipProductId, autoSellProductId).forEach { require(it.isEmpty() || Regex("[a-z0-9_.]+").matches(it)) }
require(purchaseVerificationUrl.isEmpty() || purchaseVerificationUrl.startsWith("https://"))
require(purchaseVerificationUrl.none { it == '"' || it == '\\' || it == '\n' || it == '\r' })
require(productionRewardedAdUnit.isEmpty() || Regex("ca-app-pub-[0-9]+/[0-9]+").matches(productionRewardedAdUnit))

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.idletycoon.nxtyc"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0"
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"nexus-idle-tycoon\"")
    buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())
    buildConfigField("String", "PLAY_VIP_SUBSCRIPTION_ID", "\"$vipProductId\"")
    buildConfigField("String", "PLAY_AUTOSELL_PRODUCT_ID", "\"$autoSellProductId\"")
    buildConfigField("String", "PLAY_PURCHASE_VERIFICATION_URL", "\"$purchaseVerificationUrl\"")
    buildConfigField("boolean", "ITCH_BETA", "false")
    if (!hasFirebaseConfig) resValue("string", "default_web_client_id", "")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    debug {
      if (isolatedNovaQa) applicationIdSuffix = ".novaqa"
      manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
      buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
      buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
    }
    release {
      manifestPlaceholders["admobAppId"] = "ca-app-pub-1910491454764587~5605387336"
      buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-1910491454764587~5605387336\"")
      buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"$productionRewardedAdUnit\"")
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    create("itchBeta") {
      initWith(getByName("release"))
      isDebuggable = false
      versionNameSuffix = "-itch-beta.2"
      buildConfigField("boolean", "ITCH_BETA", "true")
      buildConfigField("String", "ADMOB_APP_ID", "\"\"")
      buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"\"")
      buildConfigField("String", "PLAY_VIP_SUBSCRIPTION_ID", "\"\"")
      buildConfigField("String", "PLAY_AUTOSELL_PRODUCT_ID", "\"\"")
      buildConfigField("String", "PLAY_PURCHASE_VERIFICATION_URL", "\"\"")
      manifestPlaceholders["admobAppId"] = ""
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    resValues = true
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Custom release-derived builds need their local unit-test component enabled explicitly.
androidComponents {
  beforeVariants(selector().withBuildType("itchBeta")) { variant ->
    variant.hostTests.values.filter {
      it.type == com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE
    }.forEach { it.enable = true }
  }
}

// Android 16's Robolectric runtime requires Java 21, even when compilation uses JDK 17.
tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
  })
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
  implementation("com.google.android.ump:user-messaging-platform:4.0.0")
  implementation("com.android.billingclient:billing:8.3.0")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.play.services.auth)
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
