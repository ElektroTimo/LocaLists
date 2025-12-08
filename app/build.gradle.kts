plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
import java.util.Properties
        android {
            namespace = "com.example.localists"
            compileSdk = 33

            defaultConfig {
                applicationId = "com.example.localists"
                minSdk = 29
                targetSdk = 33
                versionCode = 1
                versionName = "1.0"

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                // load local.properties file
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                // Set API keys in BuildConfig in the most convoluted way possible due to gradle bug
                buildConfigField("String", "MAPS_API_KEY", "\"${properties.getProperty(" MAPS_API_KEY ")}\"")

                // Add this: Inject into manifest placeholder for ${MAPS_API_KEY} to make compiler shut up
                manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: ""
            }

            buildTypes {
                release {
                    isMinifyEnabled = true // Make sure this is enabled or gradle will throw a maps api error
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            kotlinOptions {
                jvmTarget = "1.8"
            }

            buildFeatures {
                viewBinding = true
                dataBinding = true // Add this line for data binding
                buildConfig = true  // Add this line or gradle will throw an error related to android manifest not having maps api
            }
        }

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.cardview:cardview:1.0.0") // NEW: For rounded rectangle container
}