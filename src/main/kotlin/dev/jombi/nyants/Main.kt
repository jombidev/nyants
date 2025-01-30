package dev.jombi.nyants

import club.minnced.opus.util.OpusLibrary
import dev.jombi.nyants.ppg.PapagoAnonTTS
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.jdabuilder.light
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.MP3Decoder
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.audio.AudioNatives
import net.dv8tion.jda.api.audio.SpeakingMode
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*

fun main() = runBlocking {
    val field = OpusLibrary::class.java.getDeclaredField("platforms")
    field.trySetAccessible()

    @Suppress("UNCHECKED_CAST")
    val map = field.get(OpusLibrary::class.java) as MutableMap<String, String>
    map["darwin-aarch64"] = "dylib" // small cheese

    AudioNatives.ensureOpus()

    val listening: MutableMap<Long, VoiceHandler> = Collections.synchronizedMap(mutableMapOf())
    val papagoTTS = PapagoAnonTTS()

    val jda = light(System.getenv("token"), enableCoroutines = true) {
        enableIntents(GatewayIntent.entries)

        enableCache(CacheFlag.getPrivileged())
        enableCache(CacheFlag.VOICE_STATE)

        setMemberCachePolicy(MemberCachePolicy.DEFAULT)

        setActivity(Activity.watching("jombi.dev"))
        setStatus(OnlineStatus.IDLE)
    }

    jda.upsertCommand("join", "통화에 접속합니다.") {
        isGuildOnly = true
    }.await()

    jda.onCommand("join") {
        val voice = it.member!!.voiceState?.channel
            ?: return@onCommand it.response("통화에 들어와있어야 합니다.")

        val man = it.guild!!.audioManager
        if (man.isConnected)
            return@onCommand it.response("저는 이미 ${man.connectedChannel!!.asMention} 통화방에 들어와 있습니다.")

        man.setSpeakingMode(SpeakingMode.VOICE)
        man.isSelfDeafened = true
        val handler = VoiceHandler(it.messageChannel.idLong)
        man.sendingHandler = handler
        man.openAudioConnection(voice)

        listening[it.guild!!.idLong] = handler

        it.response("ㅎㅇ!", false)
    }
    
    jda.upsertCommand("leave", "통화를 나갑니다") {
        isGuildOnly = true
    }.await()
    
    jda.onCommand("leave") {
        val voice = it.member!!.voiceState?.channel
            ?: return@onCommand it.response("통화에 들어와있어야 합니다.")
        
        val man = it.guild!!.audioManager

        if (man.connectedChannel != voice)
            return@onCommand it.response("같은 통화방에 들어와 있어야 합니다.")
        
        listening.remove(it.guild!!.idLong)
        man.closeAudioConnection()
        
        it.response("ㅂㅇ", false)
    }

    jda.listener<MessageReceivedEvent> {
        val man = it.guild.audioManager
        if (!man.isConnected) return@listener

        val typer = it.author.idLong
        val chanId = it.channel.idLong
        if (man.connectedChannel!!.members.none { it.idLong == typer })
            return@listener
        
        val listener = listening[it.guild.idLong] ?: return@listener
     
        
        if (listener.isAcceptable(chanId)) {
            val file = papagoTTS.tts(it.message.contentDisplay)
            val audio = MP3Decoder.decode(file, AudioDecodingProps(maxSamples = 15 * 60 * 24000))!!
            listener.queue(audio)
        }
    }
    
    jda.listener<GuildVoiceUpdateEvent> {
        val man = it.guild.audioManager
        if (!man.isConnected) return@listener

        if (man.connectedChannel!!.members.size == 1)
            man.closeAudioConnection() // bye
    }

    Unit
}

suspend fun GenericCommandInteractionEvent.response(msg: String, ephemeral: Boolean = true) =
    reply(msg).setEphemeral(ephemeral).await().let {}