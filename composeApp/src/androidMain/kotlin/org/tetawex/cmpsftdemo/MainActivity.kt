package org.tetawex.cmpsftdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Extract SoundFont from resources if not already present
        extractSoundFontIfNeeded()
        
        // Initialize synth manager
        initializeSynthManager(this)

        setContent {
            App()
        }
    }
    
    private fun extractSoundFontIfNeeded() {
        val soundFontFile = File(filesDir, "sft_gu_gs.sf2")
        if (!soundFontFile.exists()) {
            try {
                // Read from raw resources (filename: sft_gu_gs.sf2)
                val resourceId = resources.getIdentifier("sft_gu_gs", "raw", packageName)
                if (resourceId != 0) {
                    val inputStream = resources.openRawResource(resourceId)
                    inputStream.use { input ->
                        soundFontFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.util.Log.i("MainActivity", "SoundFont extracted to: ${soundFontFile.absolutePath}")
                } else {
                    android.util.Log.e("MainActivity", "SoundFont resource not found")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to extract SoundFont", e)
            }
        } else {
            android.util.Log.i("MainActivity", "SoundFont already exists at: ${soundFontFile.absolutePath}")
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}