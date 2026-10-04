package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContactRequestEntity
import com.example.data.local.UserEntity
import com.example.data.model.CallType
import com.example.ui.components.UserAvatar
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val contacts by viewModel.filteredContacts.collectAsState()
    val pendingRequests by viewModel.pendingContactRequests.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showMyQrDialog by remember { mutableStateOf(false) }
    var showScanQrDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var selectedUserForDetail by remember { mutableStateOf<UserEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // My RM Call ID Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My RM Call ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("RM Call ID", currentUser.rmCallId))
                                Toast.makeText(context, "Copied ${currentUser.rmCallId} to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = currentUser.rmCallId,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showScanQrDialog = true },
                            modifier = Modifier.testTag("scan_qr_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showMyQrDialog = true },
                            modifier = Modifier.testTag("my_qr_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "My QR",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Pending Contact Requests Section
            if (pendingRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Contact Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        pendingRequests.forEach { req ->
                            ContactRequestItemRow(
                                request = req,
                                onAccept = { viewModel.acceptContactRequest(req) },
                                onDecline = { viewModel.declineContactRequest(req.id) }
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name, RM Call ID, phone...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("contacts_search_input")
            )

            // Contacts List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${contacts.size} Contacts",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = { showAddContactDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Contact")
                }
            }

            // Contacts List or Empty State
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        UserAvatar(name = "RM", size = 64.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No contacts found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add contacts via RM Call ID, Phone number, or QR Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showAddContactDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add First Contact")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(contacts, key = { it.id }) { user ->
                        ContactRowItem(
                            user = user,
                            onClick = { selectedUserForDetail = user },
                            onVoiceCall = {
                                viewModel.startCall(
                                    contactId = user.id,
                                    contactRmCallId = user.rmCallId,
                                    contactName = user.name,
                                    contactAvatar = user.avatarUrl,
                                    contactPhone = user.phoneNumber,
                                    type = CallType.VOICE
                                )
                            },
                            onVideoCall = {
                                viewModel.startCall(
                                    contactId = user.id,
                                    contactRmCallId = user.rmCallId,
                                    contactName = user.name,
                                    contactAvatar = user.avatarUrl,
                                    contactPhone = user.phoneNumber,
                                    type = CallType.VIDEO
                                )
                            },
                            onMessage = {
                                coroutineScope.launch {
                                    val chatId = viewModel.getOrCreateDirectChat(user)
                                    onOpenChat(chatId)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Add Contact Floating Action Button
        FloatingActionButton(
            onClick = { showAddContactDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Contact"
            )
        }
    }

    // Dialogs
    if (showMyQrDialog) {
        MyQRCodeDialog(viewModel = viewModel, onDismiss = { showMyQrDialog = false })
    }

    if (showScanQrDialog) {
        ScanQRCodeDialog(
            viewModel = viewModel,
            onDismiss = { showScanQrDialog = false },
            onContactAdded = {
                Toast.makeText(context, "Contact request sent!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddContactDialog) {
        UnifiedAddContactDialog(
            viewModel = viewModel,
            onDismiss = { showAddContactDialog = false },
            onOpenQrScanner = { showScanQrDialog = true }
        )
    }

    // Contact Quick Action Dialog
    selectedUserForDetail?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = user.name, size = 48.dp, showOnlineBadge = true, isOnline = user.isOnline)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "RM Call ID: ${user.rmCallId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (user.allowPhoneSearch && user.phoneNumber.isNotBlank()) {
                            Text(text = user.phoneNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(text = user.status, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val chatId = viewModel.getOrCreateDirectChat(user)
                                    selectedUserForDetail = null
                                    onOpenChat(chatId)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                selectedUserForDetail = null
                                viewModel.startCall(
                                    contactId = user.id,
                                    contactRmCallId = user.rmCallId,
                                    contactName = user.name,
                                    contactAvatar = user.avatarUrl,
                                    contactPhone = user.phoneNumber,
                                    type = CallType.VOICE
                                )
                            },
                            modifier = Modifier.testTag("modal_audio_call_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                selectedUserForDetail = null
                                viewModel.startCall(
                                    contactId = user.id,
                                    contactRmCallId = user.rmCallId,
                                    contactName = user.name,
                                    contactAvatar = user.avatarUrl,
                                    contactPhone = user.phoneNumber,
                                    type = CallType.VIDEO
                                )
                            },
                            modifier = Modifier.testTag("modal_video_call_btn")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                viewModel.toggleBlockUser(user.id, !user.isBlocked)
                                selectedUserForDetail = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Block/Unblock",
                                tint = if (user.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUserForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ContactRequestItemRow(
    request: ContactRequestEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = request.senderName, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.senderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "ID: ${request.senderRmCallId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("accept_req_${request.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", fontSize = 12.sp)
                }

                IconButton(onClick = onDecline, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ContactRowItem(
    user: UserEntity,
    onClick: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("contact_item_${user.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = user.name,
            size = 48.dp,
            showOnlineBadge = true,
            isOnline = user.isOnline
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.rmCallId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${if (user.isBlocked) "Blocked" else user.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Direct action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMessage, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Message",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onVoiceCall, modifier = Modifier.size(38.dp), enabled = !user.isBlocked) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = if (user.isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onVideoCall, modifier = Modifier.size(38.dp), enabled = !user.isBlocked) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = if (user.isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
