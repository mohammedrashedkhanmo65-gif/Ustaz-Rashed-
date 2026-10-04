package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserEntity
import com.example.ui.components.UserAvatar
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun NewChatOrGroupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allUsers by viewModel.allUsers.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = New Direct Chat, 1 = New Group
    var groupTitle by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Direct Chat") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("New Group") }
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedTab == 0) {
                    Text(
                        text = "Select contact to message:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(allUsers, key = { it.id }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            val chatId = viewModel.getOrCreateDirectChat(user)
                                            onDismiss()
                                            onOpenChat(chatId)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = user.name, size = 40.dp, showOnlineBadge = true, isOnline = user.isOnline)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = user.name, fontWeight = FontWeight.SemiBold)
                                    Text(text = user.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. Family Call Group") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_title_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Participants (${selectedMembers.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(allUsers, key = { it.id }) { user ->
                            val isChecked = selectedMembers.contains(user.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedMembers.remove(user.id)
                                        else selectedMembers.add(user.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedMembers.add(user.id)
                                        else selectedMembers.remove(user.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                UserAvatar(name = user.name, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = user.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        if (groupTitle.isNotBlank()) {
                            viewModel.createGroupChat(groupTitle.trim(), selectedMembers.toList())
                            onDismiss()
                        }
                    },
                    enabled = groupTitle.isNotBlank(),
                    modifier = Modifier.testTag("create_group_confirm_btn")
                ) {
                    Text("Create Group")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
