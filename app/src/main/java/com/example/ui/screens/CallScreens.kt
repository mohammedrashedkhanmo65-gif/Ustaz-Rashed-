package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveCall
import com.example.data.model.CallState
import com.example.data.model.CallType
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CallRed
import com.example.ui.theme.OnlineGreen
import com.example.viewmodel.MainViewModel

@Composable
fun FullCallOverlay(
    activeCall: ActiveCall,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        // Prevent accidental closing; user should tap end call
    }

    when (activeCall.state) {
        CallState.INCOMING -> {
            IncomingCallScreen(activeCall = activeCall, viewModel = viewModel, modifier = modifier)
        }
        CallState.DIALING -> {
            OutgoingCallScreen(activeCall = activeCall, viewModel = viewModel, modifier = modifier)
        }
        CallState.CONNECTED -> {
            if (activeCall.type == CallType.VIDEO) {
                ActiveVideoCallScreen(activeCall = activeCall, viewModel = viewModel, modifier = modifier)
            } else {
                ActiveVoiceCallScreen(activeCall = activeCall, viewModel = viewModel, modifier = modifier)
            }
        }
        CallState.ENDED -> {
            CallEndedScreen(activeCall = activeCall, modifier = modifier)
        }
        CallState.IDLE -> {}
    }
}

@Composable
private fun IncomingCallScreen(
    activeCall: ActiveCall,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF021B35), Color(0xFF0A0F1D))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("incoming_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Security badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RM Call End-to-End Encrypted",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Caller Avatar with animated pulse
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0x330084FF))
                )
                UserAvatar(name = activeCall.contactName, size = 110.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = activeCall.contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (activeCall.type == CallType.VIDEO) "Incoming RM Video Call..." else "Incoming RM Voice Call...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons: Decline (Red) and Accept (Green)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.declineIncomingCall() },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CallRed)
                            .testTag("decline_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Decline", color = Color.White, fontSize = 14.sp)
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.acceptIncomingCall() },
                        modifier = Modifier
                            .size(68.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                            .testTag("accept_call_button")
                    ) {
                        Icon(
                            imageVector = if (activeCall.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Accept Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Accept", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OutgoingCallScreen(
    activeCall: ActiveCall,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "calling")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A192F), Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("outgoing_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "RM Call • Ultra HD",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(56.dp))

            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0x2238BDF8))
                )
                UserAvatar(name = activeCall.contactName, size = 110.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = activeCall.contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ringing...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Outgoing call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallRoundButton(
                    icon = if (activeCall.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    label = "Speaker",
                    isActive = activeCall.isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() }
                )

                CallRoundButton(
                    icon = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = "Mute",
                    isActive = activeCall.isMuted,
                    onClick = { viewModel.toggleMute() }
                )

                if (activeCall.type == CallType.VIDEO) {
                    CallRoundButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip",
                        isActive = false,
                        onClick = { viewModel.flipCamera() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // End Call Button
            IconButton(
                onClick = { viewModel.endActiveCall() },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CallRed)
                    .testTag("end_outgoing_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActiveVoiceCallScreen(
    activeCall: ActiveCall,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val durationFormatted = remember(activeCall.durationSeconds) {
        val mins = activeCall.durationSeconds / 60
        val secs = activeCall.durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D16))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("active_voice_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Quality pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = OnlineGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HD Voice Audio • Connected",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            UserAvatar(name = activeCall.contactName, size = 120.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = activeCall.contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnlineGreen
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pulsing Audio Waveform
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(18, 36, 22, 48, 14, 40, 28, 52, 20, 34, 16, 26)
                heights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF38BDF8))
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Grid: Mute, Speaker, Dialpad, Hold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallRoundButton(
                    icon = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (activeCall.isMuted) "Unmute" else "Mute",
                    isActive = activeCall.isMuted,
                    onClick = { viewModel.toggleMute() }
                )

                CallRoundButton(
                    icon = if (activeCall.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    label = "Speaker",
                    isActive = activeCall.isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() }
                )

                CallRoundButton(
                    icon = Icons.Default.Dialpad,
                    label = "Keypad",
                    isActive = false,
                    onClick = { }
                )

                CallRoundButton(
                    icon = Icons.Default.Pause,
                    label = "Hold",
                    isActive = false,
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // End Call Button
            IconButton(
                onClick = { viewModel.endActiveCall() },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CallRed)
                    .testTag("end_active_voice_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActiveVideoCallScreen(
    activeCall: ActiveCall,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val durationFormatted = remember(activeCall.durationSeconds) {
        val mins = activeCall.durationSeconds / 60
        val secs = activeCall.durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("active_video_call_screen")
    ) {
        // Simulated Fullscreen Remote Video Stream
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(name = activeCall.contactName, size = 130.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = activeCall.contactName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "HD Video Streaming (1080p)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // Top Status Header (Timer & Badges)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = durationFormatted,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.flipCamera() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Flip Camera",
                    tint = Color.White
                )
            }
        }

        // Picture-in-Picture Local Selfie Preview (Top Right)
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
                .size(width = 110.dp, height = 150.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (activeCall.isVideoEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatar(name = "You", size = 48.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "You", color = Color.White, fontSize = 11.sp)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "Video Disabled",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Floating Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flip camera
            IconButton(
                onClick = { viewModel.flipCamera() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }

            // Video Toggle
            IconButton(
                onClick = { viewModel.toggleVideo() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (!activeCall.isVideoEnabled) CallRed else Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (activeCall.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Video",
                    tint = Color.White
                )
            }

            // Mute Mic Toggle
            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (activeCall.isMuted) CallRed else Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Microphone",
                    tint = Color.White
                )
            }

            // End Call Button
            IconButton(
                onClick = { viewModel.endActiveCall() },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CallRed)
                    .testTag("end_video_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CallEndedScreen(
    activeCall: ActiveCall,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UserAvatar(name = activeCall.contactName, size = 96.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = activeCall.contactName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Call Ended",
                style = MaterialTheme.typography.bodyLarge,
                color = CallRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}
