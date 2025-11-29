package org.tetawex.cmpsftdemo

import kotlinx.cinterop.*
import org.tetawex.cmpsftdemo.fluidsynth.*
import org.tetawex.cmpsftdemo.sdl3.*
import platform.Foundation.NSLog

/**
 * Kotlin wrapper around FluidSynth C API for iOS
 */
@OptIn(ExperimentalForeignApi::class)
class FluidSynthNative {
    private var settings: CPointer<fluid_settings_t>? = null
    private var synth: CPointer<fluid_synth_t>? = null
    private var audioDriver: CPointer<fluid_audio_driver_t>? = null
    private var sdlInitialized = false
    private var currentBufferSize: Int = 512  // Default buffer size

    fun initialize(): Boolean {
        // Tell SDL we're handling main() ourselves
        SDL_SetMainReady()
        
        // Initialize SDL3 audio subsystem first
        if (!SDL_Init(SDL_INIT_AUDIO)) {
            NSLog("Failed to initialize SDL3 audio: ${SDL_GetError()?.toKString()}")
            return false
        }
        sdlInitialized = true
        NSLog("SDL3 audio initialized successfully")

        // Create settings
        settings = new_fluid_settings()
        if (settings == null) {
            NSLog("Failed to create FluidSynth settings")
            return false
        }

        // Configure settings for iOS/SDL3
        fluid_settings_setstr(settings, "audio.driver", "sdl3")
        fluid_settings_setint(settings, "synth.polyphony", 256)
        fluid_settings_setint(settings, "synth.midi-channels", 16)
        fluid_settings_setnum(settings, "synth.gain", 0.8)
        
        // Set audio buffer size (period-size controls latency)
        fluid_settings_setint(settings, "audio.period-size", currentBufferSize)

        // Create synth
        synth = new_fluid_synth(settings)
        if (synth == null) {
            NSLog("Failed to create FluidSynth synth")
            cleanup()
            return false
        }

        // Create audio driver
        audioDriver = new_fluid_audio_driver(settings, synth)
        if (audioDriver == null) {
            NSLog("Failed to create FluidSynth audio driver")
            cleanup()
            return false
        }

        NSLog("FluidSynth initialized successfully")
        return true
    }

    fun loadSoundFont(path: String): Int {
        val s = synth
        if (s == null) {
            NSLog("Cannot load soundfont: synth not initialized")
            return -1
        }
        val result = fluid_synth_sfload(s, path, 1)
        if (result == -1) {
            NSLog("Failed to load soundfont: $path")
        } else {
            NSLog("Loaded soundfont: $path (id=$result)")
        }
        return result
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

    fun setBufferSize(bufferSize: Int) {
        currentBufferSize = bufferSize
        // Note: Buffer size change requires reinitializing the audio driver
        // This is applied on next initialize() call
        NSLog("FluidSynth: Buffer size set to $bufferSize (will apply on next initialization)")
    }

    fun cleanup() {
        audioDriver?.let { delete_fluid_audio_driver(it) }
        synth?.let { delete_fluid_synth(it) }
        settings?.let { delete_fluid_settings(it) }

        audioDriver = null
        synth = null
        settings = null
        
        // Quit SDL3 audio subsystem
        if (sdlInitialized) {
            SDL_QuitSubSystem(SDL_INIT_AUDIO)
            sdlInitialized = false
        }
        
        NSLog("FluidSynth cleaned up")
    }
}
