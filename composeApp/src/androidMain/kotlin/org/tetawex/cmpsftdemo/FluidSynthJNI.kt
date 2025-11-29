package org.tetawex.cmpsftdemo

/**
 * JNI interface for FluidSynth synthesizer operations.
 * Provides native method bindings for FluidSynth C++ library.
 */
object FluidSynthJNI {
    
    /**
     * Create a new FluidSynth synthesizer instance.
     * @return Handle (ID) to the synthesizer, or -1 on failure
     */
    external fun createSynth(): Long
    
    /**
     * Destroy a FluidSynth synthesizer instance.
     * @param synthHandle The synthesizer handle returned from createSynth()
     */
    external fun destroySynth(synthHandle: Long)
    
    /**
     * Load a SoundFont file (.sf2).
     * @param synthHandle The synthesizer handle
     * @param filePath Path to the SoundFont file
     * @return SoundFont ID on success, -1 on failure
     */
    external fun loadSoundFont(synthHandle: Long, filePath: String): Int
    
    /**
     * Play a note (note on).
     * @param synthHandle The synthesizer handle
     * @param channel MIDI channel (0-15)
     * @param note MIDI note number (0-127)
     * @param velocity MIDI velocity (0-127, 0 = note off)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun noteOn(synthHandle: Long, channel: Int, note: Int, velocity: Int): Int
    
    /**
     * Stop a note (note off).
     * @param synthHandle The synthesizer handle
     * @param channel MIDI channel (0-15)
     * @param note MIDI note number (0-127)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun noteOff(synthHandle: Long, channel: Int, note: Int): Int
    
    /**
     * Change the program (instrument) for a channel.
     * @param synthHandle The synthesizer handle
     * @param channel MIDI channel (0-15)
     * @param program Program number (0-127)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun programChange(synthHandle: Long, channel: Int, program: Int): Int
    
    /**
     * Set the volume for a MIDI channel (CC 7).
     * @param synthHandle The synthesizer handle
     * @param channel MIDI channel (0-15)
     * @param volume Volume (0-127)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun setChannelVolume(synthHandle: Long, channel: Int, volume: Int): Int
    
    /**
     * Send a MIDI Control Change (CC) message.
     * @param synthHandle The synthesizer handle
     * @param channel MIDI channel (0-15)
     * @param controller CC number (0-127)
     * @param value CC value (0-127)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun controlChange(synthHandle: Long, channel: Int, controller: Int, value: Int): Int
    
    /**
     * Get the FluidSynth version string.
     * @return Version string
     */
    external fun getVersion(): String
    
    /**
     * Get the number of loaded SoundFonts.
     * @param synthHandle The synthesizer handle
     * @return Number of loaded SoundFonts
     */
    external fun getSoundFontCount(synthHandle: Long): Int
    
    /**
     * Set the master gain (volume) of the synthesizer.
     * @param synthHandle The synthesizer handle
     * @param gain Master gain (0.0-1.0 typically, but can be higher)
     * @return FLUID_OK (0) on success, FLUID_FAILED (-1) on failure
     */
    external fun setMasterGain(synthHandle: Long, gain: Double): Int
    
    /**
     * Get the current master gain (volume) of the synthesizer.
     * @param synthHandle The synthesizer handle
     * @return Master gain value
     */
    external fun getMasterGain(synthHandle: Long): Double
    

        init {
            try {
                System.loadLibrary("fluidsynth_wrapper")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native library 'fluidsynth_wrapper'", e)
            }
        }
}
