plugins {
    `android-application`
    kotlin("android")
    kotlin("kapt")
    hilt
}

androidAppConfig {
    defaultConfig {
        applicationId = "nick.camerafun"
        versionCode = 1
        versionName = "1.0"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", true)
            }
        }
    }

    ndkVersion = "21.4.7075529"
}

dependencies {
    implementation(Dependency.activity)
    implementation(Dependency.appCompat)
    implementation(Dependency.coreKtx)
    implementation(Dependency.vectorDrawable)
    implementation(Dependency.constraintLayout)
    implementation(Dependency.material)
    implementation(Dependency.photoView)
    implementation(Dependency.Navigation.runtime)
    implementation(Dependency.Navigation.fragment)
    implementation(Dependency.Navigation.ui)
    implementation(Dependency.Dagger.runtime)
    implementation(Dependency.Dagger.Hilt.runtime)
    implementation(Dependency.Retrofit.runtime)
    implementation(Dependency.Retrofit.moshi)
    implementation(Dependency.Moshi.runtime)
    implementation(Dependency.Room.runtime)
    implementation(Dependency.Room.roomKtx)
    implementation(Dependency.timber)
    implementation(Dependency.okhttpLoggingInterceptor)
    implementation(Dependency.multidex)
    implementation(Dependency.coil)
    implementation(Dependency.Camera.camera2)
    implementation(Dependency.Camera.lifecycle)
    implementation(Dependency.Camera.view)
    implementation(Dependency.Camera.extensions)
    implementation(Dependency.MLKit.bundledBarcodeScanning)

    kapt(Dependency.Room.compiler)
    kapt(Dependency.Dagger.compiler)
    kapt(Dependency.Dagger.Hilt.compiler)
}