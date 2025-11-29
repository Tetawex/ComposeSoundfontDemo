This is Compose Multiplatform + Fluidsynth demo app targeting Android, iOS, Web, Desktop (JVM). Mostly vibe-coded.

## Platform Support

✅ **Fully Implemented:**
- **Android** - Native C++ FluidSynth wrapper via JNI
- **Web (WASM)** - Emscripten FluidSynth build with js-synthesizer wrapper

🚧 **Stub Implementations (Not Yet Functional):**
- **iOS** - Resources prepared, requires FluidSynth integration
- **Desktop (JVM)** - Requires FluidSynth Java bindings  
- **Web (JS)** - Requires FluidSynth Emscripten integration

## WASM Implementation

The WASM target now has full FluidSynth support implemented using:
- `libfluidsynth-2.4.6-with-libsndfile.js` - FluidSynth Emscripten build with libsndfile support
- `sft_gu_gs.sf2` - SoundFont file (copied from Android resources)
- Top-level `js()` declarations for JavaScript interop
- External class declarations implementing the FluidSynth Synthesizer API
- Promise-based async initialization using Kotlin coroutines

Key implementation features:
- Uses `@OptIn(ExperimentalWasmJsInterop::class)` for Kotlin/Wasm JS interop
- Web Audio API integration via AudioWorkletNode
- Proper suspend function handling without inline `js()` calls
- Console logging for debugging initialization and playback

To run:
```bash
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

Then open http://localhost:8081/ in your browser.

Default Compose readme below
* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).