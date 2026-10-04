package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val rmCallId: String = "",
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String = "",
    val status: String = "Available",
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val allowPhoneSearch: Boolean = true,
    val isContact: Boolean = true
)

@Entity(tableName = "contact_requests")
data class ContactRequestEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderRmCallId: String,
    val senderName: String,
    val senderPhone: String,
    val senderAvatar: String = "",
    val senderStatus: String = "",
    val receiverRmCallId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "ACCEPTED", "DECLINED"
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val participantIds: String = "", // Comma-separated user IDs or RM Call IDs
    val avatarUrl: String = "",
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String = "",
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "DOCUMENT", "VOICE"
    val mediaUrl: String = "",
    val mediaCaption: String = "",
    val mediaDuration: Int = 0, // seconds for audio/video
    val mediaSize: String = "", // e.g. "2.4 MB"
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false,
    val status: String = "SENT", // "SENDING", "SENT", "DELIVERED", "READ"
    val reaction: String = "" // emoji reaction e.g. "❤️", "👍"
)

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val contactRmCallId: String = "",
    val contactAvatar: String = "",
    val callType: String = "VOICE", // "VOICE", "VIDEO"
    val callDirection: String = "INCOMING", // "INCOMING", "OUTGOING", "MISSED"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)
