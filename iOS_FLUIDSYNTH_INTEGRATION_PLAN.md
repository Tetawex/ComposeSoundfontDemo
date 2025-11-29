# iOS FluidSynth Integration Plan

## Overview
Integration plan for adding FluidSynth support to the Compose Multiplatform iOS target using prebuilt binaries:
- `SDL3.xcframework` 
- `FluidSynth.xcframework`

This document covers two viable approaches for Kotlin/Native bindings and framework linking.

---

## PHASE 1: Project Structure Setup

### 1.1 Create Directory Structure
```
composeApp/
├── src/
│   └── iosMain/
│       ├── kotlin/org/tetawex/cmpsftdemo/
│       │   ├── SynthManager.ios.kt (already exists - stub)
│       │   └── FluidSynthInterop.kt (new)
│       └── interop/ (new directory)
│           └── fluidsynth.def (new - C interop definitions)
└── frameworks/ (new directory)
    ├── FluidSynth.xcframework/
    └── SDL3.xcframework/

iosApp/
└── iosApp.xcodeproj/
    └── project.pbxproj (will be modified)
```

---

## PHASE 2: Framework Integration

### 2.1 Copy XCFrameworks
**Action:** Place the frameworks in the project:

**Option A: Within composeApp (Recommended)**
```
composeApp/frameworks/
├── FluidSynth.xcframework/
└── SDL3.xcframework/
```

**Option B: Within iosApp (Alternative)**
```
iosApp/Frameworks/
├── FluidSynth.xcframework/
└── SDL3.xcframework/
```

**Recommendation:** Option A keeps frameworks closer to the Kotlin code that uses them.

---

## PHASE 3: Kotlin/Native C Interop - Two Approaches

### APPROACH 1: Direct C Interop (Recommended - More Control)

#### 3.1 Create `fluidsynth.def` file
**Location:** `composeApp/src/iosMain/interop/fluidsynth.def`

```properties
language = C
headers = fluidsynth.h
headerFilter = fluidsynth.h fluidsynth/**

# Path to frameworks (adjust based on your structure)
compilerOpts = -F../../frameworks -framework FluidSynth -framework SDL3
linkerOpts = -F../../frameworks -framework FluidSynth -framework SDL3 -framework CoreFoundation

# Static framework mode (if frameworks are static)
# staticLibraries = libfluidsynth.a
```

#### 3.2 Update `build.gradle.kts` for cinterop
Add to the kotlin block:

```kotlin
kotlin {
    // ... existing targets ...
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            
            // Link frameworks
            linkerOpts += "-F${projectDir}/frameworks"
            linkerOpts += "-framework FluidSynth"
            linkerOpts += "-framework SDL3"
            linkerOpts += "-framework CoreFoundation"
            linkerOpts += "-rpath @executable_path/Frameworks"
        }
        
        // Configure C interop
        iosTarget.compilations.getByName("main") {
            cinterops {
                val fluidsynth by creating {
                    defFile = project.file("src/iosMain/interop/fluidsynth.def")
                    packageName = "org.tetawex.cmpsftdemo.fluidsynth"
                    
                    // Framework search paths
                    compilerOpts += listOf(
                        "-F${projectDir}/frameworks/FluidSynth.xcframework/ios-arm64",
                        "-F${projectDir}/frameworks/SDL3.xcframework/ios-arm64"
                    )
                    
                    // Include paths
                    includeDirs.allHeaders(
                        "${projectDir}/frameworks/FluidSynth.xcframework/ios-arm64/FluidSynth.framework/Headers"
                    )
                }
            }
        }
    }
}
```

#### 3.3 Create Kotlin Wrapper (`FluidSynthInterop.kt`)
**Location:** `composeApp/src/iosMain/kotlin/org/tetawex/cmpsftdemo/FluidSynthInterop.kt`

```kotlin
package org.tetawex.cmpsftdemo

import kotlinx.cinterop.*
import org.tetawex.cmpsftdemo.fluidsynth.*
import platform.Foundation.*

/**
 * Kotlin wrapper around FluidSynth C API
 */
class FluidSynthNative {
    private var settings: CPointer<fluid_settings_t>? = null
    private var synth: CPointer<fluid_synth_t>? = null
    private var audioDriver: CPointer<fluid_audio_driver_t>? = null
    
    fun initialize(): Boolean {
        // Create settings
        settings = new_fluid_settings() ?: return false
        
        // Configure settings for iOS/SDL3
        fluid_settings_setstr(settings, "audio.driver", "sdl3")
        fluid_settings_setint(settings, "synth.polyphony", 256)
        fluid_settings_setint(settings, "synth.midi-channels", 16)
        fluid_settings_setnum(settings, "synth.gain", 0.8)
        
        // Create synth
        synth = new_fluid_synth(settings) ?: return false
        
        // Create audio driver
        audioDriver = new_fluid_audio_driver(settings, synth)
        
        return audioDriver != null
    }
    
    fun loadSoundFont(path: String): Int {
        return synth?.let { 
            fluid_synth_sfload(it, path, 1) 
        } ?: -1
    }
    
    fun noteOn(channel: Int, note: Int, velocity: Int): Int {
        return synth?.let {
            fluid_synth_noteon(it, channel, note, velocity)
        } ?: -1
    }
    
    fun noteOff(channel: Int, note: Int): Int {
        return synth?.let {
            fluid_synth_noteoff(it, channel, note)
        } ?: -1
    }
    
    fun programChange(channel: Int, program: Int): Int {
        return synth?.let {
            fluid_synth_program_change(it, channel, program)
        } ?: -1
    }
    
    fun setGain(gain: Float) {
        synth?.let {
            fluid_synth_set_gain(it, gain)
        }
    }
    
    fun cleanup() {
        audioDriver?.let { delete_fluid_audio_driver(it) }
        synth?.let { delete_fluid_synth(it) }
        settings?.let { delete_fluid_settings(it) }
        
        audioDriver = null
        synth = null
        settings = null
    }
}
```

#### 3.4 Implement `SynthManager.ios.kt`

```kotlin
package org.tetawex.cmpsftdemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

class IOSSynthManager : SynthManager {
    private val fluidSynth = FluidSynthNative()
    private var initialized = false
    private val currentChannel = 0
    
    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                if (!fluidSynth.initialize()) {
                    NSLog("Failed to initialize FluidSynth")
                    return@withContext false
                }
                
                // Try to load soundfont from bundle
                val soundfontPath = getSoundfontPath()
                if (soundfontPath != null) {
                    val result = fluidSynth.loadSoundFont(soundfontPath)
                    if (result == -1) {
                        NSLog("Failed to load soundfont from: $soundfontPath")
                    } else {
                        NSLog("Soundfont loaded successfully: $soundfontPath")
                    }
                }
                
                initialized = true
                true
            } catch (e: Exception) {
                NSLog("Error initializing synth: ${e.message}")
                false
            }
        }
    }
    
    override fun playNote(note: Int, velocity: Int) {
        if (!initialized) return
        fluidSynth.noteOn(currentChannel, note, velocity)
    }
    
    override fun stopNote(note: Int) {
        if (!initialized) return
        fluidSynth.noteOff(currentChannel, note)
    }
    
    override fun changeProgram(program: Int) {
        if (!initialized) return
        fluidSynth.programChange(currentChannel, program)
    }
    
    override fun setVolume(volume: Int) {
        if (!initialized) return
        val gain = volume / 127.0f
        fluidSynth.setGain(gain)
    }
    
    override fun isInitialized(): Boolean = initialized
    
    override fun cleanup() {
        fluidSynth.cleanup()
        initialized = false
    }
    
    private fun getSoundfontPath(): String? {
        val bundle = NSBundle.mainBundle
        return bundle.pathForResource("default", ofType = "sf2")
    }
}

actual fun getSynthManager(): SynthManager = IOSSynthManager()
```

---

### APPROACH 2: Objective-C Wrapper + Kotlin (Alternative - More iOS-like)

#### 3.1 Create Objective-C Wrapper
**Location:** `iosApp/iosApp/FluidSynthWrapper.h`

```objc
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface FluidSynthWrapper : NSObject

- (BOOL)initialize;
- (int)loadSoundFont:(NSString *)path;
- (void)noteOn:(int)channel note:(int)note velocity:(int)velocity;
- (void)noteOff:(int)channel note:(int)note;
- (void)programChange:(int)channel program:(int)program;
- (void)setGain:(float)gain;
- (void)cleanup;

@end

NS_ASSUME_NONNULL_END
```

**Location:** `iosApp/iosApp/FluidSynthWrapper.m`

```objc
#import "FluidSynthWrapper.h"
#import <FluidSynth/fluidsynth.h>

@interface FluidSynthWrapper()
@property (nonatomic, assign) fluid_settings_t *settings;
@property (nonatomic, assign) fluid_synth_t *synth;
@property (nonatomic, assign) fluid_audio_driver_t *audioDriver;
@end

@implementation FluidSynthWrapper

- (BOOL)initialize {
    self.settings = new_fluid_settings();
    if (!self.settings) return NO;
    
    fluid_settings_setstr(self.settings, "audio.driver", "sdl3");
    fluid_settings_setint(self.settings, "synth.polyphony", 256);
    
    self.synth = new_fluid_synth(self.settings);
    if (!self.synth) return NO;
    
    self.audioDriver = new_fluid_audio_driver(self.settings, self.synth);
    return self.audioDriver != NULL;
}

- (int)loadSoundFont:(NSString *)path {
    if (!self.synth) return -1;
    return fluid_synth_sfload(self.synth, [path UTF8String], 1);
}

- (void)noteOn:(int)channel note:(int)note velocity:(int)velocity {
    if (self.synth) {
        fluid_synth_noteon(self.synth, channel, note, velocity);
    }
}

- (void)noteOff:(int)channel note:(int)note {
    if (self.synth) {
        fluid_synth_noteoff(self.synth, channel, note);
    }
}

- (void)programChange:(int)channel program:(int)program {
    if (self.synth) {
        fluid_synth_program_change(self.synth, channel, program);
    }
}

- (void)setGain:(float)gain {
    if (self.synth) {
        fluid_synth_set_gain(self.synth, gain);
    }
}

- (void)cleanup {
    if (self.audioDriver) delete_fluid_audio_driver(self.audioDriver);
    if (self.synth) delete_fluid_synth(self.synth);
    if (self.settings) delete_fluid_settings(self.settings);
    
    self.audioDriver = NULL;
    self.synth = NULL;
    self.settings = NULL;
}

@end
```

Then use Kotlin/Native's Objective-C interop to call this wrapper.

**Pros/Cons:**
- **Approach 1 (Direct C):** More control, fewer layers, pure Kotlin
- **Approach 2 (ObjC Wrapper):** More iOS-native, easier debugging in Xcode, but extra layer

**Recommendation:** Use **Approach 1** for better Kotlin Multiplatform integration.

---

## PHASE 4: Xcode Project Configuration

### 4.1 Add Frameworks to Xcode (Manual Steps)

**Steps in Xcode:**

1. **Open** `iosApp/iosApp.xcodeproj` in Xcode
2. **Select** the iosApp target in the project navigator
3. **Go to** "General" tab → "Frameworks, Libraries, and Embedded Content"
4. **Click** the "+" button
5. **Choose** "Add Other..." → "Add Files..."
6. **Navigate** to your frameworks directory
7. **Select** both `FluidSynth.xcframework` and `SDL3.xcframework`
8. **Set** "Embed" to "Embed & Sign" (if dynamic) or "Do Not Embed" (if static)

### 4.2 Configure Framework Search Paths

**In Xcode Build Settings:**

1. **Search** for "Framework Search Paths"
2. **Add** the path to your frameworks:
   ```
   $(PROJECT_DIR)/../composeApp/frameworks
   ```
   Or if using absolute path:
   ```
   $(SRCROOT)/../composeApp/frameworks
   ```

3. **Add** for both Debug and Release configurations

### 4.3 Configure Header Search Paths

**In Build Settings:**
```
$(PROJECT_DIR)/../composeApp/frameworks/FluidSynth.xcframework/ios-arm64/FluidSynth.framework/Headers
$(PROJECT_DIR)/../composeApp/frameworks/SDL3.xcframework/ios-arm64/SDL3.framework/Headers
```

### 4.4 Update Info.plist (if needed)

Add required permissions if accessing file system for soundfonts:
```xml
<key>UIFileSharingEnabled</key>
<true/>
<key>LSSupportsOpeningDocumentsInPlace</key>
<true/>
```

---

## PHASE 5: Gradle Configuration (Complete)

### 5.1 Complete `build.gradle.kts` Updates

```kotlin
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
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            
            // Framework linking
            linkerOpts += "-F${projectDir}/frameworks"
            linkerOpts += "-framework FluidSynth"
            linkerOpts += "-framework SDL3"
            linkerOpts += "-framework CoreFoundation"
            linkerOpts += "-rpath @executable_path/Frameworks"
            
            // Export FluidSynth types to Swift/ObjC if needed
            export(project(":shared-fluidsynth")) // if you create a shared module
        }
        
        // C interop configuration
        iosTarget.compilations.getByName("main") {
            cinterops {
                val fluidsynth by creating {
                    defFile = project.file("src/iosMain/interop/fluidsynth.def")
                    packageName = "org.tetawex.cmpsftdemo.fluidsynth"
                    
                    // Include directories for both architectures
                    val frameworkPath = "${projectDir}/frameworks"
                    
                    when (iosTarget.name) {
                        "iosArm64" -> {
                            includeDirs.allHeaders(
                                "$frameworkPath/FluidSynth.xcframework/ios-arm64/FluidSynth.framework/Headers",
                                "$frameworkPath/SDL3.xcframework/ios-arm64/SDL3.framework/Headers"
                            )
                        }
                        "iosSimulatorArm64" -> {
                            includeDirs.allHeaders(
                                "$frameworkPath/FluidSynth.xcframework/ios-arm64_x86_64-simulator/FluidSynth.framework/Headers",
                                "$frameworkPath/SDL3.xcframework/ios-arm64_x86_64-simulator/SDL3.framework/Headers"
                            )
                        }
                    }
                }
            }
        }
    }
    
    sourceSets {
        // Existing sourceSets...
        
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // iOS-specific dependencies if needed
            }
        }
        
        iosArm64Main {
            dependsOn(iosMain)
        }
        
        iosSimulatorArm64Main {
            dependsOn(iosMain)
        }
    }
}
```

---

## PHASE 6: Resource Management

### 6.1 Bundle Soundfont File

**Option A: Add to iOS App Bundle (Recommended)**
1. Copy `.sf2` file to `iosApp/iosApp/Resources/`
2. Add to Xcode project (drag & drop into Xcode)
3. Ensure "Copy Bundle Resources" includes the file

**Option B: Download on First Launch**
- Implement download logic in `IOSSynthManager.initialize()`
- Store in app's Documents directory

### 6.2 Access Soundfont in Code

```kotlin
private fun getSoundfontPath(): String? {
    val bundle = NSBundle.mainBundle
    
    // Option 1: From bundle
    return bundle.pathForResource("GeneralUser_GS", ofType = "sf2")
    
    // Option 2: From documents directory
    // val docs = NSFileManager.defaultManager.URLsForDirectory(
    //     NSDocumentDirectory, NSUserDomainMask
    // ).firstOrNull() as? NSURL
    // return docs?.path?.plus("/soundfont.sf2")
}
```

---

## PHASE 7: Build & Test Strategy

### 7.1 Build Order

1. **Sync Gradle** - Let it download dependencies and configure cinterop
2. **Build Kotlin Framework**
   ```bash
   ./gradlew :composeApp:linkDebugFrameworkIosArm64
   ```
3. **Open Xcode Project**
4. **Clean Build Folder** (Cmd+Shift+K)
5. **Build iOS App** (Cmd+B)

### 7.2 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Framework not found | Check Framework Search Paths in Xcode |
| Undefined symbols | Verify linkerOpts in build.gradle.kts |
| cinterop generation fails | Check .def file paths and framework headers |
| Runtime crash on synth init | Ensure SDL_Init called, check SDL3 framework |
| No sound output | Verify audio permissions, check SDL3 audio driver |

### 7.3 Testing Checklist

- [ ] App builds without errors
- [ ] FluidSynth initializes successfully
- [ ] Soundfont loads without errors
- [ ] Notes play with correct pitch
- [ ] No memory leaks (use Instruments)
- [ ] Works on both device and simulator
- [ ] App doesn't crash on background/foreground

---

## PHASE 8: Architecture Decision Points

### 8.1 Framework Embedding Decision

**Static vs Dynamic:**
- FluidSynth.xcframework from official build is likely **dynamic**
- **Recommendation:** Use "Embed & Sign" in Xcode

### 8.2 C Interop vs ObjC Wrapper

| Approach | Best For |
|----------|----------|
| **C Interop** (Approach 1) | Pure Kotlin/Native projects, better KMP integration |
| **ObjC Wrapper** (Approach 2) | Teams with iOS experience, easier Xcode debugging |

**Recommendation:** Start with **Approach 1** (C Interop)

### 8.3 Soundfont Storage

| Option | Pros | Cons |
|--------|------|------|
| **Bundle** | Fast, always available | Increases app size |
| **On-Demand Download** | Smaller app | Network required, async complexity |
| **User Selection** | Flexible | UX complexity |

**Recommendation:** Start with **Bundle** for simplicity

---

## PHASE 9: Implementation Timeline

### Minimal Viable Implementation (2-3 hours)
1. Copy frameworks ✓
2. Create fluidsynth.def ✓
3. Update build.gradle.kts ✓
4. Implement FluidSynthInterop.kt ✓
5. Update SynthManager.ios.kt ✓
6. Configure Xcode frameworks ✓
7. Test basic functionality ✓

### Full Implementation (1-2 days)
- Add above +
- Resource management
- Error handling
- Background/foreground handling
- Memory optimization
- UI feedback
- Unit tests

---

## Quick Start Commands

```bash
# 1. Create interop directory
mkdir -p composeApp/src/iosMain/interop

# 2. Create frameworks directory
mkdir -p composeApp/frameworks

# 3. Copy your frameworks
cp -r /path/to/FluidSynth.xcframework composeApp/frameworks/
cp -r /path/to/SDL3.xcframework composeApp/frameworks/

# 4. Sync and build
./gradlew :composeApp:build

# 5. Open in Xcode
open iosApp/iosApp.xcodeproj
```

---

## Summary of Decisions

✅ **Recommended Approach:**
- Use **Approach 1** (Direct C Interop)
- Place frameworks in `composeApp/frameworks/`
- Bundle soundfont in iOS app
- Use dynamic framework embedding
- Configure both arm64 (device) and simulator architectures

This plan gives you a complete path from framework integration to working iOS app with FluidSynth support!

---

## Additional Resources

- FluidSynth C API Documentation: http://www.fluidsynth.org/api/
- Kotlin/Native C Interop: https://kotlinlang.org/docs/native-c-interop.html
- iOS CMake toolchain: https://github.com/leetal/ios-cmake
- SDL3 Documentation: https://wiki.libsdl.org/SDL3/
