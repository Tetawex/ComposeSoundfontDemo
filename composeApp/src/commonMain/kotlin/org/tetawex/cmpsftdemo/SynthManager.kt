package org.tetawex.cmpsftdemo

/**
 * Data class for audio spectrum analysis
 * Contains frequency bin magnitudes for spectrogram visualization
 */
data class SpectrumData(
    /**
     * Frequency magnitude data (0.0 to 1.0 normalized)
     * Array length equals FFT size / 2 (number of frequency bins)
     */
    val magnitudes: FloatArray,
    
    /**
     * Sample rate used for this spectrum analysis
     */
    val sampleRate: Int = 44100,
    
    /**
     * FFT size used for this analysis
     */
    val fftSize: Int = 2048
) {
    /**
     * Get the frequency (Hz) for a given bin index
     */
    fun frequencyForBin(binIndex: Int): Float {
        return binIndex.toFloat() * sampleRate / fftSize
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SpectrumData
        if (!magnitudes.contentEquals(other.magnitudes)) return false
        if (sampleRate != other.sampleRate) return false
        if (fftSize != other.fftSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = magnitudes.contentHashCode()
        result = 31 * result + sampleRate.hashCode()
        result = 31 * result + fftSize
        return result
    }
    
    companion object {
        val EMPTY = SpectrumData(FloatArray(0))
    }
}

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
     * Set the audio buffer size (WASM only, no-op on other platforms)
     * @param bufferSize Buffer size in samples (e.g., 128, 256, 512, 1024)
     * Lower values = lower latency but higher CPU usage
     */
    fun setBufferSize(bufferSize: Int)
    
    /**
     * Check if synth is initialized
     */
    fun isInitialized(): Boolean
    
    /**
     * Check if spectrum analysis is available on this platform
     */
    fun isSpectrumAnalysisAvailable(): Boolean = false
    
    /**
     * Get current audio spectrum data for visualization
     * Returns null if spectrum analysis is not available or not initialized
     */
    fun getSpectrumData(): SpectrumData? = null
    
    /**
     * Cleanup resources
     */
    fun cleanup()
}

/**
 * Get the platform-specific synth manager implementation
 */
expect fun getSynthManager(): SynthManager
