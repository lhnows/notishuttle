package com.notishuttle.webhook

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Sends a notification payload to the configured webhook over HTTP POST. */
object WebhookSender {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * @return true when the webhook responded with a 2xx status.
     */
    fun send(url: String, payload: String, secret: String, headersJson: String): Boolean {
        val request = buildRequest(url, payload, secret, headersJson)
        client.newCall(request).execute().use { response ->
            response.body?.close()
            return response.isSuccessful
        }
    }

    private fun buildRequest(url: String, payload: String, secret: String, headersJson: String): Request {
        val builder = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "NotiShuttle/1.0")
            .header("X-NotiShuttle-Event", "notification")

        sign(payload, secret)?.let { builder.header("X-NotiShuttle-Signature", "sha256=$it") }

        parseHeaders(headersJson).forEach { (name, value) ->
            if (name.isNotBlank()) builder.header(name.trim(), value)
        }

        return builder.build()
    }

    private fun parseHeaders(headersJson: String): Map<String, String> {
        if (headersJson.isBlank()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(headersJson, type) ?: emptyMap()
        }.getOrDefault(emptyMap())
    }

    private fun sign(payload: String, secret: String): String? {
        if (secret.isBlank()) return null
        return runCatching {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            mac.doFinal(payload.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
        }.getOrNull()
    }
}
