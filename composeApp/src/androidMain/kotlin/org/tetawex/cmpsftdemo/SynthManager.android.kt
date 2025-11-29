package org.tetawex.cmpsftdemo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of SynthManager using FluidSynthJNI
 */
class AndroidSynthManager(private val context: Context) : SynthManager {
    private var synthHandle: Long = -1
    private var isInit = false
    private var currentChannel = 0

    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                // Create synth
                synthHandle = FluidSynthJNI.createSynth()
                if (synthHandle == -1L) {
                    android.util.Log.e("SynthManager", "Failed to create synthesizer")
                    return@withContext false
                }
                android.util.Log.i("SynthManager", "Synthesizer created with handle: $synthHandle")

                // Try to load a default soundfont
                val soundFontPath = getSoundFontPath()
                if (soundFontPath != null) {
                    android.util.Log.i("SynthManager", "Loading soundfont from: $soundFontPath")
                    val sfId = FluidSynthJNI.loadSoundFont(synthHandle, soundFontPath)
                    if (sfId == -1) {
                        android.util.Log.e("SynthManager", "Failed to load soundfont")
                    } else {
                        android.util.Log.i("SynthManager", "Soundfont loaded with ID: $sfId")
                    }
                } else {
                    android.util.Log.w("SynthManager", "No soundfont file found")
                }

                val sfCount = FluidSynthJNI.getSoundFontCount(synthHandle)
                android.util.Log.i("SynthManager", "Current soundfont count: $sfCount")
                
                // Set master gain to a reasonable level for audio output
                val gainResult = FluidSynthJNI.setMasterGain(synthHandle, 0.8)
                val currentGain = FluidSynthJNI.getMasterGain(synthHandle)
                android.util.Log.i("SynthManager", "Master gain set: result=$gainResult, current=$currentGain")

                isInit = true
                true
            } catch (e: Exception) {
                android.util.Log.e("SynthManager", "Failed to initialize synth", e)
                false
            }
        }
    }

    override fun playNote(note: Int, velocity: Int) {
        if (!isInit || synthHandle == -1L) return
        try {
            // Check if soundfonts are loaded
            val sfCount = FluidSynthJNI.getSoundFontCount(synthHandle)
            if (sfCount == 0) {
                android.util.Log.e("SynthManager", "Cannot play note: no soundfonts loaded")
                return
            }
            val result = FluidSynthJNI.noteOn(synthHandle, currentChannel, note, velocity)
            if (result != 0) {
                android.util.Log.e("SynthManager", "noteOn returned: $result")
            }
        } catch (e: Exception) {
            android.util.Log.e("SynthManager", "Error playing note", e)
        }
    }

    override fun stopNote(note: Int) {
        if (!isInit || synthHandle == -1L) return
        try {
            FluidSynthJNI.noteOff(synthHandle, currentChannel, note)
        } catch (e: Exception) {
            android.util.Log.e("SynthManager", "Error stopping note", e)
        }
    }

    override fun changeProgram(program: Int) {
        if (!isInit || synthHandle == -1L) return
        try {
            FluidSynthJNI.programChange(synthHandle, currentChannel, program)
        } catch (e: Exception) {
            android.util.Log.e("SynthManager", "Error changing program", e)
        }
    }

    override fun setVolume(volume: Int) {
        if (!isInit || synthHandle == -1L) return
        try {
            FluidSynthJNI.setChannelVolume(synthHandle, currentChannel, volume)
        } catch (e: Exception) {
            android.util.Log.e("SynthManager", "Error setting volume", e)
        }
    }

    override fun setBufferSize(bufferSize: Int) {
        // No-op on Android - buffer size is configured at native level
    }

    override fun isInitialized(): Boolean = isInit

    override fun cleanup() {
        if (synthHandle != -1L) {
            try {
                FluidSynthJNI.destroySynth(synthHandle)
            } catch (e: Exception) {
                android.util.Log.e("SynthManager", "Error destroying synth", e)
            }
            synthHandle = -1
            isInit = false
        }
    }

    private fun getSoundFontPath(): String? {
        // Look for the extracted sft_gu_gs.sf2 file
        val soundFontFile = File(context.filesDir, "sft_gu_gs.sf2")
        return if (soundFontFile.exists()) {
            val path = soundFontFile.absolutePath
            android.util.Log.i("SynthManager", "Found soundfont at: $path")
            path
        } else {
            android.util.Log.w("SynthManager", "Soundfont file not found at: ${soundFontFile.absolutePath}")
            null
        }
    }
}

private var _synthManager: AndroidSynthManager? = null

actual fun getSynthManager(): SynthManager {
    // This will be initialized by MainActivity
    return _synthManager ?: throw IllegalStateException(
        "SynthManager not initialized. Call initializeSynthManager() first."
    )
}

fun initializeSynthManager(context: Context) {
    _synthManager = AndroidSynthManager(context)
}
