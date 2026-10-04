package com.example.data.model

import com.example.data.local.UserEntity

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallState {
    IDLE,
    DIALING,
    INCOMING,
    CONNECTED,
    ENDED
}

data class ActiveCall(
    val callId: String,
    val contactId: String,
    val contactRmCallId: String = "",
    val contactName: String,
    val contactAvatar: String,
    val contactPhone: String,
    val type: CallType,
    val state: CallState,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val durationSeconds: Int = 0,
    val startTimestamp: Long = System.currentTimeMillis()
)

data class CurrentUser(
    val id: String = "user_me",
    val rmCallId: String = "RM-7K4P92",
    val name: String = "Ustaz User",
    val phoneNumber: String = "+1 (555) 234-5678",
    val avatarUrl: String = "",
    val status: String = "Hey there! I am using RM Call",
    val isOnline: Boolean = true,
    val lastSeenVisibility: String = "Everyone", // Everyone, My Contacts, Nobody
    val readReceipts: Boolean = true,
    val allowPhoneSearch: Boolean = true, // Whether strangers can search by phone
    val showPhoneInProfile: Boolean = false // Do not expose private phone number unnecessarily
)

enum class ChatFilter {
    ALL,
    UNREAD,
    DIRECT,
    GROUPS
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    DOCUMENT,
    VOICE
}

data class UserSearchResult(
    val user: UserEntity,
    val isContact: Boolean,
    val hasPendingRequest: Boolean
)
