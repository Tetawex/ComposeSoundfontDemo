package org.tetawex.cmpsftdemo

/**
 * Platform-independent interface for synth operations
 */
interface SynthManager {
    /**
     * Initialize the synth and load soundfont
     */
    suspend fun initialize(): Boolean
    
    /**
     * Play a note
     * @param note MIDI note number (0-127)
     * @param velocity MIDI velocity (0-127)
     */
    fun playNote(note: Int, velocity: Int = 100)
    
    /**
     * Stop a note
     * @param note MIDI note number (0-127)
     */
    fun stopNote(note: Int)
    
    /**
     * Change the instrument/program
     * @param program Program number (0-127)
     */
    fun changeProgram(program: Int)
    
    /**
     * Set the volume
     * @param volume Volume level (0-127)
     */
    fun setVolume(volume: Int)
    
    /**
     * Check if synth is initialized
     */
    fun isInitialized(): Boolean
    
    /**
     * Cleanup resources
     */
    fun cleanup()
}

/**
 * Get the platform-specific synth manager implementation
 */
expect fun getSynthManager(): SynthManager
