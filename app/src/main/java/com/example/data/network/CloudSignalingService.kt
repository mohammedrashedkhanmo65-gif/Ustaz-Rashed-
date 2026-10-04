package com.example.data.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class SignalingMessage(
    val type: String, // "CALL_INVITE", "CALL_ACCEPT", "CALL_DECLINE", "CALL_END", "CONTACT_REQUEST", "CONTACT_ACCEPT", "CHAT_MESSAGE"
    val callId: String = "",
    val callType: String = "VOICE", // "VOICE" or "VIDEO"
    val senderId: String = "",
    val senderRmCallId: String = "",
    val senderName: String = "",
    val senderPhone: String = "",
    val senderAvatar: String = "",
    val senderStatus: String = "",
    val targetRmCallId: String = "",
    val messageText: String = "",
    val messageType: String = "TEXT",
    val timestamp: Long = System.currentTimeMillis()
)

class CloudSignalingService(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var listenerJob: Job? = null
    private var currentListeningId: String = ""

    private fun topicFor(rmCallId: String): String {
        val clean = rmCallId.replace("-", "").lowercase().trim()
        return "rmcall_sig_$clean"
    }

    fun publishSignal(targetRmCallId: String, signal: SignalingMessage) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", signal.type)
                    put("callId", signal.callId)
                    put("callType", signal.callType)
                    put("senderId", signal.senderId)
                    put("senderRmCallId", signal.senderRmCallId)
                    put("senderName", signal.senderName)
                    put("senderPhone", signal.senderPhone)
                    put("senderAvatar", signal.senderAvatar)
                    put("senderStatus", signal.senderStatus)
                    put("targetRmCallId", targetRmCallId)
                    put("messageText", signal.messageText)
                    put("messageType", signal.messageType)
                    put("timestamp", signal.timestamp)
                }

                val topic = topicFor(targetRmCallId)
                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://ntfy.sh/$topic")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("CloudSignaling", "Published to $topic: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("CloudSignaling", "Publish failed: ${e.message}")
            }
        }
    }

    fun startListening(myRmCallId: String, onSignalReceived: (SignalingMessage) -> Unit) {
        if (myRmCallId.isBlank() || (currentListeningId == myRmCallId && listenerJob?.isActive == true)) {
            return
        }
        currentListeningId = myRmCallId
        listenerJob?.cancel()

        listenerJob = scope.launch {
            val topic = topicFor(myRmCallId)
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("https://ntfy.sh/$topic/json?since=now")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            delay(4000)
                            return@use
                        }

                        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                        var line: String? = null
                        while (isActive && reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: continue
                            if (l.startsWith("{") && l.endsWith("}")) {
                                try {
                                    val wrapper = JSONObject(l)
                                    val event = wrapper.optString("event")
                                    if (event == "message") {
                                        val rawMsg = wrapper.optString("message")
                                        if (rawMsg.isNotBlank() && rawMsg.startsWith("{")) {
                                            val parsed = parseJsonToSignal(JSONObject(rawMsg))
                                            if (parsed != null) {
                                                onSignalReceived(parsed)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CloudSignaling", "Parse line error: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CloudSignaling", "Stream reconnecting in 3s: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    private fun parseJsonToSignal(json: JSONObject): SignalingMessage? {
        val type = json.optString("type")
        if (type.isBlank()) return null
        return SignalingMessage(
            type = type,
            callId = json.optString("callId"),
            callType = json.optString("callType", "VOICE"),
            senderId = json.optString("senderId"),
            senderRmCallId = json.optString("senderRmCallId"),
            senderName = json.optString("senderName"),
            senderPhone = json.optString("senderPhone"),
            senderAvatar = json.optString("senderAvatar"),
            senderStatus = json.optString("senderStatus"),
            targetRmCallId = json.optString("targetRmCallId"),
            messageText = json.optString("messageText"),
            messageType = json.optString("messageType", "TEXT"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    fun stop() {
        listenerJob?.cancel()
        currentListeningId = ""
    }
}
