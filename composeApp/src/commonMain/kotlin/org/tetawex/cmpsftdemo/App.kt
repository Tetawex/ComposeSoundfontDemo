package org.tetawex.cmpsftdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val synthManager = remember { getSynthManager() }
        var synthInitialized by remember { mutableStateOf(false) }
        var volume by remember { mutableStateOf(100f) }
        var program by remember { mutableStateOf(0f) }
        
        // Buffer size presets (WASM only)
        val bufferSizePresets = listOf(64, 128, 256, 512, 1024, 2048, 4096, 8192)
        var selectedBufferSize by remember { mutableStateOf(256) }

        // Initialize synth on first composition
        LaunchedEffect(Unit) {
            synthInitialized = synthManager.initialize()
        }

        // Cleanup on disposal
        DisposableEffect(Unit) {
            onDispose {
                synthManager.cleanup()
            }
        }

        // Keyboard support
        val focusRequester = remember { FocusRequester() }
        val pressedKeys = remember { mutableStateMapOf<Key, Boolean>() }
        
        // Map keyboard keys to MIDI notes
        val keyToNote = remember {
            mapOf(
                // White keys: A S D F G H J K (C D E F G A B C)
                Key.A to 60, // C
                Key.S to 62, // D
                Key.D to 64, // E
                Key.F to 65, // F
                Key.G to 67, // G
                Key.H to 69, // A
                Key.J to 71, // B
                Key.K to 72, // C
                // Black keys: W E T Y U (C# D# F# G# A#)
                Key.W to 61, // C#
                Key.E to 63, // D#
                Key.T to 66, // F#
                Key.Y to 68, // G#
                Key.U to 70, // A#
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (!synthInitialized) return@onKeyEvent false
                    
                    val note = keyToNote[keyEvent.key] ?: return@onKeyEvent false
                    
                    when (keyEvent.type) {
                        KeyEventType.KeyDown -> {
                            if (pressedKeys[keyEvent.key] != true) {
                                pressedKeys[keyEvent.key] = true
                                synthManager.playNote(note, 100)
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            pressedKeys[keyEvent.key] = false
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
                "Synth Compose",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            
            Text(
                "Use keyboard: A S D F G H J K (white keys), W E T Y U (black keys)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Synth Controls
            Text(
                "Synth Controls",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            if (synthInitialized) {
                Text(
                    "Status: Ready",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Volume Control
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Volume: ${volume.toInt()}")
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            synthManager.setVolume(it.toInt())
                        },
                        valueRange = 0f..127f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Program/Instrument Control
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Instrument: ${program.toInt()}")
                    Slider(
                        value = program,
                        onValueChange = {
                            program = it
                            synthManager.changeProgram(it.toInt())
                        },
                        valueRange = 0f..127f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Buffer Size Control (WASM only)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val latencyMs = (selectedBufferSize / 48.0 * 10).toInt() / 10.0
                    Text("Audio Buffer Size: $selectedBufferSize samples (~${latencyMs}ms @ 48kHz)")
                    Text(
                        "Lower = less latency, higher CPU usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bufferSizePresets.forEach { bufferSize ->
                            Button(
                                onClick = {
                                    selectedBufferSize = bufferSize
                                    synthManager.setBufferSize(bufferSize)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = selectedBufferSize != bufferSize
                            ) {
                                Text(
                                    bufferSize.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Piano Keyboard (C Major scale)
                Text(
                    "Play Notes (C Major Scale)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                val notes = listOf(
                    Pair("C", 60),
                    Pair("D", 62),
                    Pair("E", 64),
                    Pair("F", 65),
                    Pair("G", 67),
                    Pair("A", 69),
                    Pair("B", 71),
                    Pair("C", 72)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    notes.forEach { (noteName, noteValue) ->
                        var isPressed by remember { mutableStateOf(false) }
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .pointerInput(noteValue) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                        if (!isPressed) {
                                            isPressed = true
                                            synthManager.playNote(noteValue, 100)
                                        }
                                        down.consume()
                                    }
                                }
                        ) {
                            Text(noteName)
                        }
                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                kotlinx.coroutines.delay(150)
                                synthManager.stopNote(noteValue)
                                isPressed = false
                            }
                        }
                    }
                }

                // Chromatic Scale Buttons
                Text(
                    "Chromatic Notes",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                val chromaticNotes = listOf(
                    Pair("C#", 61),
                    Pair("D#", 63),
                    Pair("F#", 66),
                    Pair("G#", 68),
                    Pair("A#", 70)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    chromaticNotes.forEach { (noteName, noteValue) ->
                        var isPressed by remember { mutableStateOf(false) }
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .pointerInput(noteValue) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                        if (!isPressed) {
                                            isPressed = true
                                            synthManager.playNote(noteValue, 100)
                                        }
                                        down.consume()
                                    }
                                }
                        ) {
                            Text(noteName)
                        }
                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                kotlinx.coroutines.delay(150)
                                synthManager.stopNote(noteValue)
                                isPressed = false
                            }
                        }
                    }
                }

                Button(
                    onClick = { 
                        // Stop all notes by releasing middle C
                        for (i in 60..72) {
                            synthManager.stopNote(i)
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Stop All Notes")
                }
            } else {
                Text(
                    "Status: Synth not available on this platform",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}