package org.tetawex.cmpsftdemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSLog

/**
 * iOS SynthManager implementation using FluidSynth
 */
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
                } else {
                    NSLog("Soundfont not found in bundle")
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

    override fun setBufferSize(bufferSize: Int) {
        fluidSynth.setBufferSize(bufferSize)
    }

    override fun isInitialized(): Boolean = initialized

    override fun cleanup() {
        fluidSynth.cleanup()
        initialized = false
    }

    private fun getSoundfontPath(): String? {
        val bundle = NSBundle.mainBundle
        // Try different soundfont names commonly used
        return bundle.pathForResource("default", ofType = "sf2")
            ?: bundle.pathForResource("GeneralUser_GS", ofType = "sf2")
            ?: bundle.pathForResource("soundfont", ofType = "sf2")
    }
}

actual fun getSynthManager(): SynthManager = IOSSynthManager()

