package com.example.data.repository

import com.example.data.model.ActiveCall
import com.example.data.model.CallDirection
import com.example.data.model.CallState
import com.example.data.model.CallType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CallManager(
    private val repository: RMCallRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    private var timerJob: Job? = null
    private var simulationTimeoutJob: Job? = null

    init {
        // Wire remote signaling events from repository
        repository.onRemoteCallSignal = { signal ->
            scope.launch {
                when (signal.type) {
                    "CALL_INVITE" -> {
                        if (_activeCall.value == null) {
                            val call = ActiveCall(
                                callId = signal.callId.ifBlank { UUID.randomUUID().toString() },
                                contactId = signal.senderId,
                                contactRmCallId = signal.senderRmCallId,
                                contactName = signal.senderName,
                                contactAvatar = signal.senderAvatar,
                                contactPhone = signal.senderPhone,
                                type = if (signal.callType == "VIDEO") CallType.VIDEO else CallType.VOICE,
                                state = CallState.INCOMING,
                                isSpeakerOn = (signal.callType == "VIDEO")
                            )
                            _activeCall.value = call
                        }
                    }
                    "CALL_ACCEPT" -> {
                        val current = _activeCall.value
                        if (current != null && current.callId == signal.callId) {
                            simulationTimeoutJob?.cancel()
                            _activeCall.value = current.copy(state = CallState.CONNECTED)
                            startCallTimer()
                        }
                    }
                    "CALL_DECLINE" -> {
                        val current = _activeCall.value
                        if (current != null && current.callId == signal.callId) {
                            simulationTimeoutJob?.cancel()
                            timerJob?.cancel()
                            repository.recordCallHistory(
                                contactId = current.contactId,
                                contactName = current.contactName,
                                contactRmCallId = current.contactRmCallId,
                                contactAvatar = current.contactAvatar,
                                type = current.type,
                                direction = CallDirection.MISSED,
                                durationSeconds = 0
                            )
                            _activeCall.value = current.copy(state = CallState.ENDED)
                            delay(600)
                            _activeCall.value = null
                        }
                    }
                    "CALL_END" -> {
                        val current = _activeCall.value
                        if (current != null && current.callId == signal.callId) {
                            simulationTimeoutJob?.cancel()
                            timerJob?.cancel()
                            repository.recordCallHistory(
                                contactId = current.contactId,
                                contactName = current.contactName,
                                contactRmCallId = current.contactRmCallId,
                                contactAvatar = current.contactAvatar,
                                type = current.type,
                                direction = CallDirection.INCOMING,
                                durationSeconds = current.durationSeconds
                            )
                            _activeCall.value = current.copy(state = CallState.ENDED)
                            delay(600)
                            _activeCall.value = null
                        }
                    }
                }
            }
        }
    }

    fun startOutgoingCall(
        contactId: String,
        contactRmCallId: String,
        contactName: String,
        contactAvatar: String,
        contactPhone: String,
        type: CallType
    ) {
        val callId = UUID.randomUUID().toString()
        val call = ActiveCall(
            callId = callId,
            contactId = contactId,
            contactRmCallId = contactRmCallId,
            contactName = contactName,
            contactAvatar = contactAvatar,
            contactPhone = contactPhone,
            type = type,
            state = CallState.DIALING,
            isSpeakerOn = (type == CallType.VIDEO)
        )
        _activeCall.value = call

        // Publish real-time cloud signaling to target user's RM Call ID!
        if (contactRmCallId.isNotBlank()) {
            repository.sendCallInvite(
                targetRmCallId = contactRmCallId,
                callId = callId,
                type = type
            )
        }

        // Demo simulation fallback if calling demo contact without second device active
        simulationTimeoutJob?.cancel()
        simulationTimeoutJob = scope.launch {
            delay(3200)
            if (_activeCall.value?.state == CallState.DIALING) {
                _activeCall.value = _activeCall.value?.copy(state = CallState.CONNECTED)
                startCallTimer()
            }
        }
    }

    fun triggerSimulatedIncomingCall(
        contactId: String = "user_rashed",
        contactRmCallId: String = "RM-7K4P92",
        contactName: String = "Ustaz Rashed Zahid",
        contactAvatar: String = "",
        contactPhone: String = "+880 1711 002233",
        type: CallType = CallType.VIDEO
    ) {
        if (_activeCall.value != null) return
        val call = ActiveCall(
            callId = UUID.randomUUID().toString(),
            contactId = contactId,
            contactRmCallId = contactRmCallId,
            contactName = contactName,
            contactAvatar = contactAvatar,
            contactPhone = contactPhone,
            type = type,
            state = CallState.INCOMING,
            isSpeakerOn = (type == CallType.VIDEO)
        )
        _activeCall.value = call

        simulationTimeoutJob?.cancel()
        simulationTimeoutJob = scope.launch {
            delay(25000)
            if (_activeCall.value?.state == CallState.INCOMING) {
                repository.recordCallHistory(
                    contactId = contactId,
                    contactName = contactName,
                    contactRmCallId = contactRmCallId,
                    contactAvatar = contactAvatar,
                    type = type,
                    direction = CallDirection.MISSED,
                    durationSeconds = 0
                )
                _activeCall.value = null
            }
        }
    }

    fun acceptIncomingCall() {
        val call = _activeCall.value ?: return
        simulationTimeoutJob?.cancel()
        _activeCall.value = call.copy(state = CallState.CONNECTED)
        startCallTimer()

        if (call.contactRmCallId.isNotBlank()) {
            repository.sendCallAccept(call.contactRmCallId, call.callId)
        }
    }

    fun declineIncomingCall() {
        val call = _activeCall.value ?: return
        simulationTimeoutJob?.cancel()
        timerJob?.cancel()

        if (call.contactRmCallId.isNotBlank()) {
            repository.sendCallDecline(call.contactRmCallId, call.callId)
        }

        repository.recordCallHistory(
            contactId = call.contactId,
            contactName = call.contactName,
            contactRmCallId = call.contactRmCallId,
            contactAvatar = call.contactAvatar,
            type = call.type,
            direction = CallDirection.MISSED,
            durationSeconds = 0
        )
        _activeCall.value = null
    }

    fun endCall() {
        val call = _activeCall.value ?: return
        simulationTimeoutJob?.cancel()
        timerJob?.cancel()

        if (call.contactRmCallId.isNotBlank()) {
            repository.sendCallEnd(call.contactRmCallId, call.callId)
        }

        val direction = if (call.state == CallState.INCOMING) CallDirection.MISSED else CallDirection.OUTGOING
        repository.recordCallHistory(
            contactId = call.contactId,
            contactName = call.contactName,
            contactRmCallId = call.contactRmCallId,
            contactAvatar = call.contactAvatar,
            type = call.type,
            direction = direction,
            durationSeconds = call.durationSeconds
        )

        _activeCall.value = call.copy(state = CallState.ENDED)
        scope.launch {
            delay(600)
            _activeCall.value = null
        }
    }

    fun toggleMute() {
        _activeCall.value?.let { call ->
            _activeCall.value = call.copy(isMuted = !call.isMuted)
        }
    }

    fun toggleSpeaker() {
        _activeCall.value?.let { call ->
            _activeCall.value = call.copy(isSpeakerOn = !call.isSpeakerOn)
        }
    }

    fun toggleVideo() {
        _activeCall.value?.let { call ->
            _activeCall.value = call.copy(isVideoEnabled = !call.isVideoEnabled)
        }
    }

    fun flipCamera() {
        _activeCall.value?.let { call ->
            _activeCall.value = call.copy(isFrontCamera = !call.isFrontCamera)
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_activeCall.value?.state == CallState.CONNECTED) {
                delay(1000)
                _activeCall.value?.let { call ->
                    _activeCall.value = call.copy(durationSeconds = call.durationSeconds + 1)
                }
            }
        }
    }
}
