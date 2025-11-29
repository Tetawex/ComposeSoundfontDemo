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
                    return@withContext false
                }

                // Try to load a default soundfont
                // This is a placeholder - in a real app you'd provide a soundfont file
                val soundFontPath = getSoundFontPath()
                if (soundFontPath != null) {
                    val sfId = FluidSynthJNI.loadSoundFont(synthHandle, soundFontPath)
                    if (sfId == -1) {
                        // Continue even if soundfont fails to load
                        android.util.Log.w(
                            "SynthManager",
                            "Failed to load soundfont, continuing anyway"
                        )
                    }
                }

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
            FluidSynthJNI.noteOn(synthHandle, currentChannel, note, velocity)
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
        // Look for a soundfont in app's files directory
        // This is a placeholder - you'd normally copy a soundfont during app setup
        val soundFontFile = File(context.filesDir, "default.sf2")
        return if (soundFontFile.exists()) {
            soundFontFile.absolutePath
        } else {
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
