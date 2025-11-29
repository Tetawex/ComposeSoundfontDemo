package org.tetawex.cmpsftdemo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import composesoundfontdemo.composeapp.generated.resources.Res
import composesoundfontdemo.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val synthManager = remember { getSynthManager() }
        var synthInitialized by remember { mutableStateOf(false) }
        var volume by remember { mutableStateOf(100f) }
        var program by remember { mutableStateOf(0f) }

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

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Toggle Info")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }

            // Synth Controls
            Text(
                "Synth Controls",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
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
                        Button(
                            onClick = { synthManager.playNote(noteValue, 100) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text(noteName)
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
                        Button(
                            onClick = { synthManager.playNote(noteValue, 100) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text(noteName)
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