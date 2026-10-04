package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.CallHistoryDao
import com.example.data.local.CallHistoryEntity
import com.example.data.local.ChatDao
import com.example.data.local.ChatEntity
import com.example.data.local.ContactRequestDao
import com.example.data.local.ContactRequestEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.RMCallDatabase
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.model.CallDirection
import com.example.data.model.CallType
import com.example.data.model.CurrentUser
import com.example.data.model.UserSearchResult
import com.example.data.network.CloudSignalingService
import com.example.data.network.SignalingMessage
import com.example.util.QRCodeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class RMCallRepository(
    private val context: Context,
    private val database: RMCallDatabase = RMCallDatabase.getInstance(context),
    private val signalingService: CloudSignalingService = CloudSignalingService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val userDao: UserDao = database.userDao()
    val contactRequestDao: ContactRequestDao = database.contactRequestDao()
    val chatDao: ChatDao = database.chatDao()
    val messageDao: MessageDao = database.messageDao()
    val callDao: CallHistoryDao = database.callHistoryDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rm_call_prefs", Context.MODE_PRIVATE)

    // Current user state
    private val _currentUser = MutableStateFlow(loadCurrentUser())
    val currentUser: StateFlow<CurrentUser> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", true))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // App Preferences
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _callDataSaver = MutableStateFlow(prefs.getBoolean("call_data_saver", false))
    val callDataSaver: StateFlow<Boolean> = _callDataSaver.asStateFlow()

    private val _chatWallpaper = MutableStateFlow(prefs.getString("chat_wallpaper", "Classic") ?: "Classic")
    val chatWallpaper: StateFlow<String> = _chatWallpaper.asStateFlow()

    // In-app Notification Event
    private val _incomingNotification = MutableStateFlow<String?>(null)
    val incomingNotification: StateFlow<String?> = _incomingNotification.asStateFlow()

    // External listener callback hook for active calls
    var onRemoteCallSignal: ((SignalingMessage) -> Unit)? = null

    init {
        scope.launch {
            checkAndSeedInitialData()
            startSignalingListener()
        }
    }

    private fun loadCurrentUser(): CurrentUser {
        val id = prefs.getString("user_id", "user_me") ?: "user_me"
        var rmCallId = prefs.getString("user_rm_call_id", "") ?: ""
        if (rmCallId.isBlank()) {
            rmCallId = QRCodeHelper.generateRmCallId()
            prefs.edit().putString("user_rm_call_id", rmCallId).apply()
        }
        val name = prefs.getString("user_name", "Ustaz User") ?: "Ustaz User"
        val phone = prefs.getString("user_phone", "+1 (555) 234-5678") ?: "+1 (555) 234-5678"
        val avatar = prefs.getString("user_avatar", "") ?: ""
        val status = prefs.getString("user_status", "Available on RM Call") ?: "Available on RM Call"
        val isOnline = prefs.getBoolean("user_is_online", true)
        val lastSeen = prefs.getString("last_seen_privacy", "Everyone") ?: "Everyone"
        val readReceipts = prefs.getBoolean("read_receipts", true)
        val allowPhoneSearch = prefs.getBoolean("allow_phone_search", true)
        val showPhoneInProfile = prefs.getBoolean("show_phone_in_profile", false)

        return CurrentUser(
            id, rmCallId, name, phone, avatar, status, isOnline,
            lastSeen, readReceipts, allowPhoneSearch, showPhoneInProfile
        )
    }

    private fun startSignalingListener() {
        val myRmId = _currentUser.value.rmCallId
        if (myRmId.isNotBlank()) {
            signalingService.startListening(myRmId) { signal ->
                handleIncomingSignal(signal)
            }
        }
    }

    private fun handleIncomingSignal(signal: SignalingMessage) {
        scope.launch {
            when (signal.type) {
                "CALL_INVITE", "CALL_ACCEPT", "CALL_DECLINE", "CALL_END" -> {
                    onRemoteCallSignal?.invoke(signal)
                }
                "CONTACT_REQUEST" -> {
                    val req = ContactRequestEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = signal.senderId,
                        senderRmCallId = signal.senderRmCallId,
                        senderName = signal.senderName,
                        senderPhone = signal.senderPhone,
                        senderAvatar = signal.senderAvatar,
                        senderStatus = signal.senderStatus,
                        receiverRmCallId = _currentUser.value.rmCallId,
                        timestamp = signal.timestamp
                    )
                    contactRequestDao.insertRequest(req)
                    _incomingNotification.value = "New contact request from ${signal.senderName} (${signal.senderRmCallId})"
                }
                "CONTACT_ACCEPT" -> {
                    userDao.setContactStatus(signal.senderRmCallId, true)
                    _incomingNotification.value = "${signal.senderName} accepted your contact request!"
                }
                "CHAT_MESSAGE" -> {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = "chat_" + signal.senderRmCallId,
                        senderId = signal.senderRmCallId,
                        senderName = signal.senderName,
                        text = signal.messageText,
                        messageType = signal.messageType,
                        timestamp = signal.timestamp,
                        isOutgoing = false,
                        status = "READ"
                    )
                    messageDao.insertMessage(msg)
                    chatDao.updateLastMessage("chat_" + signal.senderRmCallId, signal.messageText, signal.timestamp)
                    if (_notificationsEnabled.value) {
                        _incomingNotification.value = "${signal.senderName}: ${signal.messageText}"
                    }
                }
            }
        }
    }

    fun loginOrRegister(name: String, phone: String, status: String, avatar: String) {
        val newRmCallId = QRCodeHelper.generateRmCallId()
        val user = CurrentUser(
            id = "user_" + UUID.randomUUID().toString().take(8),
            rmCallId = newRmCallId,
            name = name.ifBlank { "User" },
            phoneNumber = phone.ifBlank { "+1 (555) 000-0000" },
            avatarUrl = avatar,
            status = status.ifBlank { "Available on RM Call" },
            isOnline = true
        )
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_id", user.id)
            .putString("user_rm_call_id", user.rmCallId)
            .putString("user_name", user.name)
            .putString("user_phone", user.phoneNumber)
            .putString("user_status", user.status)
            .putString("user_avatar", user.avatarUrl)
            .putBoolean("user_is_online", true)
            .apply()
        _currentUser.value = user
        _isLoggedIn.value = true

        startSignalingListener()
    }

    fun logout() {
        signalingService.stop()
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    fun updateProfile(name: String, status: String, phone: String, avatar: String) {
        val updated = _currentUser.value.copy(
            name = name,
            status = status,
            phoneNumber = phone,
            avatarUrl = avatar
        )
        prefs.edit()
            .putString("user_name", updated.name)
            .putString("user_status", updated.status)
            .putString("user_phone", updated.phoneNumber)
            .putString("user_avatar", updated.avatarUrl)
            .apply()
        _currentUser.value = updated
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        val updated = _currentUser.value.copy(isOnline = isOnline)
        prefs.edit().putBoolean("user_is_online", isOnline).apply()
        _currentUser.value = updated
    }

    fun setAllowPhoneSearch(allow: Boolean) {
        val updated = _currentUser.value.copy(allowPhoneSearch = allow)
        prefs.edit().putBoolean("allow_phone_search", allow).apply()
        _currentUser.value = updated
    }

    fun setShowPhoneInProfile(show: Boolean) {
        val updated = _currentUser.value.copy(showPhoneInProfile = show)
        prefs.edit().putBoolean("show_phone_in_profile", show).apply()
        _currentUser.value = updated
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setCallDataSaver(enabled: Boolean) {
        prefs.edit().putBoolean("call_data_saver", enabled).apply()
        _callDataSaver.value = enabled
    }

    fun setChatWallpaper(name: String) {
        prefs.edit().putString("chat_wallpaper", name).apply()
        _chatWallpaper.value = name
    }

    fun setLastSeenPrivacy(privacy: String) {
        val updated = _currentUser.value.copy(lastSeenVisibility = privacy)
        prefs.edit().putString("last_seen_privacy", privacy).apply()
        _currentUser.value = updated
    }

    fun setReadReceipts(enabled: Boolean) {
        val updated = _currentUser.value.copy(readReceipts = enabled)
        prefs.edit().putBoolean("read_receipts", enabled).apply()
        _currentUser.value = updated
    }

    fun dismissNotification() {
        _incomingNotification.value = null
    }

    // Data Streams
    fun getChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()
    fun getChat(id: String): Flow<ChatEntity?> = chatDao.getChatById(id)
    fun getMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)
    fun getContacts(): Flow<List<UserEntity>> = userDao.getContacts()
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getPendingContactRequests(): Flow<List<ContactRequestEntity>> = contactRequestDao.getPendingRequests()
    fun getCallHistory(): Flow<List<CallHistoryEntity>> = callDao.getAllCalls()

    // Search Users for Add Contact
    suspend fun searchUser(query: String): UserSearchResult? {
        val clean = query.trim()
        if (clean.isBlank()) return null

        val myUser = _currentUser.value
        // 1. By RM Call ID
        val parsedRmId = QRCodeHelper.parseRmCallId(clean)
        if (parsedRmId != null) {
            val user = userDao.getUserByRmCallIdDirect(parsedRmId)
            if (user != null && user.id != myUser.id && user.rmCallId != myUser.rmCallId) {
                val requests = contactRequestDao.getAllRequests().firstOrNull() ?: emptyList()
                val hasPending = requests.any { it.senderRmCallId == user.rmCallId && it.status == "PENDING" }
                return UserSearchResult(user, isContact = user.isContact, hasPendingRequest = hasPending)
            }
        }

        // 2. By Phone Number (honors allowPhoneSearch)
        val userByPhone = userDao.getUserByPhoneDirect(clean)
        if (userByPhone != null && userByPhone.id != myUser.id && userByPhone.rmCallId != myUser.rmCallId) {
            val requests = contactRequestDao.getAllRequests().firstOrNull() ?: emptyList()
            val hasPending = requests.any { it.senderRmCallId == userByPhone.rmCallId && it.status == "PENDING" }
            return UserSearchResult(userByPhone, isContact = userByPhone.isContact, hasPendingRequest = hasPending)
        }

        return null
    }

    // Contact Requests
    fun sendContactRequest(targetUser: UserEntity) {
        scope.launch {
            val myUser = _currentUser.value
            // Publish cloud signal
            signalingService.publishSignal(
                targetRmCallId = targetUser.rmCallId,
                signal = SignalingMessage(
                    type = "CONTACT_REQUEST",
                    senderId = myUser.id,
                    senderRmCallId = myUser.rmCallId,
                    senderName = myUser.name,
                    senderPhone = if (myUser.showPhoneInProfile) myUser.phoneNumber else "",
                    senderAvatar = myUser.avatarUrl,
                    senderStatus = myUser.status,
                    targetRmCallId = targetUser.rmCallId
                )
            )

            // Also create target user locally in database if not present
            userDao.insertUser(targetUser.copy(isContact = false))
            _incomingNotification.value = "Contact request sent to ${targetUser.name} (${targetUser.rmCallId})"
        }
    }

    fun acceptContactRequest(request: ContactRequestEntity) {
        scope.launch {
            val myUser = _currentUser.value
            contactRequestDao.updateStatus(request.id, "ACCEPTED")

            // Ensure sender is added to local contacts
            val contact = UserEntity(
                id = request.senderId.ifBlank { "user_" + request.senderRmCallId },
                rmCallId = request.senderRmCallId,
                name = request.senderName,
                phoneNumber = request.senderPhone,
                avatarUrl = request.senderAvatar,
                status = request.senderStatus.ifBlank { "Available on RM Call" },
                isOnline = true,
                isContact = true
            )
            userDao.insertUser(contact)

            // Create direct chat
            getOrCreateDirectChat(contact)

            // Publish acceptance signal back to sender
            signalingService.publishSignal(
                targetRmCallId = request.senderRmCallId,
                signal = SignalingMessage(
                    type = "CONTACT_ACCEPT",
                    senderId = myUser.id,
                    senderRmCallId = myUser.rmCallId,
                    senderName = myUser.name,
                    targetRmCallId = request.senderRmCallId
                )
            )

            _incomingNotification.value = "${request.senderName} added to your contacts!"
        }
    }

    fun declineContactRequest(requestId: String) {
        scope.launch {
            contactRequestDao.deleteRequest(requestId)
        }
    }

    // Call Signaling Methods
    fun sendCallInvite(
        targetRmCallId: String,
        callId: String,
        type: CallType
    ) {
        val myUser = _currentUser.value
        signalingService.publishSignal(
            targetRmCallId = targetRmCallId,
            signal = SignalingMessage(
                type = "CALL_INVITE",
                callId = callId,
                callType = type.name,
                senderId = myUser.id,
                senderRmCallId = myUser.rmCallId,
                senderName = myUser.name,
                senderPhone = myUser.phoneNumber,
                senderAvatar = myUser.avatarUrl,
                targetRmCallId = targetRmCallId
            )
        )
    }

    fun sendCallAccept(targetRmCallId: String, callId: String) {
        val myUser = _currentUser.value
        signalingService.publishSignal(
            targetRmCallId = targetRmCallId,
            signal = SignalingMessage(
                type = "CALL_ACCEPT",
                callId = callId,
                senderRmCallId = myUser.rmCallId,
                targetRmCallId = targetRmCallId
            )
        )
    }

    fun sendCallDecline(targetRmCallId: String, callId: String) {
        val myUser = _currentUser.value
        signalingService.publishSignal(
            targetRmCallId = targetRmCallId,
            signal = SignalingMessage(
                type = "CALL_DECLINE",
                callId = callId,
                senderRmCallId = myUser.rmCallId,
                targetRmCallId = targetRmCallId
            )
        )
    }

    fun sendCallEnd(targetRmCallId: String, callId: String) {
        val myUser = _currentUser.value
        signalingService.publishSignal(
            targetRmCallId = targetRmCallId,
            signal = SignalingMessage(
                type = "CALL_END",
                callId = callId,
                senderRmCallId = myUser.rmCallId,
                targetRmCallId = targetRmCallId
            )
        )
    }

    // Message Actions
    fun sendMessage(
        chatId: String,
        targetRmCallId: String = "",
        text: String,
        type: String = "TEXT",
        mediaUrl: String = "",
        mediaCaption: String = "",
        mediaDuration: Int = 0,
        mediaSize: String = ""
    ) {
        scope.launch {
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val sender = _currentUser.value
            val message = MessageEntity(
                id = messageId,
                chatId = chatId,
                senderId = sender.id,
                senderName = sender.name,
                text = text,
                messageType = type,
                mediaUrl = mediaUrl,
                mediaCaption = mediaCaption,
                mediaDuration = mediaDuration,
                mediaSize = mediaSize,
                timestamp = timestamp,
                isOutgoing = true,
                status = "SENT"
            )
            messageDao.insertMessage(message)

            val previewText = when (type) {
                "IMAGE" -> "📷 Photo"
                "VIDEO" -> "🎥 Video"
                "DOCUMENT" -> "📄 Document"
                "VOICE" -> "🎤 Voice message (${mediaDuration}s)"
                else -> text
            }
            chatDao.updateLastMessage(chatId, previewText, timestamp)

            // Publish message over internet if recipient has RM Call ID
            if (targetRmCallId.isNotBlank()) {
                signalingService.publishSignal(
                    targetRmCallId = targetRmCallId,
                    signal = SignalingMessage(
                        type = "CHAT_MESSAGE",
                        senderId = sender.id,
                        senderRmCallId = sender.rmCallId,
                        senderName = sender.name,
                        messageText = text,
                        messageType = type,
                        targetRmCallId = targetRmCallId,
                        timestamp = timestamp
                    )
                )
            }

            // Simulate read tick
            delay(1200)
            messageDao.updateStatus(messageId, "READ")
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        scope.launch { messageDao.updateReaction(messageId, emoji) }
    }

    fun deleteMessage(messageId: String) {
        scope.launch { messageDao.deleteMessageById(messageId) }
    }

    fun clearChat(chatId: String) {
        scope.launch {
            messageDao.deleteMessagesForChat(chatId)
            chatDao.updateLastMessage(chatId, "Chat history cleared", System.currentTimeMillis())
        }
    }

    fun deleteChat(chatId: String) {
        scope.launch {
            messageDao.deleteMessagesForChat(chatId)
            chatDao.deleteChatById(chatId)
        }
    }

    fun markChatAsRead(chatId: String) {
        scope.launch { chatDao.clearUnreadCount(chatId) }
    }

    // Call History Actions
    fun recordCallHistory(
        contactId: String,
        contactName: String,
        contactRmCallId: String,
        contactAvatar: String,
        type: CallType,
        direction: CallDirection,
        durationSeconds: Int
    ) {
        scope.launch {
            val call = CallHistoryEntity(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                contactName = contactName,
                contactRmCallId = contactRmCallId,
                contactAvatar = contactAvatar,
                callType = type.name,
                callDirection = direction.name,
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds
            )
            callDao.insertCall(call)
        }
    }

    fun deleteCall(id: String) {
        scope.launch { callDao.deleteCallById(id) }
    }

    fun clearAllCalls() {
        scope.launch { callDao.clearAllCalls() }
    }

    fun toggleBlockUser(userId: String, isBlocked: Boolean) {
        scope.launch { userDao.setBlocked(userId, isBlocked) }
    }

    suspend fun getOrCreateDirectChat(user: UserEntity): String {
        val existingChats = chatDao.getAllChats().firstOrNull() ?: emptyList()
        val direct = existingChats.find { !it.isGroup && (it.participantIds == user.id || it.participantIds == user.rmCallId) }
        if (direct != null) return direct.id

        val newChatId = "chat_" + (if (user.rmCallId.isNotBlank()) user.rmCallId else user.id)
        val newChat = ChatEntity(
            id = newChatId,
            title = user.name,
            isGroup = false,
            lastMessage = "Start your conversation with ${user.name}",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            participantIds = user.rmCallId.ifBlank { user.id },
            avatarUrl = user.avatarUrl
        )
        chatDao.insertChat(newChat)
        return newChatId
    }

    fun createGroupChat(title: String, memberIds: List<String>) {
        scope.launch {
            val groupId = "group_" + UUID.randomUUID().toString().take(8)
            val newGroup = ChatEntity(
                id = groupId,
                title = title,
                isGroup = true,
                lastMessage = "Group created",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                participantIds = memberIds.joinToString(","),
                avatarUrl = ""
            )
            chatDao.insertChat(newGroup)
            val welcomeMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = groupId,
                senderId = "system",
                senderName = "RM Call System",
                text = "Welcome to the group \"$title\"!",
                messageType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isOutgoing = false
            )
            messageDao.insertMessage(welcomeMsg)
        }
    }

    // Seeding demo contacts with permanent RM Call IDs
    private suspend fun checkAndSeedInitialData() {
        val existingUsers = userDao.getAllUsers().firstOrNull()
        if (!existingUsers.isNullOrEmpty()) return

        val initialUsers = listOf(
            UserEntity(
                id = "user_rashed",
                rmCallId = "RM-7K4P92",
                name = "Ustaz Rashed Zahid",
                phoneNumber = "+880 1711 002233",
                avatarUrl = "",
                status = "Developer of RM Call. Making communication simple & accessible.",
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                isContact = true
            ),
            UserEntity(
                id = "user_sarah",
                rmCallId = "RM-2B8N4Q",
                name = "Sarah Ahmed",
                phoneNumber = "+1 (555) 432-8765",
                avatarUrl = "",
                status = "In a meeting | Call if urgent",
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis() - 1000 * 60 * 3,
                isContact = true
            ),
            UserEntity(
                id = "user_bilal",
                rmCallId = "RM-9W1C5J",
                name = "Bilal Farooq",
                phoneNumber = "+44 7911 123456",
                avatarUrl = "",
                status = "Loving the RM Call HD audio quality!",
                isOnline = false,
                lastSeenTimestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                isContact = true
            ),
            UserEntity(
                id = "user_aisha",
                rmCallId = "RM-4M6V8T",
                name = "Aisha Rahman",
                phoneNumber = "+971 50 987 6543",
                avatarUrl = "",
                status = "Available on RM Call",
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                isContact = true
            )
        )
        userDao.insertUsers(initialUsers)

        // Initial Chats
        val chatRashed = ChatEntity(
            id = "chat_RM-7K4P92",
            title = "Ustaz Rashed Zahid",
            isGroup = false,
            lastMessage = "Welcome to RM Call! Try an audio or video call.",
            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 10,
            unreadCount = 1,
            participantIds = "RM-7K4P92",
            isPinned = true
        )
        val chatSarah = ChatEntity(
            id = "chat_RM-2B8N4Q",
            title = "Sarah Ahmed",
            isGroup = false,
            lastMessage = "🎤 Voice message (14s)",
            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 35,
            unreadCount = 0,
            participantIds = "RM-2B8N4Q"
        )
        chatDao.insertChats(listOf(chatRashed, chatSarah))

        val now = System.currentTimeMillis()
        val messagesRashed = listOf(
            MessageEntity(
                id = "m_r_1",
                chatId = "chat_RM-7K4P92",
                senderId = "RM-7K4P92",
                senderName = "Ustaz Rashed Zahid",
                text = "Assalamu Alaikum! Welcome to RM Call.",
                messageType = "TEXT",
                timestamp = now - 1000 * 60 * 20,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_r_2",
                chatId = "chat_RM-7K4P92",
                senderId = "user_me",
                senderName = "You",
                text = "Thank you Ustaz! The calling system is very fast.",
                messageType = "TEXT",
                timestamp = now - 1000 * 60 * 15,
                isOutgoing = true,
                status = "READ",
                reaction = "❤️"
            )
        )
        messageDao.insertMessages(messagesRashed)

        // Seed call history
        val sampleCalls = listOf(
            CallHistoryEntity(
                id = "c_1",
                contactId = "user_rashed",
                contactName = "Ustaz Rashed Zahid",
                contactRmCallId = "RM-7K4P92",
                callType = "VOICE",
                callDirection = "OUTGOING",
                timestamp = now - 1000 * 60 * 45,
                durationSeconds = 245
            ),
            CallHistoryEntity(
                id = "c_2",
                contactId = "user_sarah",
                contactName = "Sarah Ahmed",
                contactRmCallId = "RM-2B8N4Q",
                callType = "VIDEO",
                callDirection = "INCOMING",
                timestamp = now - 1000 * 60 * 180,
                durationSeconds = 480
            )
        )
        callDao.insertCalls(sampleCalls)
    }
}
