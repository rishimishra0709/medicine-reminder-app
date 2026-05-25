package com.example.medreminder.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.ui.common.ConfettiEffect
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToAddMedication: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val doses by viewModel.dashboardDoses.collectAsState()

    var triggerConfetti by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with App Title
            SmallTopAppBar(
                title = {
                    Text(
                        text = "DoseMind",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
            )

            // Horizontal Week Calendar Selector
            WeekCalendarView(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.changeDate(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gamified Streak Banner
                item {
                    StreakBanner(streakCount = streak)
                }

                // Empty State
                if (doses.isEmpty()) {
                    item {
                        EmptyStateView()
                    }
                } else {
                    // Grouped Dose Sections
                    val grouped = doses.groupBy { getPeriodOfDay(it.scheduledTime) }
                    
                    grouped.forEach { (period, periodDoses) ->
                        item {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(periodDoses, key = { "${it.medicationId}_${it.scheduledTime}" }) { dose ->
                            DoseItemCard(
                                dose = dose,
                                onTake = {
                                    viewModel.markAsTaken(dose.medicationId, dose.scheduledTime)
                                    triggerConfetti = true
                                },
                                onSnooze = {
                                    viewModel.markAsSnoozed(dose.medicationId, dose.scheduledTime)
                                },
                                onLogAsNeeded = {
                                    viewModel.logAsNeeded(dose.medicationId)
                                    triggerConfetti = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAddMedication,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Medication",
                modifier = Modifier.size(28.dp)
            )
        }

        // Confetti Celebration
        ConfettiEffect(
            trigger = triggerConfetti,
            onAnimationEnd = { triggerConfetti = false }
        )
    }
}

@Composable
fun WeekCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val days = remember(today) {
        (-3..3).map { today.plusDays(it.toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dateStr = date.dayOfMonth.toString()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday && !isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streakCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = Icons.Default.Warning, // Fire icon placeholder
                    contentDescription = "Streak Fire",
                    tint = Color(0xFFFF7043),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "$streakCount Day Adherence Streak!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (streakCount > 0) "Excellent tracking! You\'re building a healthy habit."
                    else "Take your medications today to start a new streak!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DoseItemCard(
    dose: DashboardDose,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onLogAsNeeded: () -> Unit
) {
    val containerColor = when (dose.status) {
        DoseStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        DoseStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        DoseStatus.SNOOZED -> Color(0xFFFFF9C4).copy(alpha = 0.5f)
        DoseStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val nameDecoration = if (dose.status == DoseStatus.TAKEN) TextDecoration.LineThrough else TextDecoration.None
    val titleColor = if (dose.status == DoseStatus.TAKEN) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (dose.status == DoseStatus.PENDING) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) 
            else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pill Shape Icon Visual
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(
                            when (dose.iconShape) {
                                "capsule" -> RoundedCornerShape(22.dp)
                                "tablet" -> RoundedCornerShape(8.dp)
                                else -> CircleShape
                            }
                        )
                        .background(Color(dose.iconColor))
                ) {
                    Icon(
                        imageVector = when (dose.iconShape) {
                            "capsule" -> Icons.Default.Share // capsule look-alike
                            "tablet" -> Icons.Default.Menu
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Pill type",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dose.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = nameDecoration,
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dose.dosage} • ${dose.instructions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right-aligned Info: Time or Status Icon
                Column(horizontalAlignment = Alignment.End) {
                    if (dose.isAsNeeded) {
                        Text(
                            text = "As Needed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = dose.scheduledTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = dose.status)
                }
            }

            // Inventory warning banner
            if (dose.isInventoryTrackingEnabled && dose.isStockLow) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Low Stock Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Low Stock: ${dose.currentStock} left (approx ${dose.remainingDays} days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action buttons if pending
            if (dose.status == DoseStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (dose.isAsNeeded && dose.scheduledTime.isEmpty()) {
                        Button(
                            onClick = onLogAsNeeded,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Dose", fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSnooze,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Snooze", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onTake,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Take", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DoseStatus) {
    val pair = when (status) {
        DoseStatus.TAKEN -> Pair("Taken", MaterialTheme.colorScheme.primary)
        DoseStatus.MISSED -> Pair("Missed", MaterialTheme.colorScheme.error)
        DoseStatus.SNOOZED -> Pair("Snoozed", Color(0xFFF57F17))
        DoseStatus.PENDING -> Pair("Pending", MaterialTheme.colorScheme.secondary)
    }

    Text(
        text = pair.first,
        color = pair.second,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(pair.second.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "No Medication",
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No medications for this day",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Use the + button to add new reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

private fun getPeriodOfDay(timeStr: String): String {
    if (timeStr.isEmpty()) return "As Needed"
    val hour = timeStr.split(":")[0].toIntOrNull() ?: 12
    return when (hour) {
        in 0..11 -> "Morning 🌅"
        in 12..16 -> "Afternoon ☀️"
        in 17..20 -> "Evening 🌆"
        else -> "Night 🌙"
    }
}
