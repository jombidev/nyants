package dev.jombi.nyants

import korlibs.audio.sound.AudioData
import korlibs.audio.sound.AudioSamples
import korlibs.audio.sound.AudioSamplesDeque
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class VoiceHandler(private val id: Long) : AudioSendHandler {
    private val stream = AudioSamplesDeque(1)
    
    fun isAcceptable(id: Long) = this.id == id

    fun queue(data: AudioData) {
        stream.write(data.samples)
    }

    override fun canProvide(): Boolean {
        return stream.availableRead > 0
    }
    
    private val buf = ByteBuffer.allocate(960 * 4) // 20ms of 24000hz
    private val readSample = AudioSamples(1, 480)
    private val emptySample = AudioSamples(1, 480)
    
    override fun provide20MsAudio(): ByteBuffer? {
        emptySample.setTo(readSample)
        buf.clear()
        
        stream.read(readSample)

        for (sh in readSample.data[0]) for (i in 0..<4) buf.putShort(sh) // extend 24khz mono -> 48khz stereo
        
        buf.flip()
        
        return buf
    }
}