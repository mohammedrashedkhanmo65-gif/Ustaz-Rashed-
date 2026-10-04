package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CallType
import com.example.ui.components.InAppNotificationBanner
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.MainViewModel

enum class MainTab(val title: String) {
    HOME("Home"),
    CHATS("Chats"),
    CALLS("Calls"),
    CONTACTS("Contacts"),
    PROFILE("Profile")
}

sealed class CurrentScreen {
    object Main : CurrentScreen()
    data class ChatDetail(val chatId: String) : CurrentScreen()
    object Settings : CurrentScreen()
    object AboutUs : CurrentScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var currentScreen by remember { mutableStateOf<CurrentScreen>(CurrentScreen.Main) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    var showScanQrDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    val activeCall by mainViewModel.activeCall.collectAsState()
    val incomingNotification by mainViewModel.incomingNotification.collectAsState()
    val chats by mainViewModel.allChats.collectAsState()
    val unreadTotal = chats.sumOf { it.unreadCount }

    Box(modifier = modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is CurrentScreen.ChatDetail -> {
                ChatDetailScreen(
                    chatId = screen.chatId,
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel,
                    onBack = { currentScreen = CurrentScreen.Main }
                )
            }

            is CurrentScreen.Settings -> {
                SettingsScreen(
                    viewModel = mainViewModel,
                    onBack = { currentScreen = CurrentScreen.Main }
                )
            }

            is CurrentScreen.AboutUs -> {
                AboutUsScreen(
                    onBack = { currentScreen = CurrentScreen.Main }
                )
            }

            is CurrentScreen.Main -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A192F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_app_logo),
                                            contentDescription = "RM Call Logo",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "RM Call",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 20.sp
                                    )
                                }
                            },
                            actions = {
                                // Scan QR Shortcut
                                IconButton(
                                    onClick = { showScanQrDialog = true },
                                    modifier = Modifier.testTag("top_scan_qr_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Share App Button
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Download RM Call - Simple & fast voice and video calls: https://rmcall.app"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share RM Call"))
                                    },
                                    modifier = Modifier.testTag("top_share_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share RM Call",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // More menu
                                Box {
                                    IconButton(
                                        onClick = { showTopMenu = true },
                                        modifier = Modifier.testTag("top_menu_btn")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = showTopMenu,
                                        onDismissRequest = { showTopMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("My QR Code") },
                                            onClick = {
                                                showTopMenu = false
                                                showMyQrDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.QrCode, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Scan QR Code") },
                                            onClick = {
                                                showTopMenu = false
                                                showScanQrDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Contact") },
                                            onClick = {
                                                showTopMenu = false
                                                showAddContactDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("New Group") },
                                            onClick = {
                                                showTopMenu = false
                                                showNewChatDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Simulate Incoming Call") },
                                            onClick = {
                                                showTopMenu = false
                                                mainViewModel.triggerTestIncomingCall(CallType.VIDEO)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.PlayCircleOutline, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings") },
                                            onClick = {
                                                showTopMenu = false
                                                currentScreen = CurrentScreen.Settings
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Settings, contentDescription = null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("About RM Call") },
                                            onClick = {
                                                showTopMenu = false
                                                currentScreen = CurrentScreen.AboutUs
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Info, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            MainTab.values().forEach { tab ->
                                val isSelected = selectedTab == tab
                                val icon = when (tab) {
                                    MainTab.HOME -> if (isSelected) Icons.Default.Home else Icons.Outlined.Home
                                    MainTab.CHATS -> if (isSelected) Icons.Default.Chat else Icons.Outlined.Chat
                                    MainTab.CALLS -> if (isSelected) Icons.Default.Call else Icons.Outlined.Call
                                    MainTab.CONTACTS -> if (isSelected) Icons.Default.People else Icons.Outlined.People
                                    MainTab.PROFILE -> if (isSelected) Icons.Default.Person else Icons.Outlined.Person
                                }

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        if (tab == MainTab.CHATS && unreadTotal > 0) {
                                            BadgedBox(badge = {
                                                Badge { Text(unreadTotal.toString()) }
                                            }) {
                                                Icon(imageVector = icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(imageVector = icon, contentDescription = tab.title)
                                        }
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToChats = { selectedTab = MainTab.CHATS },
                                    onNavigateToCalls = { selectedTab = MainTab.CALLS },
                                    onNavigateToContacts = { selectedTab = MainTab.CONTACTS },
                                    onOpenChat = { chatId -> currentScreen = CurrentScreen.ChatDetail(chatId) },
                                    onOpenAboutUs = { currentScreen = CurrentScreen.AboutUs },
                                    onNewChatClick = { showNewChatDialog = true }
                                )
                            }
                            MainTab.CHATS -> {
                                ChatsListScreen(
                                    viewModel = mainViewModel,
                                    onOpenChat = { chatId -> currentScreen = CurrentScreen.ChatDetail(chatId) },
                                    onNewChatClick = { showNewChatDialog = true }
                                )
                            }
                            MainTab.CALLS -> {
                                CallsHistoryScreen(
                                    viewModel = mainViewModel,
                                    onStartNewCall = { selectedTab = MainTab.CONTACTS }
                                )
                            }
                            MainTab.CONTACTS -> {
                                ContactsScreen(
                                    viewModel = mainViewModel,
                                    onOpenChat = { chatId -> currentScreen = CurrentScreen.ChatDetail(chatId) }
                                )
                            }
                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    viewModel = mainViewModel,
                                    onOpenSettings = { currentScreen = CurrentScreen.Settings },
                                    onOpenAboutUs = { currentScreen = CurrentScreen.AboutUs }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Call Overlay (Takes over screen if incoming or in-progress)
        activeCall?.let { call ->
            FullCallOverlay(
                activeCall = call,
                viewModel = mainViewModel
            )
        }

        // In-App Notification Banner for simulated incoming chat messages
        InAppNotificationBanner(
            notificationText = incomingNotification,
            onDismiss = { mainViewModel.dismissNotification() }
        )

        // New Chat or Group Dialog
        if (showNewChatDialog) {
            NewChatOrGroupDialog(
                viewModel = mainViewModel,
                onDismiss = { showNewChatDialog = false },
                onOpenChat = { chatId ->
                    currentScreen = CurrentScreen.ChatDetail(chatId)
                }
            )
        }

        // QR Code & Add Contact Dialogs
        if (showMyQrDialog) {
            MyQRCodeDialog(
                viewModel = mainViewModel,
                onDismiss = { showMyQrDialog = false }
            )
        }

        if (showScanQrDialog) {
            ScanQRCodeDialog(
                viewModel = mainViewModel,
                onDismiss = { showScanQrDialog = false },
                onContactAdded = {
                    android.widget.Toast.makeText(context, "Contact request sent!", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAddContactDialog) {
            UnifiedAddContactDialog(
                viewModel = mainViewModel,
                onDismiss = { showAddContactDialog = false },
                onOpenQrScanner = { showScanQrDialog = true }
            )
        }
    }
}
