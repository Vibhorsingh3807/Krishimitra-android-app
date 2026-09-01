package com.krishimitra.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
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
import java.io.InputStream
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    aiRouter: HybridAIRouter,
    voiceManager: VoiceManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

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

    // Gallery Photo Picker Launcher for Gemini Vision AI
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    attachedBitmap = bitmap
                }
            } catch (e: Throwable) {
                android.util.Log.e("ChatScreen", "Failed to load attached photo: ${e.message}")
            }
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "नमस्ते! मैं आपका कृषिमित्र कृषि सहायक हूँ। आप अपनी फसल, मंडी भाव, उन्नत किस्में, खाद-बीज, रोग उपचार के बारे में प्रश्न पूछ सकते हैं, बोल सकते हैं या फसल की फोटो (🖼️ Attach Image) अपलोड करके Gemini Vision AI से सीधा सलाह प्राप्त कर सकते हैं।",
                isUser = false,
                source = "✨ Gemini 1.5 Flash Vision & Grok AI Certified",
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

    fun bitmapToBase64(bmp: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    fun sendMessage(query: String, bitmap: Bitmap? = attachedBitmap) {
        if (query.isBlank() && bitmap == null) return
        val userMsg = ChatMessage(text = query, isUser = true, attachedImageBitmap = bitmap)
        messages.add(userMsg)

        val imageBase64 = if (bitmap != null) bitmapToBase64(bitmap) else null
        inputText = ""
        attachedBitmap = null

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
                val reply = aiRouter.routeMultimodalQuery(query, imageBase64, forceLang = forceLang)
                messages.add(reply)
                listState.animateScrollToItem(messages.size - 1)
                voiceManager.speak(reply.text, forceHindi = (forceLang == "hi"))
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        text = "क्षमा करें, उत्तर प्राप्त करने में समस्या आई। कृपया दोबारा प्रयास करें।",
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

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = voiceMode == VoiceMode.AUTO,
                    onClick = { voiceManager.setVoiceMode(VoiceMode.AUTO) },
                    label = { Text("Auto", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = voiceMode == VoiceMode.HINDI,
                    onClick = { voiceManager.setVoiceMode(VoiceMode.HINDI) },
                    label = { Text("हिंदी", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = voiceMode == VoiceMode.ENGLISH,
                    onClick = { voiceManager.setVoiceMode(VoiceMode.ENGLISH) },
                    label = { Text("English", fontSize = 11.sp) }
                )
            }
        }

        // Active Error / Listening Banner
        AnimatedVisibility(visible = isListening || isSpeaking || !lastVoiceError.isNullOrEmpty()) {
            val statusColor = when {
                isListening -> AlertRed
                isSpeaking -> GreenPrimary
                else -> WarningOrange
            }
            val statusMsg = when {
                isListening -> stringResource(R.string.assistant_listening)
                isSpeaking -> "बोल रहा है… (Speaking)"
                else -> lastVoiceError ?: ""
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMsg,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message,
                    onSpeakClick = {
                        voiceManager.speak(message.text)
                    }
                )
            }
        }

        // Sample Query Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleQueries) { query ->
                Surface(
                    modifier = Modifier.clickable { sendMessage(query) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                ) {
                    Text(
                        text = query,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GreenDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Image Attachment Preview Bar for Gemini Vision
        if (attachedBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEFEF))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        bitmap = attachedBitmap!!.asImageBitmap(),
                        contentDescription = "Attached photo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("फसल चित्र संलग्न है (Image Attached)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Gemini Vision AI से विश्लेषण होगा", fontSize = 10.5.sp, color = TextSecondary)
                    }
                }

                IconButton(onClick = { attachedBitmap = null }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove photo", tint = AlertRed)
                }
            }
        }

        // Bottom Input Bar (Attach Image + Text Input + Hold-to-Talk Mic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Photo Attachment Button for Gemini Vision AI
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach crop photo",
                    tint = GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (attachedBitmap != null) "चित्र के बारे में प्रश्न पूछें…" else stringResource(R.string.assistant_hint),
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

            // If text typed or image attached, show Send button; otherwise Hold-to-Talk Mic button
            if (inputText.isNotBlank() || attachedBitmap != null) {
                IconButton(
                    onClick = { sendMessage(inputText, attachedBitmap) },
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
                                        sendMessage(query, attachedBitmap)
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
                        contentDescription = "Hold to Talk",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) GreenPrimary else Color.White)
                .padding(14.dp)
        ) {
            Column {
                // Render attached crop photo inside user message bubble
                if (message.attachedImageBitmap != null) {
                    Image(
                        bitmap = message.attachedImageBitmap.asImageBitmap(),
                        contentDescription = "Attached crop image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
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
                                text = message.tokenUsage,
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
