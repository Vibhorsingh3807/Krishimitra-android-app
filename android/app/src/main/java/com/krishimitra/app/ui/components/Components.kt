package com.krishimitra.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.R
import com.krishimitra.app.ui.navigation.Screen
import com.krishimitra.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KrishiTopBar(
    title: String,
    isOnline: Boolean,
    currentLanguage: com.krishimitra.app.domain.language.AppLanguage = com.krishimitra.app.domain.language.AppLanguage.HINDI,
    onLanguageToggle: () -> Unit = {},
    onSourcesClick: () -> Unit = {},
    showBack: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                StatusPill(isOnline = isOnline, isHindi = currentLanguage == com.krishimitra.app.domain.language.AppLanguage.HINDI)
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            // Global Language Switch Toggle Pill (EN | हिन्दी)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onLanguageToggle() }
                    .padding(horizontal = 4.dp),
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentLanguage == com.krishimitra.app.domain.language.AppLanguage.HINDI) "हिन्दी" else "EN",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onSourcesClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Sources",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GreenPrimary
        )
    )
}

@Composable
fun StatusPill(isOnline: Boolean, isHindi: Boolean = true) {
    val bgColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFD84315)
    val text = if (isOnline) {
        if (isHindi) "ऑनलाइन" else "Online"
    } else {
        if (isHindi) "ऑफ़लाइन" else "Offline"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun KrishiBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Screen.Home.route, Icons.Default.Home, stringResource(R.string.nav_home)),
            Triple(Screen.Chat.route, Icons.Default.Chat, stringResource(R.string.nav_assistant)),
            Triple(Screen.Camera.route, Icons.Default.PhotoCamera, stringResource(R.string.nav_camera)),
            Triple(Screen.Weather.route, Icons.Default.Cloud, stringResource(R.string.nav_weather)),
            Triple(Screen.Schemes.route, Icons.Default.AccountBalance, stringResource(R.string.nav_schemes))
        )

        items.forEach { (route, icon, label) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenPrimary,
                    selectedTextColor = GreenPrimary,
                    indicatorColor = GreenPrimaryContainer,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

@Composable
fun LeafViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val size = size.minDimension * 0.70f
        val left = (this.size.width - size) / 2f
        val top = (this.size.height - size) / 2.2f

        // Framing guide outline
        drawRoundRect(
            color = Color(0x66FFFFFF),
            topLeft = Offset(left, top),
            size = Size(size, size),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Corner accents
        val cornerLen = 28.dp.toPx()
        val strokeW = 4.dp.toPx()
        val cornerColor = Color(0xFF4CAF50)

        // Top Left
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

        // Top Right
        drawLine(cornerColor, Offset(left + size, top), Offset(left + size - cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(left + size, top), Offset(left + size, top + cornerLen), strokeW)

        // Bottom Left
        drawLine(cornerColor, Offset(left, top + size), Offset(left + cornerLen, top + size), strokeW)
        drawLine(cornerColor, Offset(left, top + size), Offset(left, top + size - cornerLen), strokeW)

        // Bottom Right
        drawLine(cornerColor, Offset(left + size, top + size), Offset(left + size - cornerLen, top + size), strokeW)
        drawLine(cornerColor, Offset(left + size, top + size), Offset(left + size, top + size - cornerLen), strokeW)
    }
}
