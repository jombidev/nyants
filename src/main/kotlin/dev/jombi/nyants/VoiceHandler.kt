package dev.jombi.nyants

import korlibs.audio.format.WAV
import korlibs.audio.sound.AudioData
import korlibs.audio.sound.AudioSamples
import korlibs.audio.sound.AudioSamplesDeque
import korlibs.io.async.runBlockingNoSuspensions
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class VoiceHandler : AudioSendHandler {
    // AudioFormat(11025f, 1152, 2, true, true)
    private val stream = AudioSamplesDeque(1)

    fun queue(data: AudioData) {
//        Path("${i++}.wav").apply { 
//            if (!exists())
//                createFile()
//        }.writeBytes(data.toWav())
        stream.write(data.samples)
    }

    override fun canProvide(): Boolean {
        return stream.availableRead > 0
    }
    
    override fun provide20MsAudio(): ByteBuffer? {
        val samples = AudioSamples(2, 480 * 2) // two channel, 48kHz 20ms audio samples
        
        val readSample = AudioSamples(1, 480)
        stream.read(readSample)
        
        for ((i, datum) in readSample.data[0].withIndex()) { // OG Sound is mono
            samples.setStereo(i * 2 + 0, datum, datum)
            samples.setStereo(i * 2 + 1, datum, datum)
        }
        
        val blocked = runBlockingNoSuspensions {
            WAV.encodeToByteArray(AudioData(48000, samples))
        }
        
        return ByteBuffer.wrap(blocked)
    }

    override fun isOpus(): Boolean {
        return true
    }
}