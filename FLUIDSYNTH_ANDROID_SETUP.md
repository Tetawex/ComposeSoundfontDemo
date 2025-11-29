# FluidSynth Android JNI Integration Guide

## Files Created

### C++ Code
- `composeApp/src/androidMain/cpp/CMakeLists.txt` - CMake build configuration
- `composeApp/src/androidMain/cpp/fluidsynth_wrapper.cpp` - JNI wrapper implementation

### Kotlin Code
- `composeApp/src/androidMain/kotlin/org/tetawex/cmpsftdemo/FluidSynthJNI.kt` - JNI interface declarations
- `composeApp/src/androidMain/kotlin/org/tetawex/cmpsftdemo/FluidSynthWrapper.kt` - Kotlin wrapper for easy access

### Build Configuration
- Updated `composeApp/build.gradle.kts` with NDK and CMake settings

## Next Steps

### 1. Add FluidSynth Dependency

You need to either:

#### Option A: Use Pre-built AAR (Recommended)
Add to `composeApp/build.gradle.kts`:
```kotlin
dependencies {
    // FluidSynth for Android
    implementation("org.fluidsynth:fluidsynth-android:2.3.3")
}
```

#### Option B: Build from Source
1. Clone FluidSynth repository
2. Build for Android NDK with `cmake`
3. Copy `libfluidsynth.so` to `composeApp/src/androidMain/jniLibs/<abi>/`

### 2. Set Up Local.properties (if needed)

Create or update `local.properties`:
```properties
sdk.dir=/path/to/Android/SDK
ndk.dir=/path/to/Android/NDK
```

### 3. Build the Project

```bash
# Build debug APK
./gradlew.bat :composeApp:assembleDebug

# Or from Android Studio: Build → Make Project
```

## Usage Example

```kotlin
// In your Activity or Composable
val synth = FluidSynthWrapper()

// Initialize
if (synth.initialize()) {
    // Load a SoundFont
    val sfontId = synth.loadSoundFont("/path/to/soundfont.sf2")
    
    // Set instrument on channel 0 to program 0 (Piano)
    synth.changeProgram(0, 0)
    
    // Play a note
    synth.playNote(0, 60, 100)  // Middle C, velocity 100
    
    // Stop the note
    synth.stopNote(0, 60)
    
    // Clean up
    synth.release()
}
```

## Available Methods

### FluidSynthJNI (Low-level)
- `createSynth(): Long` - Create synthesizer
- `destroySynth(synthHandle: Long)` - Destroy synthesizer
- `loadSoundFont(synthHandle: Long, filePath: String): Int` - Load .sf2 file
- `noteOn(synthHandle: Long, channel: Int, note: Int, velocity: Int): Int` - Play note
- `noteOff(synthHandle: Long, channel: Int, note: Int): Int` - Stop note
- `programChange(synthHandle: Long, channel: Int, program: Int): Int` - Change instrument
- `setChannelVolume(synthHandle: Long, channel: Int, volume: Int): Int` - Set volume (CC 7)
- `controlChange(synthHandle: Long, channel: Int, controller: Int, value: Int): Int` - Send MIDI CC
- `getVersion(): String` - Get FluidSynth version

### FluidSynthWrapper (High-level)
- `initialize(): Boolean` - Initialize synth
- `loadSoundFont(filePath: String): Int` - Load .sf2 file
- `playNote(channel: Int, note: Int, velocity: Int): Boolean` - Play note
- `stopNote(channel: Int, note: Int): Boolean` - Stop note
- `changeProgram(channel: Int, program: Int): Boolean` - Change instrument
- `setVolume(channel: Int, volume: Int): Boolean` - Set volume
- `sendControlChange(channel: Int, controller: Int, value: Int): Boolean` - Send MIDI CC
- `release()` - Clean up resources

## Troubleshooting

### CMake errors
- Ensure NDK is properly installed: `Android SDK Manager → SDK Tools → NDK`
- Check `local.properties` has correct paths

### Library not found
- Verify FluidSynth AAR is properly imported
- Check `libfluidsynth_wrapper.so` is built in `build/intermediates/cmake/...`

### Native code crashes
- Check Logcat: `adb logcat | grep FluidSynthJNI`
- Verify SoundFont file exists and is readable
- Ensure correct MIDI channel/note/velocity ranges

### Permission issues
- Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Architecture Support

By default, CMake will build for all supported ABIs:
- arm64-v8a (64-bit ARM)
- armeabi-v7a (32-bit ARM)
- x86_64 (Intel 64-bit)
- x86 (Intel 32-bit)

To limit to specific ABIs, update `build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }
}
```

## FluidSynth JNI Wrapper Details

The C++ wrapper handles:
- Instance management with handle-based state tracking
- Exception handling and Android logging
- JNI string marshalling for file paths
- MIDI parameter validation

All methods return proper error codes compatible with FluidSynth conventions:
- `FLUID_OK` (0) = success
- `FLUID_FAILED` (-1) = failure
