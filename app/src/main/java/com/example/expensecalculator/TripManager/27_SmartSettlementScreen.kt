package com.example.expensecalculator.TripManager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensecalculator.ui.theme.IconBackground
import kotlin.math.abs

@Composable
fun SmartSettlementContent(
    balances: Map<String, Double>,
    settlements: List<Settlement>,
    currencySymbol: String = "₹",
    onRecordPayment: (String, String, Double) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isAllSettled = balances.values.all { abs(it) < 0.01 }
    var expandedSettlementId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            SettlementHeaderCard(
                isAllSettled = isAllSettled,
                totalSettlements = settlements.size
            )
        }

        // Settlement Instructions
        if (settlements.isNotEmpty()) {
            item {
                Text(
                    "Settlement Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    "Optimized to minimize transactions • ${settlements.size} payment${if (settlements.size > 1) "s" else ""} needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Settlement Cards
            items(settlements.size) { index ->
                val settlement = settlements[index]
                SettlementCard(
                    settlement = settlement,
                    settlementNumber = index + 1,
                    isExpanded = expandedSettlementId == index,
                    currencySymbol = currencySymbol,
                    onExpandToggle = {
                        expandedSettlementId = if (expandedSettlementId == index) null else index
                    },
                    onCopyClick = {
                        copyToClipboard(context, settlement, currencySymbol)
                    },
                    onShareClick = {
                        shareSettlement(context, settlement, currencySymbol)
                    },
                    onMarkPaid = {
                        onRecordPayment(settlement.from, settlement.to, settlement.amount)
                        Toast.makeText(context, "Payment recorded", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Summary Card
            item {
                SettlementSummaryCard(
                    totalTransactions = settlements.size,
                    totalAmount = settlements.sumOf { it.amount },
                    currencySymbol = currencySymbol
                )
            }
        } else {
            // All Settled State
            item {
                AllSettledEmptyState()
            }
        }

        // Individual Balances Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Individual Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (balances.isEmpty()) {
            item {
                Text(
                    "No participants to calculate balances.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(balances.entries.toList()) { (name, balance) ->
                IndividualBalanceItem(
                    name = name,
                    balance = balance,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

@Composable
private fun SettlementHeaderCard(
    isAllSettled: Boolean,
    totalSettlements: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isAllSettled)
                Color.White
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAllSettled) "All Settled" else "Pending Settlements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isAllSettled)
                        "No Payments Needed"
                    else
                        "$totalSettlements payment${if (totalSettlements > 1) "s" else ""} needed",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettlementCard(
    settlement: Settlement,
    settlementNumber: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onMarkPaid: () -> Unit = {},
    currencySymbol: String // Add currency symbol parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Settlement Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settlement Number Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$settlementNumber",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = settlement.from,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "pays",
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = settlement.to,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Tap to see options",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "$currencySymbol${"%.2f".format(settlement.amount)}", // Use currency symbol
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Expanded Options
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Actions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.Check,
                            label = "Mark Paid",
                            onClick = onMarkPaid,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            onClick = onCopyClick,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = onShareClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = IconBackground,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettlementSummaryCard(
    totalTransactions: Int,
    totalAmount: Double,
    currencySymbol: String // Add currency symbol parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = IconBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Total Settlements",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "$totalTransactions transaction${if (totalTransactions > 1) "s" else ""}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Total Amount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "$currencySymbol${"%.2f".format(totalAmount)}", // Use currency symbol
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AllSettledEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "All Settled",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Everyone has been paid back.\nNo settlements needed",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IndividualBalanceItem(name: String, balance: Double, currencySymbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(41.dp)
                .clip(CircleShape)
                .background(IconBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.toString()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(17.dp))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        val (displayText, textColor) = when {
            balance > 0.01 -> "Gets back $currencySymbol${"%.2f".format(abs(balance))}" to MaterialTheme.colorScheme.primary
            balance < -0.01 -> "Owes $currencySymbol${"%.2f".format(abs(balance))}" to MaterialTheme.colorScheme.error
            else -> "Settled" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }

        Text(
            text = displayText,
            fontWeight = if (abs(balance) > 0.01) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            fontSize = 13.sp
        )
    }
}

// Helper functions for actions
private fun copyToClipboard(context: Context, settlement: Settlement, currencySymbol: String) {

    val text = "${settlement.from} needs to pay ${settlement.to} $currencySymbol${"%.2f".format(settlement.amount)}"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Settlement", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareSettlement(context: Context, settlement: Settlement, currencySymbol: String) {
    val text = """
        Trip Settlement Reminder
        
        ${settlement.from} needs to pay ${settlement.to}
        Amount: $currencySymbol${"%.2f".format(settlement.amount)}
        
        Please settle this amount via UPI, cash, or bank transfer.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Trip Settlement Reminder")
    }
    context.startActivity(Intent.createChooser(intent, "Share Settlement Reminder"))
}
