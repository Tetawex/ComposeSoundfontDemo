package org.tetawex.cmpsftdemo

import android.util.Log

/**
 * Wrapper class for FluidSynth synthesizer operations.
 * Provides a Kotlin-friendly interface to the native FluidSynth library.
 */
class FluidSynthWrapper {
    private var synthHandle: Long = -1
    private var isInitialized: Boolean = false
    
    companion object {
        private const val TAG = "FluidSynthWrapper"
    }
    
    /**
     * Initialize a new FluidSynth synthesizer.
     * @return true if initialization succeeded, false otherwise
     */
    fun initialize(): Boolean {
        return try {
            synthHandle = FluidSynthJNI.createSynth()
            isInitialized = synthHandle >= 0
            if (isInitialized) {
                Log.i(TAG, "FluidSynth initialized successfully: $synthHandle")
                Log.i(TAG, "Version: ${FluidSynthJNI.getVersion()}")
            } else {
                Log.e(TAG, "Failed to create FluidSynth synthesizer")
            }
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Exception during initialization", e)
            false
        }
    }
    
    /**
     * Load a SoundFont file.
     * @param filePath Path to the .sf2 file
     * @return SoundFont ID on success, -1 on failure
     */
    fun loadSoundFont(filePath: String): Int {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return -1
        }
        
        return try {
            val sfontId = FluidSynthJNI.loadSoundFont(synthHandle, filePath)
            if (sfontId >= 0) {
                Log.i(TAG, "Loaded SoundFont: $filePath (ID: $sfontId)")
            } else {
                Log.e(TAG, "Failed to load SoundFont: $filePath")
            }
            sfontId
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading SoundFont", e)
            -1
        }
    }
    
    /**
     * Play a note.
     * @param channel MIDI channel (0-15)
     * @param note MIDI note (0-127)
     * @param velocity Velocity (0-127, where 0 = note off)
     * @return true if successful
     */
    fun playNote(channel: Int, note: Int, velocity: Int): Boolean {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return false
        }
        
        return try {
            val result = FluidSynthJNI.noteOn(synthHandle, channel, note, velocity)
            result == 0 // FLUID_OK
        } catch (e: Exception) {
            Log.e(TAG, "Exception playing note", e)
            false
        }
    }
    
    /**
     * Stop a note.
     * @param channel MIDI channel (0-15)
     * @param note MIDI note (0-127)
     * @return true if successful
     */
    fun stopNote(channel: Int, note: Int): Boolean {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return false
        }
        
        return try {
            val result = FluidSynthJNI.noteOff(synthHandle, channel, note)
            result == 0 // FLUID_OK
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping note", e)
            false
        }
    }
    
    /**
     * Change the instrument (program) on a channel.
     * @param channel MIDI channel (0-15)
     * @param program Program number (0-127)
     * @return true if successful
     */
    fun changeProgram(channel: Int, program: Int): Boolean {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return false
        }
        
        return try {
            val result = FluidSynthJNI.programChange(synthHandle, channel, program)
            result == 0 // FLUID_OK
        } catch (e: Exception) {
            Log.e(TAG, "Exception changing program", e)
            false
        }
    }
    
    /**
     * Set the volume for a channel.
     * @param channel MIDI channel (0-15)
     * @param volume Volume (0-127)
     * @return true if successful
     */
    fun setVolume(channel: Int, volume: Int): Boolean {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return false
        }
        
        return try {
            val result = FluidSynthJNI.setChannelVolume(synthHandle, channel, volume)
            result == 0 // FLUID_OK
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting volume", e)
            false
        }
    }
    
    /**
     * Send a MIDI CC (Control Change) message.
     * @param channel MIDI channel (0-15)
     * @param controller CC number (0-127)
     * @param value CC value (0-127)
     * @return true if successful
     */
    fun sendControlChange(channel: Int, controller: Int, value: Int): Boolean {
        if (!isInitialized || synthHandle < 0) {
            Log.e(TAG, "Synthesizer not initialized")
            return false
        }
        
        return try {
            val result = FluidSynthJNI.controlChange(synthHandle, channel, controller, value)
            result == 0 // FLUID_OK
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending CC", e)
            false
        }
    }
    
    /**
     * Get the synthesizer handle.
     * @return The native synthesizer handle
     */
    fun getSynthHandle(): Long = synthHandle
    
    /**
     * Check if synthesizer is initialized.
     * @return true if initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Clean up and destroy the synthesizer.
     */
    fun release() {
        if (isInitialized && synthHandle >= 0) {
            try {
                FluidSynthJNI.destroySynth(synthHandle)
                Log.i(TAG, "Synthesizer released")
            } catch (e: Exception) {
                Log.e(TAG, "Exception releasing synthesizer", e)
            } finally {
                isInitialized = false
                synthHandle = -1
            }
        }
    }
}
