package org.tetawex.cmpsftdemo

/**
 * Stub implementation for Web/JS
 */
class JsSynthManager : SynthManager {
    override suspend fun initialize(): Boolean = false

    override fun playNote(note: Int, velocity: Int) {}

    override fun stopNote(note: Int) {}

    override fun changeProgram(program: Int) {}

    override fun setVolume(volume: Int) {}

    override fun setBufferSize(bufferSize: Int) {}

    override fun isInitialized(): Boolean = false

    override fun cleanup() {}
}

actual fun getSynthManager(): SynthManager = JsSynthManager()
