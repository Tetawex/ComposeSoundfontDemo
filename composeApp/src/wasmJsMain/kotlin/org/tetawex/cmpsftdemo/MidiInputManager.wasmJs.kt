@file:OptIn(ExperimentalWasmJsInterop::class)

package org.tetawex.cmpsftdemo

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// ============================================================================
// Web MIDI API External Declarations
// ============================================================================

/**
 * Navigator interface with requestMIDIAccess
 */
external interface Navigator : JsAny {
    fun requestMIDIAccess(options: MIDIOptions?): Promise<MIDIAccess>
}

/**
 * Options for requestMIDIAccess
 */
external interface MIDIOptions : JsAny {
    val sysex: Boolean
    val software: Boolean
}

/**
 * MIDIAccess - provides access to MIDI devices
 */
external interface MIDIAccess : JsAny {
    val inputs: MIDIInputMap
    val outputs: MIDIOutputMap
    val sysexEnabled: Boolean
}

/**
 * Map of MIDI inputs
 */
external interface MIDIInputMap : JsAny

/**
 * Map of MIDI outputs  
 */
external interface MIDIOutputMap : JsAny

/**
 * Individual MIDI input port
 */
external interface MIDIInput : JsAny {
    val id: String
    val name: String?
    val manufacturer: String?
    val state: String  // "connected" | "disconnected"
    val type: String   // "input"
    val version: String?
}

/**
 * Individual MIDI output port
 */
external interface MIDIOutput : JsAny {
    val id: String
    val name: String?
    val manufacturer: String?
    val state: String
    val type: String   // "output"
    val version: String?
    fun send(data: JsAny, timestamp: Double)
}

/**
 * MIDI message event
 */
external interface MIDIMessageEvent : JsAny {
    val data: JsAny  // Uint8Array
    val timeStamp: Double
}

/**
 * MIDI connection event (device connected/disconnected)
 */
external interface MIDIConnectionEvent : JsAny {
    val port: MIDIPort
}

/**
 * Generic MIDI port
 */
external interface MIDIPort : JsAny {
    val id: String
    val name: String?
    val manufacturer: String?
    val state: String
    val type: String
    val connection: String  // "open" | "closed" | "pending"
    fun open(): Promise<MIDIPort>
    fun close(): Promise<MIDIPort>
}

/**
 * Permission state
 */
external interface PermissionStatus : JsAny {
    val state: String  // "granted" | "denied" | "prompt"
}

// ============================================================================
// JS Helper Functions
// ============================================================================

// Get navigator object
private fun getNavigator(): Navigator = js("navigator")

// Check if Web MIDI API is supported
private fun isMidiSupported(): Boolean = 
    js("'requestMIDIAccess' in navigator")

// Create MIDI options object
private fun createMidiOptions(sysex: Boolean, software: Boolean): MIDIOptions =
    js("({ sysex: sysex, software: software })")

// Request MIDI access with options
private fun requestMidiAccess(options: MIDIOptions): Promise<MIDIAccess> =
    js("navigator.requestMIDIAccess(options)")

// Request MIDI access without options (basic)
private fun requestMidiAccessBasic(): Promise<MIDIAccess> =
    js("navigator.requestMIDIAccess()")

// Iterate over MIDIInputMap entries
private fun forEachMidiInput(inputs: MIDIInputMap, callback: (MIDIInput) -> Unit): Unit =
    js("inputs.forEach(function(input) { callback(input); })")

// Iterate over MIDIOutputMap entries
private fun forEachMidiOutput(outputs: MIDIOutputMap, callback: (MIDIOutput) -> Unit): Unit =
    js("outputs.forEach(function(output) { callback(output); })")

// Set onmidimessage handler for input
private fun setMidiMessageHandler(input: MIDIInput, handler: (MIDIMessageEvent) -> Unit): Unit =
    js("input.onmidimessage = function(event) { handler(event); }")

// Clear onmidimessage handler
private fun clearMidiMessageHandler(input: MIDIInput): Unit =
    js("input.onmidimessage = null")

// Set onstatechange handler for MIDIAccess
private fun setStateChangeHandler(access: MIDIAccess, handler: (MIDIConnectionEvent) -> Unit): Unit =
    js("access.onstatechange = function(event) { handler(event); }")

// Get MIDI message data as array of ints
private fun getMidiDataByte(data: JsAny, index: Int): Int =
    js("data[index]")

private fun getMidiDataLength(data: JsAny): Int =
    js("data.length")

// Query permission state
private fun queryMidiPermission(sysex: Boolean): Promise<JsAny?> =
    js("navigator.permissions ? navigator.permissions.query({ name: 'midi', sysex: sysex }) : Promise.resolve(null)")

private fun getPermissionState(status: JsAny?): String =
    js("status ? status.state : 'unknown'")

// ============================================================================
// MIDI Data Classes
// ============================================================================

/**
 * Represents a connected MIDI device
 */
data class MidiDevice(
    val id: String,
    val name: String,
    val manufacturer: String,
    val isInput: Boolean,
    val isConnected: Boolean
)

/**
 * Parsed MIDI message types
 */
sealed class MidiMessage {
    abstract val channel: Int
    abstract val timestamp: Double
    
    data class NoteOn(
        override val channel: Int,
        val note: Int,
        val velocity: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class NoteOff(
        override val channel: Int,
        val note: Int,
        val velocity: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class ControlChange(
        override val channel: Int,
        val controller: Int,
        val value: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class ProgramChange(
        override val channel: Int,
        val program: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class PitchBend(
        override val channel: Int,
        val value: Int,  // 0-16383, center at 8192
        override val timestamp: Double
    ) : MidiMessage()
    
    data class ChannelPressure(
        override val channel: Int,
        val pressure: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class PolyPressure(
        override val channel: Int,
        val note: Int,
        val pressure: Int,
        override val timestamp: Double
    ) : MidiMessage()
    
    data class SystemExclusive(
        val data: ByteArray,
        override val timestamp: Double
    ) : MidiMessage() {
        override val channel: Int = 0
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as SystemExclusive
            if (!data.contentEquals(other.data)) return false
            if (timestamp != other.timestamp) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }
    
    data class Unknown(
        val status: Int,
        val data: ByteArray,
        override val timestamp: Double
    ) : MidiMessage() {
        override val channel: Int = 0
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Unknown
            if (status != other.status) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = status
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}

/**
 * MIDI permission state
 */
enum class MidiPermissionState {
    GRANTED,
    DENIED,
    PROMPT,
    UNKNOWN,
    NOT_SUPPORTED
}

/**
 * Result of MIDI initialization
 */
sealed class MidiInitResult {
    data class Success(val devices: List<MidiDevice>) : MidiInitResult()
    data class PermissionDenied(val message: String) : MidiInitResult()
    data class NotSupported(val message: String) : MidiInitResult()
    data class Error(val message: String) : MidiInitResult()
}

// ============================================================================
// MIDI Input Manager
// ============================================================================

/**
 * Manages Web MIDI API connections and forwards events to FluidSynth.
 * Implements the common MidiManager interface.
 */
class WasmMidiManager(
    private val synthManager: SynthManager
) : MidiManager {
    private var midiAccess: MIDIAccess? = null
    private var currentState: MidiState = MidiState.NOT_INITIALIZED
    private var sysexEnabled = false
    
    // Track which input devices are enabled for listening
    private val enabledInputIds = mutableSetOf<String>()
    
    // Callbacks for UI/logging
    var onDeviceConnected: ((MidiDeviceInfo) -> Unit)? = null
    var onDeviceDisconnected: ((MidiDeviceInfo) -> Unit)? = null
    var onMidiMessage: ((MidiMessage) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    override fun isSupported(): Boolean = isMidiSupported()
    
    override fun getState(): MidiState = currentState
    
    override suspend fun initialize(): Boolean {
        if (!isSupported()) {
            currentState = MidiState.NOT_SUPPORTED
            return false
        }
        
        if (currentState == MidiState.READY) {
            console.warn("WasmMidiManager: Already initialized")
            return true
        }
        
        currentState = MidiState.INITIALIZING
        
        return try {
            console.log("WasmMidiManager: Requesting MIDI access")
            
            val options = createMidiOptions(sysex = false, software = true)
            val access = promiseToSuspend(requestMidiAccess(options)) as MIDIAccess
            
            midiAccess = access
            sysexEnabled = access.sysexEnabled
            currentState = MidiState.READY
            
            console.log("WasmMidiManager: MIDI access granted (sysex enabled: $sysexEnabled)")
            
            // Set up device connection/disconnection listener
            setupStateChangeListener(access)
            
            // Log discovered devices
            val devices = getAllDevices()
            console.log("WasmMidiManager: Found ${devices.size} MIDI device(s)")
            devices.forEach { device ->
                console.log("  - ${device.name} (${device.manufacturer}) [${if (device.isInput) "input" else "output"}]")
            }
            
            // Auto-enable all input devices by default
            getInputDevices().forEach { device ->
                setInputEnabled(device.id, true)
            }
            
            true
            
        } catch (e: Throwable) {
            val message = e.message ?: "Unknown error"
            console.error("WasmMidiManager: Failed to initialize: $message")
            
            // Check if it's a permission error
            currentState = if (message.contains("permission", ignoreCase = true) || 
                message.contains("denied", ignoreCase = true) ||
                message.contains("SecurityError", ignoreCase = true)) {
                MidiState.PERMISSION_DENIED
            } else {
                MidiState.ERROR
            }
            
            false
        }
    }
    
    override fun getInputDevices(): List<MidiDeviceInfo> {
        val access = midiAccess ?: return emptyList()
        val devices = mutableListOf<MidiDeviceInfo>()
        
        forEachMidiInput(access.inputs) { input ->
            devices.add(MidiDeviceInfo(
                id = input.id,
                name = input.name ?: "Unknown Device",
                manufacturer = input.manufacturer ?: "Unknown",
                isInput = true,
                isConnected = input.state == "connected"
            ))
        }
        
        return devices
    }
    
    override fun getOutputDevices(): List<MidiDeviceInfo> {
        val access = midiAccess ?: return emptyList()
        val devices = mutableListOf<MidiDeviceInfo>()
        
        forEachMidiOutput(access.outputs) { output ->
            devices.add(MidiDeviceInfo(
                id = output.id,
                name = output.name ?: "Unknown Device",
                manufacturer = output.manufacturer ?: "Unknown",
                isInput = false,
                isConnected = output.state == "connected"
            ))
        }
        
        return devices
    }
    
    private fun getAllDevices(): List<MidiDeviceInfo> = getInputDevices() + getOutputDevices()
    
    override fun setInputEnabled(deviceId: String, enabled: Boolean) {
        val access = midiAccess ?: return
        
        if (enabled) {
            if (enabledInputIds.add(deviceId)) {
                // Find and set up handler for this device
                forEachMidiInput(access.inputs) { input ->
                    if (input.id == deviceId) {
                        setMidiMessageHandler(input) { event -> handleMidiMessage(input.id, event) }
                        console.log("WasmMidiManager: Enabled input: ${input.name ?: input.id}")
                    }
                }
            }
        } else {
            if (enabledInputIds.remove(deviceId)) {
                // Find and clear handler for this device
                forEachMidiInput(access.inputs) { input ->
                    if (input.id == deviceId) {
                        clearMidiMessageHandler(input)
                        console.log("WasmMidiManager: Disabled input: ${input.name ?: input.id}")
                    }
                }
            }
        }
    }
    
    override fun isInputEnabled(deviceId: String): Boolean = deviceId in enabledInputIds
    
    override fun getEnabledInputIds(): Set<String> = enabledInputIds.toSet()
    
    override fun cleanup() {
        console.log("WasmMidiManager: Cleaning up")
        
        midiAccess?.let { access ->
            forEachMidiInput(access.inputs) { input ->
                clearMidiMessageHandler(input)
            }
        }
        
        enabledInputIds.clear()
        midiAccess = null
        currentState = MidiState.NOT_INITIALIZED
        sysexEnabled = false
    }
    
    /**
     * Check if sysex is enabled
     */
    fun isSysexEnabled(): Boolean = sysexEnabled
    
    // ========================================================================
    // Private Implementation
    // ========================================================================
    
    private fun setupStateChangeListener(access: MIDIAccess) {
        setStateChangeHandler(access) { event ->
            val port = event.port
            val device = MidiDeviceInfo(
                id = port.id,
                name = port.name ?: "Unknown Device",
                manufacturer = port.manufacturer ?: "Unknown",
                isInput = port.type == "input",
                isConnected = port.state == "connected"
            )
            
            if (port.state == "connected") {
                console.log("WasmMidiManager: Device connected: ${device.name}")
                
                // If it's an input that was previously enabled, re-enable handler
                if (port.type == "input" && port.id in enabledInputIds) {
                    forEachMidiInput(access.inputs) { input ->
                        if (input.id == port.id) {
                            setMidiMessageHandler(input) { ev -> handleMidiMessage(input.id, ev) }
                        }
                    }
                }
                
                onDeviceConnected?.invoke(device)
            } else {
                console.log("WasmMidiManager: Device disconnected: ${device.name}")
                onDeviceDisconnected?.invoke(device)
            }
        }
    }
    
    private fun handleMidiMessage(deviceId: String, event: MIDIMessageEvent) {
        // Only process if this device is enabled
        if (deviceId !in enabledInputIds) return
        
        try {
            val data = event.data
            val length = getMidiDataLength(data)
            
            if (length == 0) return
            
            val status = getMidiDataByte(data, 0)
            val timestamp = event.timeStamp
            
            // Parse MIDI message
            val message = parseMidiMessage(status, data, length, timestamp)
            
            // Notify callback
            onMidiMessage?.invoke(message)
            
            // Forward to FluidSynth
            forwardToSynth(message)
            
        } catch (e: Throwable) {
            console.error("WasmMidiManager: Error handling MIDI message: ${e.message}")
            onError?.invoke("Error processing MIDI: ${e.message}")
        }
    }
    private fun parseMidiMessage(status: Int, data: JsAny, length: Int, timestamp: Double): MidiMessage {
        val command = status and 0xF0
        val channel = status and 0x0F
        
        return when (command) {
            0x90 -> { // Note On
                val note = if (length > 1) getMidiDataByte(data, 1) else 0
                val velocity = if (length > 2) getMidiDataByte(data, 2) else 0
                
                // Note On with velocity 0 is actually Note Off
                if (velocity == 0) {
                    MidiMessage.NoteOff(channel, note, 0, timestamp)
                } else {
                    MidiMessage.NoteOn(channel, note, velocity, timestamp)
                }
            }
            
            0x80 -> { // Note Off
                val note = if (length > 1) getMidiDataByte(data, 1) else 0
                val velocity = if (length > 2) getMidiDataByte(data, 2) else 0
                MidiMessage.NoteOff(channel, note, velocity, timestamp)
            }
            
            0xB0 -> { // Control Change
                val controller = if (length > 1) getMidiDataByte(data, 1) else 0
                val value = if (length > 2) getMidiDataByte(data, 2) else 0
                MidiMessage.ControlChange(channel, controller, value, timestamp)
            }
            
            0xC0 -> { // Program Change
                val program = if (length > 1) getMidiDataByte(data, 1) else 0
                MidiMessage.ProgramChange(channel, program, timestamp)
            }
            
            0xE0 -> { // Pitch Bend
                val lsb = if (length > 1) getMidiDataByte(data, 1) else 0
                val msb = if (length > 2) getMidiDataByte(data, 2) else 0
                val value = (msb shl 7) or lsb  // 14-bit value, 0-16383, center at 8192
                MidiMessage.PitchBend(channel, value, timestamp)
            }
            
            0xD0 -> { // Channel Pressure (Aftertouch)
                val pressure = if (length > 1) getMidiDataByte(data, 1) else 0
                MidiMessage.ChannelPressure(channel, pressure, timestamp)
            }
            
            0xA0 -> { // Polyphonic Key Pressure
                val note = if (length > 1) getMidiDataByte(data, 1) else 0
                val pressure = if (length > 2) getMidiDataByte(data, 2) else 0
                MidiMessage.PolyPressure(channel, note, pressure, timestamp)
            }
            
            0xF0 -> { // System messages
                if (status == 0xF0) {
                    // System Exclusive
                    val bytes = ByteArray(length) { i -> getMidiDataByte(data, i).toByte() }
                    MidiMessage.SystemExclusive(bytes, timestamp)
                } else {
                    // Other system messages (clock, etc.)
                    val bytes = ByteArray(length) { i -> getMidiDataByte(data, i).toByte() }
                    MidiMessage.Unknown(status, bytes, timestamp)
                }
            }
            
            else -> {
                val bytes = ByteArray(length) { i -> getMidiDataByte(data, i).toByte() }
                MidiMessage.Unknown(status, bytes, timestamp)
            }
        }
    }
    
    private fun forwardToSynth(message: MidiMessage) {
        if (!synthManager.isInitialized()) {
            return
        }
        
        when (message) {
            is MidiMessage.NoteOn -> {
                // FluidSynth's midiNoteOn takes channel, key, velocity
                synthManager.playNoteOnChannel(message.channel, message.note, message.velocity)
            }
            
            is MidiMessage.NoteOff -> {
                synthManager.stopNoteOnChannel(message.channel, message.note)
            }
            
            is MidiMessage.ControlChange -> {
                synthManager.sendControlChange(message.channel, message.controller, message.value)
            }
            
            is MidiMessage.ProgramChange -> {
                synthManager.changeProgramOnChannel(message.channel, message.program)
            }
            
            is MidiMessage.PitchBend -> {
                synthManager.sendPitchBend(message.channel, message.value)
            }
            
            is MidiMessage.ChannelPressure -> {
                synthManager.sendChannelPressure(message.channel, message.pressure)
            }
            
            is MidiMessage.PolyPressure -> {
                synthManager.sendKeyPressure(message.channel, message.note, message.pressure)
            }
            
            is MidiMessage.SystemExclusive -> {
                // SysEx would require special handling - log for now
                console.log("MidiInputManager: SysEx message received (${message.data.size} bytes)")
            }
            
            is MidiMessage.Unknown -> {
                // Unknown messages are logged but not forwarded
            }
        }
    }
    
    // Nullable promise suspend helper
    private suspend fun promiseToSuspendNullable(promise: Promise<JsAny?>): JsAny? = suspendCoroutine { cont ->
        promiseThenMidi(
            promise,
            onSuccess = { value -> cont.resume(value) },
            onError = { error -> 
                cont.resumeWithException(RuntimeException(getMidiErrorMessage(error)))
            }
        )
    }
}

// Promise helper for MIDI
private fun <T : JsAny?> promiseThenMidi(
    promise: Promise<T>,
    onSuccess: (T) -> Unit,
    onError: (JsAny?) -> Unit
): Unit = js("""
    promise.then(
        function(value) { onSuccess(value); },
        function(error) { onError(error); }
    )
""")

private fun getMidiErrorMessage(error: JsAny?): String = 
    js("error ? (error.message || error.toString()) : 'Unknown error'")

// ============================================================================
// SynthManager Extensions for MIDI forwarding
// ============================================================================

/**
 * Extension to play note on specific channel
 */
fun SynthManager.playNoteOnChannel(channel: Int, note: Int, velocity: Int) {
    if (this is WasmSynthManager) {
        this.playNoteOnChannel(channel, note, velocity)
    }
}

/**
 * Extension to stop note on specific channel
 */
fun SynthManager.stopNoteOnChannel(channel: Int, note: Int) {
    if (this is WasmSynthManager) {
        this.stopNoteOnChannel(channel, note)
    }
}

/**
 * Extension to send control change
 */
fun SynthManager.sendControlChange(channel: Int, controller: Int, value: Int) {
    if (this is WasmSynthManager) {
        this.sendControlChange(channel, controller, value)
    }
}

/**
 * Extension to change program on specific channel
 */
fun SynthManager.changeProgramOnChannel(channel: Int, program: Int) {
    if (this is WasmSynthManager) {
        this.changeProgramOnChannel(channel, program)
    }
}

/**
 * Extension to send pitch bend
 */
fun SynthManager.sendPitchBend(channel: Int, value: Int) {
    if (this is WasmSynthManager) {
        this.sendPitchBend(channel, value)
    }
}

/**
 * Extension to send channel pressure
 */
fun SynthManager.sendChannelPressure(channel: Int, pressure: Int) {
    if (this is WasmSynthManager) {
        this.sendChannelPressure(channel, pressure)
    }
}

/**
 * Extension to send polyphonic key pressure
 */
fun SynthManager.sendKeyPressure(channel: Int, key: Int, pressure: Int) {
    if (this is WasmSynthManager) {
        this.sendKeyPressure(channel, key, pressure)
    }
}

// ============================================================================
// Factory function for common interface
// ============================================================================

actual fun getMidiManager(synthManager: SynthManager): MidiManager = WasmMidiManager(synthManager)
