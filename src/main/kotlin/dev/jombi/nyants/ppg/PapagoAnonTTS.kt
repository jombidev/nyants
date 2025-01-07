package dev.jombi.nyants.ppg

import dev.jombi.nyants.json
import dev.jombi.nyants.ppg.dto.PapagoErrorResponse
import dev.jombi.nyants.ppg.dto.PapagoTTSResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class PapagoAnonTTS {
    companion object {
        const val ENDPOINT = "https://papago.naver.com"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/132.0"
    }

    val client = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(UserAgent) {
            agent = USER_AGENT
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    init {
        PapagoUUIDGen.gen()
        PapagoSecretGen.gen()
    }

    suspend fun tts(target: String): ByteArray {
        val offset = System.currentTimeMillis()
        val url = "$ENDPOINT/apis/tts/makeID"
        val uuid = PapagoUUIDGen.gen()
        val hash = createAuthorization(offset, url)

        val con = client.submitForm(
            url,
            formParameters = parameters {
                append("alpha", "0")
                append("pitch", "0")
                append("speaker", "kyuri")
                append("speed", "0")
                append("text", target)
            },
            encodeInQuery = false,
        ) {
            header(HttpHeaders.Authorization, "PPG $uuid:$hash")
            header("Timestamp", offset)
        }
        
        val ppg = if (con.status.isSuccess()) {
            con.body<PapagoTTSResponse>()
        } else {
            throw TTSException(con.body<PapagoErrorResponse>().errorMessage)
        }

        val data = client.get("https://papago.naver.com/apis/tts/${ppg.id}").bodyAsBytes()
        return data
    }

    private fun createAuthorization(time: Long, url: String): String {
        val uuid = PapagoUUIDGen.gen()
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(PapagoSecretGen.gen().toByteArray(charset("utf-8")), "HmacMD5"))
        val myMac = uuid + "\n" + url.split("?")[0] + "\n" + time
        return Base64.encode(mac.doFinal(myMac.toByteArray()))
    }
}