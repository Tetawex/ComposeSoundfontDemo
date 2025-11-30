package org.tetawex.cmpsftdemo

/**
 * Represents a connected MIDI device (common across platforms)
 */
data class MidiDeviceInfo(
    val id: String,
    val name: String,
    val manufacturer: String,
    val isInput: Boolean,
    val isConnected: Boolean
)

/**
 * MIDI permission/initialization state
 */
enum class MidiState {
    NOT_INITIALIZED,
    INITIALIZING,
    READY,
    PERMISSION_DENIED,
    NOT_SUPPORTED,
    ERROR
}

/**
 * Platform-independent interface for MIDI input management
 */
interface MidiManager {
    /**
     * Check if MIDI input is supported on this platform
     */
    fun isSupported(): Boolean
    
    /**
     * Get current MIDI state
     */
    fun getState(): MidiState
    
    /**
     * Initialize MIDI access (may trigger permission prompt on Web)
     * @return true if initialization was successful
     */
    suspend fun initialize(): Boolean
    
    /**
     * Get list of available MIDI input devices
     */
    fun getInputDevices(): List<MidiDeviceInfo>
    
    /**
     * Get list of available MIDI output devices
     */
    fun getOutputDevices(): List<MidiDeviceInfo>
    
    /**
     * Enable/disable listening to a specific input device
     * @param deviceId The device ID to enable/disable
     * @param enabled Whether to enable listening
     */
    fun setInputEnabled(deviceId: String, enabled: Boolean)
    
    /**
     * Check if a specific input device is enabled
     */
    fun isInputEnabled(deviceId: String): Boolean
    
    /**
     * Get set of enabled input device IDs
     */
    fun getEnabledInputIds(): Set<String>
    
    /**
     * Cleanup and release MIDI resources
     */
    fun cleanup()
}

/**
 * Get the platform-specific MIDI manager implementation
 */
expect fun getMidiManager(synthManager: SynthManager): MidiManager
