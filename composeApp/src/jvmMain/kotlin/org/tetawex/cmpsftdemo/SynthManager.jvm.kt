package org.tetawex.cmpsftdemo

/**
 * Stub implementation for JVM/Desktop
 */
class StubSynthManager : SynthManager {
    override suspend fun initialize(): Boolean {
        println("SynthManager: Synth not available on desktop")
        return false
    }

    override fun playNote(note: Int, velocity: Int) {
        println("SynthManager: playNote($note, $velocity) - not available on desktop")
    }

    override fun stopNote(note: Int) {
        println("SynthManager: stopNote($note) - not available on desktop")
    }

    override fun changeProgram(program: Int) {
        println("SynthManager: changeProgram($program) - not available on desktop")
    }

    override fun setVolume(volume: Int) {
        println("SynthManager: setVolume($volume) - not available on desktop")
    }

    override fun setBufferSize(bufferSize: Int) {
        println("SynthManager: setBufferSize($bufferSize) - not available on desktop")
    }

    override fun isInitialized(): Boolean = false

    override fun cleanup() {
        // No-op
    }
}

actual fun getSynthManager(): SynthManager = StubSynthManager()
