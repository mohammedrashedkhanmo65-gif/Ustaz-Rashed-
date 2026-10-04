package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CallHistoryEntity
import com.example.data.local.ChatEntity
import com.example.data.local.ContactRequestEntity
import com.example.data.local.UserEntity
import com.example.data.model.ActiveCall
import com.example.data.model.CallType
import com.example.data.model.ChatFilter
import com.example.data.model.CurrentUser
import com.example.data.model.UserSearchResult
import com.example.data.repository.CallManager
import com.example.data.repository.RMCallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = RMCallRepository(application.applicationContext)
    val callManager = CallManager(repository)

    val currentUser: StateFlow<CurrentUser> = repository.currentUser
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
    val callDataSaver: StateFlow<Boolean> = repository.callDataSaver
    val chatWallpaper: StateFlow<String> = repository.chatWallpaper
    val incomingNotification: StateFlow<String?> = repository.incomingNotification

    val activeCall: StateFlow<ActiveCall?> = callManager.activeCall

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _chatFilter = MutableStateFlow(ChatFilter.ALL)
    val chatFilter: StateFlow<ChatFilter> = _chatFilter.asStateFlow()

    val allChats: StateFlow<List<ChatEntity>> = repository.getChats().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        allChats,
        _searchQuery,
        _chatFilter
    ) { chats, query, filter ->
        chats.filter { chat ->
            val matchesQuery = query.isBlank() ||
                    chat.title.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.UNREAD -> chat.unreadCount > 0
                ChatFilter.DIRECT -> !chat.isGroup
                ChatFilter.GROUPS -> chat.isGroup
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Accepted contacts only
    val contacts: StateFlow<List<UserEntity>> = repository.getContacts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredContacts: StateFlow<List<UserEntity>> = combine(
        contacts,
        _searchQuery
    ) { users, query ->
        if (query.isBlank()) users
        else users.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.rmCallId.contains(query, ignoreCase = true) ||
            (it.allowPhoneSearch && it.phoneNumber.contains(query, ignoreCase = true)) ||
            it.status.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pendingContactRequests: StateFlow<List<ContactRequestEntity>> = repository.getPendingContactRequests().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val callHistory: StateFlow<List<CallHistoryEntity>> = repository.getCallHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setChatFilter(filter: ChatFilter) {
        _chatFilter.value = filter
    }

    fun startCall(
        contactId: String,
        contactRmCallId: String,
        contactName: String,
        contactAvatar: String,
        contactPhone: String,
        type: CallType
    ) {
        callManager.startOutgoingCall(
            contactId = contactId,
            contactRmCallId = contactRmCallId,
            contactName = contactName,
            contactAvatar = contactAvatar,
            contactPhone = contactPhone,
            type = type
        )
    }

    fun triggerTestIncomingCall(type: CallType = CallType.VIDEO) {
        callManager.triggerSimulatedIncomingCall(type = type)
    }

    fun acceptIncomingCall() = callManager.acceptIncomingCall()
    fun declineIncomingCall() = callManager.declineIncomingCall()
    fun endActiveCall() = callManager.endCall()
    fun toggleMute() = callManager.toggleMute()
    fun toggleSpeaker() = callManager.toggleSpeaker()
    fun toggleVideo() = callManager.toggleVideo()
    fun flipCamera() = callManager.flipCamera()

    fun login(name: String, phone: String, status: String, avatar: String) {
        repository.loginOrRegister(name, phone, status, avatar)
    }

    fun logout() = repository.logout()

    fun updateProfile(name: String, status: String, phone: String, avatar: String) {
        repository.updateProfile(name, status, phone, avatar)
    }

    fun toggleOnline(isOnline: Boolean) = repository.toggleOnlineStatus(isOnline)

    fun setAllowPhoneSearch(allow: Boolean) = repository.setAllowPhoneSearch(allow)
    fun setShowPhoneInProfile(show: Boolean) = repository.setShowPhoneInProfile(show)

    fun deleteChat(chatId: String) = repository.deleteChat(chatId)

    fun deleteCall(id: String) = repository.deleteCall(id)
    fun clearCalls() = repository.clearAllCalls()

    // Add Contact & Search
    suspend fun searchUser(query: String): UserSearchResult? {
        return repository.searchUser(query)
    }

    fun sendContactRequest(user: UserEntity) {
        repository.sendContactRequest(user)
    }

    fun acceptContactRequest(request: ContactRequestEntity) {
        repository.acceptContactRequest(request)
    }

    fun declineContactRequest(requestId: String) {
        repository.declineContactRequest(requestId)
    }

    fun toggleBlockUser(userId: String, isBlocked: Boolean) {
        repository.toggleBlockUser(userId, isBlocked)
    }

    fun setDarkMode(enabled: Boolean) = repository.setDarkMode(enabled)
    fun setNotificationsEnabled(enabled: Boolean) = repository.setNotificationsEnabled(enabled)
    fun setCallDataSaver(enabled: Boolean) = repository.setCallDataSaver(enabled)
    fun setChatWallpaper(name: String) = repository.setChatWallpaper(name)
    fun setLastSeenPrivacy(privacy: String) = repository.setLastSeenPrivacy(privacy)
    fun setReadReceipts(enabled: Boolean) = repository.setReadReceipts(enabled)
    fun dismissNotification() = repository.dismissNotification()

    fun createGroupChat(title: String, memberIds: List<String>) {
        repository.createGroupChat(title, memberIds)
    }

    suspend fun getOrCreateDirectChat(user: UserEntity): String {
        return repository.getOrCreateDirectChat(user)
    }
}
