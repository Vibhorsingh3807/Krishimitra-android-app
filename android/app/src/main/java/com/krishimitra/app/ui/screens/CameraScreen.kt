package com.krishimitra.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.R
import com.krishimitra.app.domain.model.DiseaseDiagnosisResult
import com.krishimitra.app.ml.OnnxDiseaseClassifier
import com.krishimitra.app.ui.components.LeafViewfinderOverlay
import com.krishimitra.app.ui.theme.*

@Composable
fun CameraScreen(
    classifier: OnnxDiseaseClassifier
) {
    var diagnosisResult by remember { mutableStateOf<DiseaseDiagnosisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    fun captureAndAnalyze(simulatedLeafType: Int = 0) {
        isAnalyzing = true
        // Create an evaluation image bitmap representative of an affected leaf
        val bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()

        // Background leaf tissue
        paint.color = when (simulatedLeafType) {
            1 -> AndroidColor.rgb(180, 160, 40) // Yellow Rust
            2 -> AndroidColor.rgb(90, 60, 30)   // Blight / Blast necrotic lesion
            else -> AndroidColor.rgb(45, 140, 45) // Healthy green leaf
        }
        canvas.drawRect(0f, 0f, 224f, 224f, paint)

        // Draw leaf vein structure
        paint.color = AndroidColor.rgb(30, 110, 30)
        paint.strokeWidth = 3f
        canvas.drawLine(112f, 0f, 112f, 224f, paint)

        // Run on-device ONNX Runtime classification
        diagnosisResult = classifier.classifyLeaf(bmp)
        isAnalyzing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Camera Viewfinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Color(0xFF1E241E)),
            contentAlignment = Alignment.Center
        ) {
            LeafViewfinderOverlay()

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.camera_instruction),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x88000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Capture Button
                Button(
                    onClick = { captureAndAnalyze(simulatedLeafType = 2) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.camera_capture),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Test sample switcher (Healthy leaf / Diseased leaf / Rust)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { captureAndAnalyze(simulatedLeafType = 0) }) {
                Text("स्वस्थ पत्ती", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { captureAndAnalyze(simulatedLeafType = 2) }) {
                Text("झुलसा रोग", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { captureAndAnalyze(simulatedLeafType = 1) }) {
                Text("पीला रतुआ", fontSize = 12.sp)
            }
        }

        // Diagnosis Results Display
        if (diagnosisResult != null) {
            val result = diagnosisResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.diagnosis_result),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        )

                        // Confidence Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (result.isUncertain) Color(0xFFFFEBEE) else GreenPrimaryContainer
                        ) {
                            Text(
                                text = "${(result.confidence * 100).toInt()}% सटीकता",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.isUncertain) AlertRed else GreenDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = CardBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val isHindi = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "hi"

                    if (result.isUncertain) {
                        // Uncertain Warning
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.uncertain_warning),
                                color = Color(0xFFE65100),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        // Crop & Disease Name
                        Text(
                            text = if (isHindi) "फसल: ${result.crop}" else "Crop: ${result.crop}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isHindi) "रोग: ${result.diseaseNameHi} (${result.diseaseNameEn})" else "Disease: ${result.diseaseNameEn} (${result.diseaseNameHi})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AlertRed
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Symptoms
                        Text(
                            text = stringResource(R.string.diagnosis_symptoms),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = result.symptoms,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Organic Treatment
                        Text(
                            text = stringResource(R.string.diagnosis_organic),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        )
                        Text(
                            text = result.organicRemedy,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chemical Treatment
                        Text(
                            text = stringResource(R.string.diagnosis_chemical),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AmberSecondary
                            )
                        )
                        Text(
                            text = result.chemicalRemedy,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prevention
                        Text(
                            text = stringResource(R.string.diagnosis_prevention),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = result.prevention,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Provenance
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "स्रोत: ${result.source}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
