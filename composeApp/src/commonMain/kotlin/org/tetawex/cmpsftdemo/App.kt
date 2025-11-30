package org.tetawex.cmpsftdemo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.absoluteValue

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5B7AA4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5F7FA),
    onPrimaryContainer = Color(0xFF1A2A3A),
    secondary = Color(0xFF7A8FA4),
    onSecondary = Color.White,
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8ECF0),
    onSurfaceVariant = Color(0xFF44474A),
    outline = Color(0xFFB0B8C4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

// Buffer size presets
private val bufferSizePresets = listOf(128, 256, 512, 1024, 2048, 4096)

// Piano key data (relative to octave, will be shifted by octave selector)
private data class PianoKey(
    val noteOffset: Int, // Offset from C of the octave (0-11 for first octave, 12-23 for second)
    val isBlack: Boolean,
    val keyboardChar: String, // The keyboard key mapped to this note
    val noteName: String // Base note name without octave number
)

// Define piano keys for 2 octaves with QWERTY mapping (offsets from base octave)
// Layout: Black keys are positioned above white keys
// QWERTY row 1: 2 3   5 6 7   9 0   = (black keys)
// QWERTY row 2: Q W E R T Y U I O P [ ] (white keys)
private val pianoKeyOffsets = listOf(
    // First octave (C-B)
    PianoKey(0, false, "Q", "C"),
    PianoKey(1, true, "2", "C#"),
    PianoKey(2, false, "W", "D"),
    PianoKey(3, true, "3", "D#"),
    PianoKey(4, false, "E", "E"),
    PianoKey(5, false, "R", "F"),
    PianoKey(6, true, "5", "F#"),
    PianoKey(7, false, "T", "G"),
    PianoKey(8, true, "6", "G#"),
    PianoKey(9, false, "Y", "A"),
    PianoKey(10, true, "7", "A#"),
    PianoKey(11, false, "U", "B"),
    // Second octave (C-G)
    PianoKey(12, false, "I", "C"),
    PianoKey(13, true, "9", "C#"),
    PianoKey(14, false, "O", "D"),
    PianoKey(15, true, "0", "D#"),
    PianoKey(16, false, "P", "E"),
    PianoKey(17, false, "[", "F"),
    PianoKey(18, true, "=", "F#"),
    PianoKey(19, false, "]", "G"),
)

// Create key mappings (offsets from base octave)
private val keyToNoteOffset: Map<Key, Int> = mapOf(
    // White keys (lower row on QWERTY)
    Key.Q to 0, Key.W to 2, Key.E to 4, Key.R to 5,
    Key.T to 7, Key.Y to 9, Key.U to 11, Key.I to 12,
    Key.O to 14, Key.P to 16, Key.LeftBracket to 17, Key.RightBracket to 19,
    // Black keys (upper row on QWERTY)
    Key.Two to 1, Key.Three to 3, Key.Five to 6, Key.Six to 8,
    Key.Seven to 10, Key.Nine to 13, Key.Zero to 15, Key.Equals to 18,
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = LightColorScheme) {
        val synthManager = remember { getSynthManager() }
        val midiManager = remember { getMidiManager(synthManager) }
        
        var synthInitialized by remember { mutableStateOf(false) }
        var midiState by remember { mutableStateOf(MidiState.NOT_INITIALIZED) }
        var midiDevices by remember { mutableStateOf<List<MidiDeviceInfo>>(emptyList()) }
        var enabledMidiInputs by remember { mutableStateOf<Set<String>>(emptySet()) }
        
        var volume by remember { mutableStateOf(100f) }
        var program by remember { mutableStateOf(0f) }
        
        // Octave selector (MIDI octave 0-8, default to 4 which is middle C)
        var baseOctave by remember { mutableStateOf(4) }
        
        // Buffer size selector
        var selectedBufferSizeIndex by remember { mutableStateOf(1) } // Default to 256
        
        // Spectrogram visibility toggle
        var showSpectrogram by remember { mutableStateOf(true) }
        
        // MIDI devices panel visibility
        var showMidiPanel by remember { mutableStateOf(true) }

        // Track pressed notes (by MIDI note number)
        val pressedNotes = remember { mutableStateMapOf<Int, Boolean>() }
        
        // Keyboard focus
        val focusRequester = remember { FocusRequester() }
        val pressedKeys = remember { mutableStateMapOf<Key, Boolean>() }
        
        // Calculate base MIDI note from octave (C of that octave)
        // MIDI note 60 = C4, so C0 = 12, C1 = 24, etc.
        val baseMidiNote = (baseOctave + 1) * 12
        
        // Coroutine scope for MIDI initialization triggered by user action
        val coroutineScope = rememberCoroutineScope()

        // Initialize synth on first composition
        LaunchedEffect(Unit) {
            synthInitialized = synthManager.initialize()
        }
        
        // Check MIDI support (but don't request permission yet - needs user gesture)
        LaunchedEffect(synthInitialized) {
            if (synthInitialized) {
                if (!midiManager.isSupported()) {
                    midiState = MidiState.NOT_SUPPORTED
                }
                // Don't auto-initialize - Web MIDI API requires user gesture
            }
        }
        
        // Function to initialize MIDI (called from user click)
        val initializeMidi: () -> Unit = {
            if (midiManager.isSupported() && midiState != MidiState.READY && midiState != MidiState.INITIALIZING) {
                midiState = MidiState.INITIALIZING
                coroutineScope.launch {
                    val success = midiManager.initialize()
                    midiState = midiManager.getState()
                    if (success) {
                        midiDevices = midiManager.getInputDevices()
                    }
                }
            }
        }
        
        // Refresh MIDI devices periodically (for hot-plug detection)
        LaunchedEffect(midiState) {
            if (midiState == MidiState.READY) {
                while (true) {
                    delay(1000)
                    midiDevices = midiManager.getInputDevices()
                }
            }
        }

        // Cleanup on disposal
        DisposableEffect(Unit) {
            onDispose {
                midiManager.cleanup()
                synthManager.cleanup()
            }
        }

        // Request focus on launch
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (!synthInitialized) return@onKeyEvent false
                    
                    val noteOffset = keyToNoteOffset[keyEvent.key] ?: return@onKeyEvent false
                    val note = baseMidiNote + noteOffset
                    
                    when (keyEvent.type) {
                        KeyEventType.KeyDown -> {
                            if (pressedKeys[keyEvent.key] != true) {
                                pressedKeys[keyEvent.key] = true
                                pressedNotes[note] = true
                                synthManager.playNote(note, 100)
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            pressedKeys[keyEvent.key] = false
                            pressedNotes[note] = false
                            synthManager.stopNote(note)
                            true
                        }
                        else -> false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App Title
            Text(
                "Tech Demo: Compose Multiplatform + Fluidsynth",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            if (synthInitialized) {
                Text(
                    "Ready • Click piano keys or use keyboard (Q-] for white, 2-= for black)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // MIDI Devices Section
                MidiDevicesPanel(
                    midiManager = midiManager,
                    midiState = midiState,
                    midiDevices = midiDevices,
                    enabledInputIds = enabledMidiInputs,
                    onToggleInput = { deviceId, enabled ->
                        midiManager.setInputEnabled(deviceId, enabled)
                        enabledMidiInputs = midiManager.getEnabledInputIds()
                    },
                    showPanel = showMidiPanel,
                    onTogglePanel = { showMidiPanel = it },
                    onInitializeMidi = initializeMidi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Refresh devices when MIDI becomes ready
                LaunchedEffect(midiState) {
                    if (midiState == MidiState.READY) {
                        midiDevices = midiManager.getInputDevices()
                        enabledMidiInputs = midiManager.getEnabledInputIds()
                    }
                }

                // Piano Keyboard (max 80% of viewport height)
                PianoKeyboard(
                    baseMidiNote = baseMidiNote,
                    pressedNotes = pressedNotes,
                    onNotePressed = { note ->
                        pressedNotes[note] = true
                        synthManager.playNote(note, 100)
                    },
                    onNoteReleased = { note ->
                        pressedNotes[note] = false
                        synthManager.stopNote(note)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .fillMaxHeight(0.5f) // Use up to 50% of height, will be capped
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row 1: Volume & Instrument
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Volume: ${volume.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                synthManager.setVolume(it.toInt())
                            },
                            valueRange = 0f..127f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Instrument Control
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Instrument: ${program.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = program,
                            onValueChange = {
                                program = it
                                synthManager.changeProgram(it.toInt())
                            },
                            valueRange = 0f..127f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Controls Row 2: Octave & Buffer Size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Octave Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Octave: $baseOctave (C${baseOctave})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = baseOctave.toFloat(),
                            onValueChange = { baseOctave = it.toInt() },
                            valueRange = 0f..7f,
                            steps = 6, // 7 discrete values: 0-7
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buffer Size Selector
                    Column(modifier = Modifier.weight(1f)) {
                        val bufferSize = bufferSizePresets[selectedBufferSizeIndex]
                        Text(
                            "Buffer: $bufferSize samples",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = selectedBufferSizeIndex.toFloat(),
                            onValueChange = { 
                                selectedBufferSizeIndex = it.toInt()
                                synthManager.setBufferSize(bufferSizePresets[it.toInt()])
                            },
                            valueRange = 0f..(bufferSizePresets.size - 1).toFloat(),
                            steps = bufferSizePresets.size - 2, // Discrete steps
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Spectrogram visualization (only shown if available and enabled)
                if (synthManager.isSpectrumAnalysisAvailable()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Spectrum Analyzer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = showSpectrogram,
                            onCheckedChange = { showSpectrogram = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    
                    if (showSpectrogram) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Spectrogram(
                            synthManager = synthManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                Text(
                    "Synth not available on this platform",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun PianoKeyboard(
    baseMidiNote: Int,
    pressedNotes: Map<Int, Boolean>,
    onNotePressed: (Int) -> Unit,
    onNoteReleased: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Track which note is currently being touched and if we're in a drag
    var currentTouchedNote by remember { mutableStateOf<Int?>(null) }
    var isPointerDown by remember { mutableStateOf(false) }
    
    // Separate white and black keys
    val whiteKeys = pianoKeyOffsets.filter { !it.isBlack }
    val blackKeys = pianoKeyOffsets.filter { it.isBlack }
    
    // Colors
    val whiteKeyColor = Color.White
    val whiteKeyPressedColor = Color(0xFFE3E8ED)
    val blackKeyColor = Color(0xFF2C2C2C)
    val blackKeyPressedColor = Color(0xFF4A4A4A)
    val outlineColor = Color(0xFFB0B8C4)
    val textColor = Color(0xFF666666)
    val blackKeyTextColor = Color(0xFFCCCCCC)
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(baseMidiNote) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position ?: continue
                            
                            when (event.type) {
                                PointerEventType.Press -> {
                                    isPointerDown = true
                                    val totalWidth = size.width.toFloat()
                                    val totalHeight = size.height.toFloat()
                                    val whiteKeyWidth = totalWidth / whiteKeys.size
                                    val blackKeyWidth = whiteKeyWidth * 0.6f
                                    val blackKeyHeight = totalHeight * 0.6f
                                    
                                    // Check black keys first (they're on top)
                                    var hitNoteOffset: Int? = null
                                    
                                    for (key in blackKeys) {
                                        // Find the white key index before this black key
                                        val whiteKeyIndex = whiteKeys.indexOfFirst { it.noteOffset > key.noteOffset } - 1
                                        if (whiteKeyIndex >= 0) {
                                            val blackKeyX = (whiteKeyIndex + 1) * whiteKeyWidth - blackKeyWidth / 2
                                            
                                            if (position.x >= blackKeyX && 
                                                position.x <= blackKeyX + blackKeyWidth &&
                                                position.y <= blackKeyHeight) {
                                                hitNoteOffset = key.noteOffset
                                                break
                                            }
                                        }
                                    }
                                    
                                    // If no black key hit, check white keys
                                    if (hitNoteOffset == null) {
                                        val whiteKeyIndex = (position.x / whiteKeyWidth).toInt()
                                            .coerceIn(0, whiteKeys.size - 1)
                                        hitNoteOffset = whiteKeys[whiteKeyIndex].noteOffset
                                    }
                                    
                                    // Play the note immediately on press
                                    val hitNote = baseMidiNote + hitNoteOffset
                                    currentTouchedNote = hitNote
                                    onNotePressed(hitNote)
                                    
                                    event.changes.forEach { it.consume() }
                                }
                                PointerEventType.Move -> {
                                    // Only handle move if pointer is down (dragging)
                                    if (isPointerDown) {
                                        val totalWidth = size.width.toFloat()
                                        val totalHeight = size.height.toFloat()
                                        val whiteKeyWidth = totalWidth / whiteKeys.size
                                        val blackKeyWidth = whiteKeyWidth * 0.6f
                                        val blackKeyHeight = totalHeight * 0.6f
                                        
                                        // Check black keys first (they're on top)
                                        var hitNoteOffset: Int? = null
                                        
                                        for (key in blackKeys) {
                                            val whiteKeyIndex = whiteKeys.indexOfFirst { it.noteOffset > key.noteOffset } - 1
                                            if (whiteKeyIndex >= 0) {
                                                val blackKeyX = (whiteKeyIndex + 1) * whiteKeyWidth - blackKeyWidth / 2
                                                
                                                if (position.x >= blackKeyX && 
                                                    position.x <= blackKeyX + blackKeyWidth &&
                                                    position.y <= blackKeyHeight) {
                                                    hitNoteOffset = key.noteOffset
                                                    break
                                                }
                                            }
                                        }
                                        
                                        if (hitNoteOffset == null) {
                                            val whiteKeyIndex = (position.x / whiteKeyWidth).toInt()
                                                .coerceIn(0, whiteKeys.size - 1)
                                            hitNoteOffset = whiteKeys[whiteKeyIndex].noteOffset
                                        }
                                        
                                        val hitNote = baseMidiNote + hitNoteOffset
                                        
                                        // Only change if we moved to a different note
                                        if (hitNote != currentTouchedNote) {
                                            currentTouchedNote?.let { onNoteReleased(it) }
                                            onNotePressed(hitNote)
                                            currentTouchedNote = hitNote
                                        }
                                        
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                                PointerEventType.Release -> {
                                    isPointerDown = false
                                    currentTouchedNote?.let { onNoteReleased(it) }
                                    currentTouchedNote = null
                                    event.changes.forEach { it.consume() }
                                }
                                else -> {}
                            }
                        }
                    }
                }
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            val whiteKeyWidth = totalWidth / whiteKeys.size
            val blackKeyWidth = whiteKeyWidth * 0.6f
            val blackKeyHeight = totalHeight * 0.6f
            val cornerRadius = 4.dp.toPx()
            
            // Draw white keys
            whiteKeys.forEachIndexed { index, key ->
                val x = index * whiteKeyWidth
                val midiNote = baseMidiNote + key.noteOffset
                val isPressed = pressedNotes[midiNote] == true
                
                // Key fill
                drawRoundRect(
                    color = if (isPressed) whiteKeyPressedColor else whiteKeyColor,
                    topLeft = Offset(x + 1, 0f),
                    size = Size(whiteKeyWidth - 2, totalHeight - 1),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
                
                // Key outline
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(x + 1, 0f),
                    size = Size(whiteKeyWidth - 2, totalHeight - 1),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 1.5f)
                )
                
                // Draw keyboard hint at bottom
                val textLayoutResult = textMeasurer.measure(
                    text = key.keyboardChar,
                    style = TextStyle(fontSize = 12.sp, color = textColor)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x + (whiteKeyWidth - textLayoutResult.size.width) / 2,
                        totalHeight - textLayoutResult.size.height - 8.dp.toPx()
                    )
                )
            }
            
            // Draw black keys
            blackKeys.forEach { key ->
                // Find the white key index before this black key
                val whiteKeyIndex = whiteKeys.indexOfFirst { it.noteOffset > key.noteOffset } - 1
                if (whiteKeyIndex >= 0) {
                    val x = (whiteKeyIndex + 1) * whiteKeyWidth - blackKeyWidth / 2
                    val midiNote = baseMidiNote + key.noteOffset
                    val isPressed = pressedNotes[midiNote] == true
                    
                    // Key fill
                    drawRoundRect(
                        color = if (isPressed) blackKeyPressedColor else blackKeyColor,
                        topLeft = Offset(x, 0f),
                        size = Size(blackKeyWidth, blackKeyHeight),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Key outline
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(x, 0f),
                        size = Size(blackKeyWidth, blackKeyHeight),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = 1f)
                    )
                    
                    // Draw keyboard hint
                    val textLayoutResult = textMeasurer.measure(
                        text = key.keyboardChar,
                        style = TextStyle(fontSize = 10.sp, color = blackKeyTextColor)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x + (blackKeyWidth - textLayoutResult.size.width) / 2,
                            blackKeyHeight - textLayoutResult.size.height - 6.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

/**
 * Spectrogram component that displays real-time frequency spectrum from the synth
 */
@Composable
private fun Spectrogram(
    synthManager: SynthManager,
    modifier: Modifier = Modifier
) {
    val numBars = 64
    
    // Display magnitudes as state - changes trigger recomposition
    val displayMagnitudes = remember { mutableStateListOf<Float>().apply { addAll(List(numBars) { 0f }) } }
    
    // Poll spectrum data at 120fps
    LaunchedEffect(synthManager) {
        while (true) {
            if (synthManager.isInitialized() && synthManager.isSpectrumAnalysisAvailable()) {
                val data = synthManager.getSpectrumData()
                if (data != null && data.magnitudes.isNotEmpty()) {
                    val magnitudes = data.magnitudes
                    val totalBins = magnitudes.size
                    
                    for (i in 0 until numBars) {
                        val startBin = ((i.toFloat() / numBars).let { it * it } * totalBins * 0.5f).toInt()
                        val endBin = (((i + 1).toFloat() / numBars).let { it * it } * totalBins * 0.5f).toInt()
                            .coerceAtLeast(startBin + 1)
                            .coerceAtMost(totalBins)
                        
                        var sum = 0f
                        for (bin in startBin until endBin) {
                            sum += magnitudes[bin]
                        }
                        displayMagnitudes[i] = if (endBin > startBin) (sum / (endBin - startBin) * 1.5f).coerceIn(0f, 1f) else 0f
                    }
                }
            }
            delay(8) // ~120fps
        }
    }
    
    // Colors for spectrum bars - gradient from blue (low) to red (high frequency)
    val barColors = remember {
        listOf(
            Color(0xFF2196F3), // Blue - low frequencies
            Color(0xFF00BCD4), // Cyan
            Color(0xFF4CAF50), // Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFF9800), // Orange
            Color(0xFFF44336)  // Red - high frequencies
        )
    }
    
    Box(
        modifier = modifier
            .background(Color(0xFF1A1C1E), shape = MaterialTheme.shapes.small)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // Calculate bar dimensions
            val barWidth = size.width / numBars
            val barSpacing = 2f
            val effectiveBarWidth = barWidth - barSpacing
            
            for (i in 0 until numBars) {
                val magnitude = displayMagnitudes[i]
                
                // Calculate bar height
                val barHeight = magnitude * size.height
                
                // Calculate color based on bar position (frequency)
                val colorIndex = (i.toFloat() / numBars * (barColors.size - 1)).toInt()
                    .coerceIn(0, barColors.size - 1)
                val color = barColors[colorIndex]
                
                // Draw the bar
                if (barHeight > 0.5f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * barWidth + barSpacing / 2, size.height - barHeight),
                        size = Size(effectiveBarWidth, barHeight),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

/**
 * MIDI devices panel showing connection status and device list
 */
@Composable
private fun MidiDevicesPanel(
    midiManager: MidiManager,
    midiState: MidiState,
    midiDevices: List<MidiDeviceInfo>,
    enabledInputIds: Set<String>,
    onToggleInput: (deviceId: String, enabled: Boolean) -> Unit,
    showPanel: Boolean,
    onTogglePanel: (Boolean) -> Unit,
    onInitializeMidi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header row with toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onTogglePanel(!showPanel) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator
                MidiStatusIndicator(midiState)
                
                Text(
                    text = "MIDI Devices",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Device count badge
                if (midiState == MidiState.READY) {
                    val inputCount = midiDevices.count { it.isInput && it.isConnected }
                    Text(
                        text = "$inputCount input${if (inputCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Text(
                text = if (showPanel) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Expandable content
        if (showPanel) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                when (midiState) {
                    MidiState.NOT_SUPPORTED -> {
                        Column {
                            Text(
                                "MIDI Not Supported",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Web MIDI API is not available in this browser.\nTry Chrome, Edge, or Firefox (v108+). Safari is not supported.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    MidiState.PERMISSION_DENIED -> {
                        Column {
                            Text(
                                "MIDI Permission Denied",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Please allow MIDI access in your browser settings, then click Retry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onInitializeMidi,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    
                    MidiState.NOT_INITIALIZED -> {
                        Column {
                            Text(
                                "Click to enable MIDI input from connected devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onInitializeMidi,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Connect MIDI Devices")
                            }
                        }
                    }
                    
                    MidiState.INITIALIZING -> {
                        Text(
                            "Requesting MIDI access...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    MidiState.ERROR -> {
                        Column {
                            Text(
                                "MIDI Error",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onInitializeMidi,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    
                    MidiState.READY -> {
                        val inputDevices = midiDevices.filter { it.isInput }
                        
                        if (inputDevices.isEmpty()) {
                            Text(
                                "No MIDI input devices found.\nConnect a MIDI controller and it will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                inputDevices.forEach { device ->
                                    MidiDeviceRow(
                                        device = device,
                                        isEnabled = device.id in enabledInputIds,
                                        onToggle = { enabled ->
                                            onToggleInput(device.id, enabled)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status indicator circle for MIDI state
 */
@Composable
private fun MidiStatusIndicator(state: MidiState) {
    val color = when (state) {
        MidiState.READY -> Color(0xFF4CAF50) // Green
        MidiState.INITIALIZING -> Color(0xFFFFC107) // Yellow/Amber
        MidiState.NOT_SUPPORTED, MidiState.PERMISSION_DENIED, MidiState.ERROR -> Color(0xFFF44336) // Red
        MidiState.NOT_INITIALIZED -> Color(0xFF9E9E9E) // Gray
    }
    
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Individual MIDI device row with enable/disable toggle
 */
@Composable
private fun MidiDeviceRow(
    device: MidiDeviceInfo,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onToggle(!isEnabled) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (device.isConnected) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                        )
                )
                
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (device.manufacturer.isNotBlank() && device.manufacturer != "Unknown") {
                Text(
                    text = device.manufacturer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        
        Checkbox(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}