package com.krishimitra.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.model.Loan
import com.krishimitra.app.ui.theme.*

@Composable
fun LoansScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val allLoans = remember { dbHelper.getAllLoans() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Notice banner (no applications submitted, strictly official advisory)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "यह अनुभाग केवल प्रमाणित ब्याज दरों एवं पात्रता की जानकारी देता है। आवेदन सीधे अपनी निकटतम बैंक शाखा अथवा आधिकारिक पोर्टल पर करें।",
                        color = GreenDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        items(allLoans) { loan ->
            LoanCard(loan = loan, onOpenLink = {
                loan.officialUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            })
        }
    }
}

@Composable
fun LoanCard(loan: Loan, onOpenLink: () -> Unit) {
    val isHindi = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "hi"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Bank Name
            Text(
                text = if (isHindi) loan.bankNameHi else loan.bankName,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            // Loan Title
            Text(
                text = if (isHindi) loan.loanTypeHi else loan.loanType,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFF0F0F0), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Interest Rate & Max Limit Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FBF8))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = if (isHindi) "ब्याज दर:" else "Interest Rate:", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = if (isHindi) (loan.interestRateHi ?: "रियायती") else (loan.interestRate ?: "Concessional"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (isHindi) "अधिकतम सीमा:" else "Max Limit:", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = if (isHindi) (loan.maxLimitHi ?: "") else (loan.maxLimit ?: ""),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Purpose
            Text(
                text = if (isHindi) "ऋण का उद्देश्य:" else "Loan Purpose:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (isHindi) (loan.purposeHi ?: "") else (loan.purposeEn ?: ""),
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Documents
            Text(
                text = if (isHindi) "आवश्यक दस्तावेज:" else "Documents Required:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (isHindi) (loan.documentsRequiredHi ?: "") else (loan.documentsRequired ?: ""),
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenLink,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi) "बैंक पोर्टल पर विस्तृत विवरण देखें" else "View Details on Bank Portal",
                    fontSize = 12.sp
                )
            }
        }
    }
}
