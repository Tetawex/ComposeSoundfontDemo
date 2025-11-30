package org.tetawex.cmpsftdemo

/**
 * Stub MIDI manager for JS target (not WASM).
 * TODO: Could be implemented using Web MIDI API similar to WASM.
 */
class StubMidiManager(
    private val synthManager: SynthManager
) : MidiManager {
    
    override fun isSupported(): Boolean = false
    
    override fun getState(): MidiState = MidiState.NOT_SUPPORTED
    
    override suspend fun initialize(): Boolean = false
    
    override fun getInputDevices(): List<MidiDeviceInfo> = emptyList()
    
    override fun getOutputDevices(): List<MidiDeviceInfo> = emptyList()
    
    override fun setInputEnabled(deviceId: String, enabled: Boolean) {
        // No-op
    }
    
    override fun isInputEnabled(deviceId: String): Boolean = false
    
    override fun getEnabledInputIds(): Set<String> = emptySet()
    
    override fun cleanup() {
        // No-op
    }
}

actual fun getMidiManager(synthManager: SynthManager): MidiManager = StubMidiManager(synthManager)
