package dev.jombi.nyants

import dev.jombi.nyants.ppg.PapagoAnonTTS
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.jdabuilder.light
import korlibs.audio.format.defaultAudioFormats
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.OnlineStatus
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
            ?: return@onCommand it.reject("통화에 들어와있어야 합니다.")

        val man = it.guild!!.audioManager
        if (man.isConnected)
            return@onCommand it.reject("저는 이미 ${man.connectedChannel!!.asMention} 통화방에 들어와 있습니다.")
        
        man.setSpeakingMode(SpeakingMode.VOICE)
        man.isSelfDeafened = true
        val handler = VoiceHandler()
        man.sendingHandler = handler
        man.openAudioConnection(voice)
        
        listening[it.messageChannel.idLong] = handler
            
        it.reject("ㅎㅇ!", false)
    }
    
    jda.listener<MessageReceivedEvent> {
        val man = it.guild.audioManager
        if (!man.isConnected) return@listener
        
        val chanId = it.channel.idLong
        if (chanId in listening) {
            val file = papagoTTS.tts(it.message.contentRaw)

            val audio = defaultAudioFormats.decode(file)!!
            listening[chanId]!!.queue(audio)
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

suspend fun GenericCommandInteractionEvent.reject(msg: String, ephemeral: Boolean = true) = reply(msg).setEphemeral(ephemeral).await().let{}