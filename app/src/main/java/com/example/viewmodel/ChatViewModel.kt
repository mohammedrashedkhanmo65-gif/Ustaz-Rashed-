package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.RMCallRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RMCallRepository(application.applicationContext)

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    val currentChat: StateFlow<ChatEntity?> = _currentChatId.flatMapLatest { id ->
        if (id != null) repository.getChat(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<MessageEntity>> = _currentChatId.flatMapLatest { id ->
        if (id != null) repository.getMessages(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice recording state
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private var recordTimerJob: Job? = null

    fun setChatId(chatId: String) {
        _currentChatId.value = chatId
        repository.markChatAsRead(chatId)
    }

    fun sendTextMessage(text: String) {
        val chatId = _currentChatId.value ?: return
        if (text.isBlank()) return
        repository.sendMessage(chatId = chatId, text = text.trim(), type = "TEXT")
    }

    fun sendMediaMessage(
        type: String,
        caption: String = "",
        url: String = "",
        duration: Int = 0,
        size: String = ""
    ) {
        val chatId = _currentChatId.value ?: return
        repository.sendMessage(
            chatId = chatId,
            text = caption,
            type = type,
            mediaUrl = url,
            mediaCaption = caption,
            mediaDuration = duration,
            mediaSize = size
        )
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        _recordingDurationSeconds.value = 0
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (_isRecordingVoice.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    fun stopAndSendVoiceRecording() {
        val duration = _recordingDurationSeconds.value
        _isRecordingVoice.value = false
        recordTimerJob?.cancel()
        val chatId = _currentChatId.value ?: return
        if (duration > 0) {
            repository.sendMessage(
                chatId = chatId,
                text = "Voice message",
                type = "VOICE",
                mediaDuration = duration
            )
        }
    }

    fun cancelVoiceRecording() {
        _isRecordingVoice.value = false
        recordTimerJob?.cancel()
        _recordingDurationSeconds.value = 0
    }

    fun reactToMessage(messageId: String, emoji: String) {
        repository.reactToMessage(messageId, emoji)
    }

    fun deleteMessage(messageId: String) {
        repository.deleteMessage(messageId)
    }

    fun clearChatHistory() {
        val chatId = _currentChatId.value ?: return
        repository.clearChat(chatId)
    }
}
