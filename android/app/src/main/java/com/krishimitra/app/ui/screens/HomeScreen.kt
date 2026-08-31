package com.krishimitra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.R
import com.krishimitra.app.ui.navigation.Screen
import com.krishimitra.app.ui.theme.*

data class ActionCardItem(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val primaryColor: Color,
    val route: String
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    val actionItems = listOf(
        ActionCardItem(
            R.string.action_ask_ai,
            R.string.action_ask_ai_desc,
            Icons.Default.Chat,
            GreenPrimary,
            Screen.Chat.route
        ),
        ActionCardItem(
            R.string.action_scan_leaf,
            R.string.action_scan_leaf_desc,
            Icons.Default.PhotoCamera,
            AmberSecondary,
            Screen.Camera.route
        ),
        ActionCardItem(
            R.string.action_weather,
            R.string.action_weather_desc,
            Icons.Default.Cloud,
            Color(0xFF0288D1),
            Screen.Weather.route
        ),
        ActionCardItem(
            R.string.action_schemes,
            R.string.action_schemes_desc,
            Icons.Default.AccountBalance,
            Color(0xFF6A1B9A),
            Screen.Schemes.route
        ),
        ActionCardItem(
            R.string.action_loans,
            R.string.action_loans_desc,
            Icons.Default.Payments,
            Color(0xFF2E7D32),
            Screen.Loans.route
        ),
        ActionCardItem(
            R.string.action_mandi,
            R.string.action_mandi_desc,
            Icons.Default.TrendingUp,
            Color(0xFFE65100),
            Screen.Mandi.route
        ),
        ActionCardItem(
            R.string.action_crops,
            R.string.action_crops_desc,
            Icons.Default.Grass,
            Color(0xFF00695C),
            Screen.CropGuide.route
        )
    )


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
    ) {
        // Welcome Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(GreenPrimary, Color(0xFF2E7D32))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_welcome),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE8F5E9),
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Season Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFFFE082),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "रबी व जायद कृषि सत्र 2026",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section Title
        Text(
            text = stringResource(R.string.home_quick_actions),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Action Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(actionItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { onNavigate(item.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.primaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(item.descRes),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
