package com.krishimitra.app.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.R
import com.krishimitra.app.domain.ai.HybridAIRouter
import com.krishimitra.app.domain.model.ChatMessage
import com.krishimitra.app.ui.theme.*
import com.krishimitra.app.voice.VoiceManager
import com.krishimitra.app.voice.VoiceMode
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    aiRouter: HybridAIRouter,
    voiceManager: VoiceManager
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val voiceMode by voiceManager.voiceMode.collectAsState()
    val voiceState by voiceManager.voiceState.collectAsState()
    val lastVoiceError by voiceManager.lastError.collectAsState()

    val appLocale = remember {
        try {
            Locale.getDefault().language ?: "hi"
        } catch (e: Exception) {
            "hi"
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "नमस्ते! मैं आपका कृषिमित्र कृषि सहायक हूँ। आप अपनी फसल, मंडी भाव, उन्नत किस्में, खाद-बीज, रोग उपचार व सरकारी योजनाओं के बारे में कोई भी प्रश्न पूछ सकते हैं।",
                isUser = false,
                source = "भाकृअनुप (ICAR) एवं एगमार्कनेट प्रमाणित ज्ञानकोश",
                isVerified = true
            )
        )
    }

    val sampleQueries = listOf(
        "धान का ताजा मंडी भाव क्या है?",
        "गेहूं की उन्नत किस्में कौन सी हैं?",
        "कपास का सबसे अच्छा भाव किस मंडी में है?",
        "पालक्काड़ में नारियल का भाव बताओ",
        "टमाटर में अगेती झुलसा का क्या इलाज है?",
        "पीएम किसान योजना के लिए आवेदन कैसे करें?",
        "किसान क्रेडिट कार्ड (KCC) की ब्याज दर क्या है?"
    )

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(text = query, isUser = true)
        messages.add(userMsg)
        inputText = ""

        coroutineScope.launch {
            try {
                listState.animateScrollToItem(messages.size - 1)
                val isAppHindi = appLocale == "hi"
                val forceLang = when (voiceMode) {
                    VoiceMode.HINDI -> "hi"
                    VoiceMode.ENGLISH -> "en"
                    VoiceMode.AUTO -> {
                        if (com.krishimitra.app.voice.LanguageDetector.isHindiResponsePreferred(query, VoiceMode.AUTO)) "hi"
                        else if (isAppHindi) "hi"
                        else "en"
                    }
                }
                val reply = aiRouter.routeQuery(query, forceLang = forceLang)
                messages.add(reply)
                listState.animateScrollToItem(messages.size - 1)
                voiceManager.speak(reply.text, forceHindi = (forceLang == "hi"))
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        text = "क्षमा करें, उत्तर लोड करने में समस्या आई। कृपया दोबारा प्रयास करें।",
                        isUser = false,
                        source = "कृषिमित्र सहायक",
                        isVerified = false
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Voice Mode Selector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (voiceMode) {
                    VoiceMode.AUTO -> stringResource(R.string.voice_mode_auto)
                    VoiceMode.HINDI -> stringResource(R.string.voice_mode_hi)
                    VoiceMode.ENGLISH -> stringResource(R.string.voice_mode_en)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            // Switch language chip
            AssistChip(
                onClick = {
                    val nextMode = when (voiceMode) {
                        VoiceMode.AUTO -> VoiceMode.HINDI
                        VoiceMode.HINDI -> VoiceMode.ENGLISH
                        VoiceMode.ENGLISH -> VoiceMode.AUTO
                    }
                    voiceManager.setVoiceMode(nextMode)
                },
                label = { Text("बदलें (Switch)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    onSpeakClick = { voiceManager.speak(msg.text) }
                )
            }
        }

        // Suggestions horizontal row
        if (messages.size <= 2) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleQueries) { q ->
                    Surface(
                        modifier = Modifier.clickable { sendMessage(q) },
                        shape = RoundedCornerShape(16.dp),
                        color = GreenPrimaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = q,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GreenDark,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Hold-to-Talk Status Indicator
        AnimatedVisibility(visible = isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.assistant_listening),
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Voice Error / Offline Notice Banner
        AnimatedVisibility(visible = lastVoiceError != null && !isListening) {
            lastVoiceError?.let { err ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = err,
                        color = Color(0xFFE65100),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.assistant_hint),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = CardBorder
                ),
                maxLines = 3
            )

            // If text typed, show Send button; otherwise Hold-to-Talk Mic button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = { sendMessage(inputText) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            } else {
                // Original Hold-to-Talk Mic Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AlertRed else GreenPrimary)
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    voiceManager.startListening { query ->
                                        sendMessage(query)
                                    }
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    voiceManager.stopListening()
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.assistant_hold_to_talk),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakClick: () -> Unit
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) GreenPrimary else Color.White
    val textColor = if (isUser) Color.White else TextPrimary
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        lineHeight = 22.sp
                    )
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (message.tokenUsage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = message.tokenUsage ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Verified Source Tag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.source ?: stringResource(R.string.assistant_verified_tag),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            )
                        }

                        // Listen / TTS Speaker Button
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                tint = GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
