This is Compose Multiplatform + Fluidsynth demo app targeting Android, iOS, Web, Desktop (JVM). Mostly vibe-coded.

## Platform Support

✅ **Fully Implemented:**
- **Android** - Native C++ FluidSynth wrapper via JNI
- **Web (WASM)** - Emscripten FluidSynth build with js-synthesizer wrapper.
- **iOS / Mac Catalyst** - FluidSynth integration (native). Works on iOS devices and Mac Catalyst builds, audio latency is not great.

🚧 **Stub Implementations (Not Yet Functional):**
- **Desktop (JVM)** - Requires FluidSynth Java bindings  
- **Web (JS)** - Requires FluidSynth Emscripten integration

## Features:
- 🚧 Basic Compose Multiplatform UI to control note, channel, etc. Buggy
- 🚧 Buffer size, sampling rate, envelop - not implemented or very basic
- 🚧 Latency issues on iOS. WASM and Android are more or less fine

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

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.
