import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val frameworkPath = "${projectDir}/frameworks"
        val (fluidSynthFwPath, sdl3FwPath) = when (iosTarget.name) {
            "iosArm64" -> Pair(
                "$frameworkPath/FluidSynth.xcframework/ios-arm64",
                "$frameworkPath/SDL3.xcframework/ios-arm64"
            )
            "iosSimulatorArm64" -> Pair(
                "$frameworkPath/FluidSynth.xcframework/ios-arm64_x86_64-simulator",
                "$frameworkPath/SDL3.xcframework/ios-arm64_x86_64-simulator"
            )
            else -> throw IllegalArgumentException("Unsupported target: ${iosTarget.name}")
        }
        
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            
            // Link FluidSynth and SDL3 frameworks with architecture-specific paths
            linkerOpts += "-F$fluidSynthFwPath"
            linkerOpts += "-F$sdl3FwPath"
            linkerOpts += "-framework"
            linkerOpts += "FluidSynth"
            linkerOpts += "-framework"
            linkerOpts += "SDL3"
            linkerOpts += "-framework"
            linkerOpts += "CoreFoundation"
            linkerOpts += "-framework"
            linkerOpts += "AudioToolbox"
            linkerOpts += "-framework"
            linkerOpts += "CoreAudio"
        }
        
        // Configure C interop for FluidSynth and SDL3
        iosTarget.compilations.getByName("main") {
            cinterops {
                val fluidsynth by creating {
                    defFile = project.file("src/iosMain/interop/fluidsynth.def")
                    packageName = "org.tetawex.cmpsftdemo.fluidsynth"
                    
                    compilerOpts += listOf(
                        "-F$fluidSynthFwPath",
                        "-F$sdl3FwPath"
                    )
                    includeDirs.allHeaders(
                        "$fluidSynthFwPath/FluidSynth.framework/Headers"
                    )
                }
                val sdl3 by creating {
                    defFile = project.file("src/iosMain/interop/sdl3.def")
                    packageName = "org.tetawex.cmpsftdemo.sdl3"
                    
                    compilerOpts += listOf(
                        "-F$sdl3FwPath"
                    )
                    includeDirs.allHeaders(
                        "$sdl3FwPath/SDL3.framework/Headers"
                    )
                }
            }
        }
    }
    
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "org.tetawex.cmpsftdemo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "org.tetawex.cmpsftdemo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
                cppFlags.add("-fexceptions")
                cppFlags.add("-frtti")
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }

//        ndk {
//            abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
//        }
    }

    externalNativeBuild {
        cmake {
            path("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.tetawex.cmpsftdemo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.tetawex.cmpsftdemo"
            packageVersion = "1.0.0"
        }
    }
}
