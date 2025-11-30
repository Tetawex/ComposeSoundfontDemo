@file:OptIn(ExperimentalWasmJsInterop::class)

package org.tetawex.cmpsftdemo

import kotlin.js.Promise
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// External declarations for js-synthesizer library - loaded via script tag as global JSSynth
external class Synthesizer : JsAny {
    fun init(sampleRate: Double)
    fun loadSFont(buffer: JsAny): Promise<JsAny?>
    fun midiNoteOn(channel: Int, key: Int, velocity: Int)
    fun midiNoteOff(channel: Int, key: Int)
    fun midiProgramChange(channel: Int, program: Int)
    fun midiControl(channel: Int, control: Int, value: Int)
    fun setGain(gain: Double)
    fun getGain(): Double
    fun close()
    fun createAudioNode(audioContext: AudioContext, bufferSize: Int): AudioWorkletNode
}

// JSSynth namespace
external interface IJSSynth : JsAny {
    fun waitForReady(): Promise<JsAny?>
}

private val JSSynth: IJSSynth = js("window.JSSynth")

external class AudioWorkletNode : JsAny {
    fun connect(destination: JsAny): JsAny
    fun disconnect()
}

external class AudioContext : JsAny {
    val sampleRate: Double
    val destination: AudioDestinationNode
    fun resume(): Promise<JsAny?>
    fun createAnalyser(): AnalyserNode
}

external class AudioDestinationNode : JsAny

// Web Audio API AnalyserNode
external class AnalyserNode : JsAny {
    var fftSize: Int
    var smoothingTimeConstant: Double
    val frequencyBinCount: Int
    fun getByteFrequencyData(array: JsAny)
    fun getFloatFrequencyData(array: JsAny)
    fun connect(destination: JsAny): JsAny
}

// Top-level console declarations
external object console : JsAny {
    fun log(message: String)
    fun warn(message: String)
    fun error(message: String)
}

// Top-level audio context - created with low-latency settings for better performance
private val audioContext: AudioContext = js("new (window.AudioContext || window.webkitAudioContext)({ latencyHint: 'interactive', sampleRate: 48000 })")

/**
 * WASM implementation of SynthManager using FluidSynth Emscripten
 */
class WasmSynthManager : SynthManager {
    private var synthesizer: Synthesizer? = null
    private var audioNode: AudioWorkletNode? = null
    private var analyserNode: AnalyserNode? = null
    private var frequencyDataArray: JsAny? = null
    private var isInit = false
    private val channel = 0
    private var currentBufferSize: Int = getOptimalBufferSize()
    
    // Resume audio context if suspended (for browser autoplay policy)
    private fun resumeAudioContextIfNeeded() {
        try {
            val state = getAudioContextState(audioContext)
            if (state == "suspended") {
                console.log("WasmSynthManager: Resuming suspended audio context...")
                // Resume asynchronously - don't block note playback
                resumeAudioContextAsync(audioContext)
            }
        } catch (e: Throwable) {
            console.warn("WasmSynthManager: Failed to check/resume audio context: ${e.message}")
        }
    }
    
    override suspend fun initialize(): Boolean {
        return try {
            console.log("WasmSynthManager: Starting initialization")
            
            // Wait for JSSynth to be ready
            console.log("WasmSynthManager: Waiting for JSSynth to be ready...")
            promiseToSuspend(JSSynth.waitForReady())
            console.log("WasmSynthManager: JSSynth is ready")
            
            // Create synthesizer instance using new JSSynth.Synthesizer()
            console.log("WasmSynthManager: Creating synthesizer...")
            val synth = createSynthesizer()
            console.log("WasmSynthManager: Synthesizer created")
            
            // Initialize with audio context sample rate (synchronous)
            val sampleRate = audioContext.sampleRate
            console.log("WasmSynthManager: Initializing synth with sample rate: $sampleRate")
            synth.init(sampleRate)
            console.log("WasmSynthManager: Synth initialized")
            
            // Load soundfont from URL as ArrayBuffer
            console.log("WasmSynthManager: Fetching soundfont file...")
            val sfontBuffer = fetchArrayBuffer("sft_gu_gs.sf2")
            console.log("WasmSynthManager: Soundfont fetched, loading into synth...")
            val sfIdResult = promiseToSuspend(synth.loadSFont(sfontBuffer))
            val sfId = toInt(sfIdResult)
            console.log("WasmSynthManager: Soundfont loaded with ID: $sfId")
            
            if (sfId < 0) {
                console.error("WasmSynthManager: Failed to load soundfont")
                return false
            }
            
            // Set master gain
            synth.setGain(0.8)
            console.log("WasmSynthManager: Master gain set to ${synth.getGain()}")
            
            // Create audio node with optimized buffer size for low latency
            console.log("WasmSynthManager: Creating audio node with buffer size: $currentBufferSize")
            val node = try {
                synth.createAudioNode(audioContext, currentBufferSize)
            } catch (e: Throwable) {
                console.error("WasmSynthManager: Failed to create audio node: ${e.message}")
                throw e
            }
            console.log("WasmSynthManager: Audio node created successfully")
            
            // Create AnalyserNode for spectrum analysis
            console.log("WasmSynthManager: Creating AnalyserNode for spectrum analysis")
            val analyser = try {
                audioContext.createAnalyser().apply {
                    fftSize = 2048  // Results in 1024 frequency bins
                    smoothingTimeConstant = 0.8  // Smoothing for visual appeal
                }
            } catch (e: Throwable) {
                console.error("WasmSynthManager: Failed to create AnalyserNode: ${e.message}")
                throw e
            }
            console.log("WasmSynthManager: AnalyserNode created with fftSize=${analyser.fftSize}")
            
            // Connect: audioNode -> analyserNode -> destination
            console.log("WasmSynthManager: Connecting audio graph: synth -> analyser -> destination")
            try {
                node.connect(analyser)
                analyser.connect(audioContext.destination)
            } catch (e: Throwable) {
                console.error("WasmSynthManager: Failed to connect audio graph: ${e.message}")
                throw e
            }
            console.log("WasmSynthManager: Audio graph connected")
            
            // Create Uint8Array for frequency data
            frequencyDataArray = createUint8Array(analyser.frequencyBinCount)
            console.log("WasmSynthManager: Created frequency data array with ${analyser.frequencyBinCount} bins")
            
            synthesizer = synth
            audioNode = node
            analyserNode = analyser
            isInit = true
            
            console.log("WasmSynthManager: Initialization complete")
            true
        } catch (e: Throwable) {
            logError("WasmSynthManager: Initialization failed", e)
            false
        }
    }

    override fun playNote(note: Int, velocity: Int) {
        if (!isInit) {
            console.warn("WasmSynthManager: Cannot play note - not initialized")
            return
        }
        try {
            // Resume audio context if it's suspended (handles browser autoplay policy)
            resumeAudioContextIfNeeded()
            
            synthesizer?.midiNoteOn(channel, note, velocity)
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error playing note: ${e.message ?: "Unknown error"}")
        }
    }

    override fun stopNote(note: Int) {
        if (!isInit) return
        try {
            synthesizer?.midiNoteOff(channel, note)
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error stopping note: ${e.message ?: "Unknown error"}")
        }
    }

    override fun changeProgram(program: Int) {
        if (!isInit) return
        try {
            synthesizer?.midiProgramChange(channel, program)
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error changing program: ${e.message ?: "Unknown error"}")
        }
    }

    override fun setVolume(volume: Int) {
        if (!isInit) return
        try {
            // MIDI CC 7 = Channel Volume (0-127)
            val midiVolume = (volume.coerceIn(0, 100) * 127 / 100).coerceIn(0, 127)
            synthesizer?.midiControl(channel, 7, midiVolume)
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error setting volume: ${e.message ?: "Unknown error"}")
        }
    }

    override fun setBufferSize(bufferSize: Int) {
        if (!isInit) {
            currentBufferSize = bufferSize
            console.log("WasmSynthManager: Buffer size set to $bufferSize (will apply on initialization)")
            return
        }
        
        try {
            val synth = synthesizer ?: return
            console.log("WasmSynthManager: Changing buffer size from $currentBufferSize to $bufferSize")
            
            // Disconnect old audio node
            audioNode?.disconnect()
            
            // Create new audio node with new buffer size
            val node = synth.createAudioNode(audioContext, bufferSize)
            
            // Reconnect through analyser if available
            val analyser = analyserNode
            if (analyser != null) {
                node.connect(analyser)
                // Analyser is already connected to destination
            } else {
                node.connect(audioContext.destination)
            }
            
            audioNode = node
            currentBufferSize = bufferSize
            console.log("WasmSynthManager: Buffer size changed successfully")
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error changing buffer size: ${e.message ?: "Unknown error"}")
        }
    }

    override fun isInitialized(): Boolean = isInit
    
    override fun isSpectrumAnalysisAvailable(): Boolean = analyserNode != null
    
    override fun getSpectrumData(): SpectrumData? {
        val analyser = analyserNode ?: return null
        val dataArray = frequencyDataArray ?: return null
        
        try {
            // Get frequency data from analyser (values 0-255)
            analyser.getByteFrequencyData(dataArray)
            
            // Convert Uint8Array to FloatArray with normalization (0.0 to 1.0)
            val binCount = analyser.frequencyBinCount
            val magnitudes = FloatArray(binCount) { i ->
                getUint8ArrayValue(dataArray, i) / 255f
            }
            
            return SpectrumData(
                magnitudes = magnitudes,
                sampleRate = audioContext.sampleRate.toInt(),
                fftSize = analyser.fftSize
            )
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error getting spectrum data: ${e.message ?: "Unknown error"}")
            return null
        }
    }

    override fun cleanup() {
        try {
            audioNode?.disconnect()
            analyserNode = null
            frequencyDataArray = null
            synthesizer?.close()
            synthesizer = null
            audioNode = null
            isInit = false
        } catch (e: Throwable) {
            console.error("WasmSynthManager: Error during cleanup: ${e.message ?: "Unknown error"}")
        }
    }
}

// Top-level helper functions using js()
private fun createSynthesizer(): Synthesizer = 
    js("new JSSynth.Synthesizer()")

// Check if running on mobile device
private fun isMobileDevice(): Boolean =
    js("(/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent))")

// Get optimal buffer size based on device capabilities
// Smaller buffer = lower latency but requires more CPU
private fun getOptimalBufferSize(): Int {
    return 512
    // Mobile devices: use 512 for stability
    // Desktop: use 256 for lower latency
    // return if (isMobileDevice()) 512 else 256
}

// Resume audio context asynchronously (fire and forget)
private fun resumeAudioContextAsync(ctx: AudioContext): Unit =
    js("ctx.resume().catch(function(err) { console.error('Failed to resume audio context:', err); })")

// Fetch a file and return a Promise<Response>
private fun fetchUrl(url: String): Promise<JsAny> =
    js("fetch(url)")

// Get ArrayBuffer from Response
private fun responseToArrayBuffer(response: JsAny): Promise<JsAny> =
    js("response.arrayBuffer()")

// Fetch a file as ArrayBuffer (for soundfont loading)
private suspend fun fetchArrayBuffer(url: String): JsAny = suspendCoroutine { cont ->
    val fetchPromise = fetchUrl(url)
    promiseThen(
        fetchPromise,
        onSuccess = { response ->
            val arrayBufferPromise = responseToArrayBuffer(response)
            promiseThen(
                arrayBufferPromise,
                onSuccess = { buffer -> cont.resume(buffer) },
                onError = { error ->
                    val errorMsg = getErrorMessage(error)
                    console.error("Failed to get ArrayBuffer: $errorMsg")
                    cont.resumeWithException(RuntimeException("Failed to get ArrayBuffer: $errorMsg"))
                }
            )
        },
        onError = { error ->
            val errorMsg = getErrorMessage(error)
            console.error("Failed to fetch file: $errorMsg")
            cont.resumeWithException(RuntimeException("Failed to fetch file: $errorMsg"))
        }
    )
}

// Helper to convert JsAny? to Int
private fun toInt(value: JsAny?): Int = 
    js("Number(value)")

// Wrapper function that creates the promise then/catch handlers at top level
private fun <T : JsAny?> promiseThen(
    promise: Promise<T>,
    onSuccess: (T) -> Unit,
    onError: (JsAny?) -> Unit
): Unit = js("""
    promise.then(
        function(value) { onSuccess(value); },
        function(error) { onError(error); }
    )
""")

// Suspend function wrapper - doesn't use js() directly in suspend context
private suspend fun promiseToSuspend(promise: Promise<JsAny?>): JsAny? = suspendCoroutine { cont ->
    promiseThen(
        promise,
        onSuccess = { value -> 
            cont.resume(value) 
        },
        onError = { error -> 
            val errorMsg = getErrorMessage(error)
            val errorStack = getErrorStack(error)
            console.error("Promise rejected: $errorMsg")
            if (errorStack.isNotEmpty()) {
                console.error("Error stack: $errorStack")
            }
            cont.resumeWithException(RuntimeException("Promise rejected: $errorMsg"))
        }
    )
}

// Helper to extract error message from JS error object
private fun getErrorMessage(error: JsAny?): String = 
    js("error ? (error.message || error.toString()) : 'Unknown error'")

// Helper to extract error stack from JS error object
private fun getErrorStack(error: JsAny?): String = 
    js("error && error.stack ? error.stack : ''")

// Helper to get audio context state
private fun getAudioContextState(audioContext: AudioContext): String =
    js("audioContext.state")

// Helper to create a Uint8Array for frequency data
private fun createUint8Array(size: Int): JsAny =
    js("new Uint8Array(size)")

// Helper to get value from Uint8Array at index
private fun getUint8ArrayValue(array: JsAny, index: Int): Float =
    js("array[index]")

// Helper to log error details
private fun logError(message: String, error: Throwable) {
    console.error("$message: ${error.message ?: "Unknown error"}")
    error.stackTraceToString().let { stack ->
        if (stack.isNotEmpty()) {
            console.error("Kotlin stack trace: $stack")
        }
    }
}

actual fun getSynthManager(): SynthManager = WasmSynthManager()
