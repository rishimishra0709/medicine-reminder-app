package com.example.medreminder.ui.addmed

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medreminder.domain.model.ScheduleType
import com.example.medreminder.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddMedicationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val name by viewModel.name.collectAsState()
    val dosage by viewModel.dosage.collectAsState()
    val instructions by viewModel.instructions.collectAsState()
    val selectedShape by viewModel.selectedShape.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val scheduleType by viewModel.scheduleType.collectAsState()
    val specificDays by viewModel.specificDays.collectAsState()
    val reminderTimes by viewModel.reminderTimes.collectAsState()
    val currentStock by viewModel.currentStock.collectAsState()
    val isInventoryTrackingEnabled by viewModel.isInventoryTrackingEnabled.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.saveSuccess.collectLatest { success ->
            if (success) {
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SmallTopAppBar(
                title = { Text("Add Medication", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            item {
                CardSection(title = "Medication Info") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = viewModel::setName,
                        label = { Text("Medicine Name") },
                        placeholder = { Text("e.g. Paracetamol, Lisinopril") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = viewModel::setDosage,
                            label = { Text("Dosage") },
                            placeholder = { Text("e.g. 500mg, 1 tablet") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = instructions,
                            onValueChange = viewModel::setInstructions,
                            label = { Text("Instructions") },
                            placeholder = { Text("e.g. After food") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Pill Visual Designer
            item {
                CardSection(title = "Appearance & Visuals") {
                    Text(
                        "Pill Shape",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("capsule", "tablet", "circle").forEach { shape ->
                            val isSelected = selectedShape == shape
                            val color = Color(selectedColor)
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { viewModel.setSelectedShape(shape) }
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 2.dp else 0.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(
                                            when (shape) {
                                                "capsule" -> RoundedCornerShape(18.dp)
                                                "tablet" -> RoundedCornerShape(6.dp)
                                                else -> CircleShape
                                            }
                                        )
                                        .background(color)
                                ) {
                                    Icon(
                                        imageVector = when (shape) {
                                            "capsule" -> Icons.Default.Share
                                            "tablet" -> Icons.Default.Menu
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = shape,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Pill Color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            0xFF80CBC4, // PillMint
                            0xFFB39DDB, // PillLavender
                            0xFF90CAF9, // PillBlue
                            0xFFF48FB1, // PillPink
                            0xFFFFCC80  // PillOrange
                        ).forEach { colorHex ->
                            val isSelected = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 3.dp else 0.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        CircleShape
                                    )
                                    .clickable { viewModel.setSelectedColor(colorHex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scheduling
            item {
                CardSection(title = "Schedule Frequency") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Pair("Daily", ScheduleType.DAILY),
                            Pair("Specific Days", ScheduleType.SPECIFIC_DAYS),
                            Pair("As Needed", ScheduleType.AS_NEEDED)
                        ).forEach { (label, type) ->
                            val isSelected = scheduleType == type
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { viewModel.setScheduleType(type) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Expandable Custom Day Selectors
                    AnimatedVisibility(
                        visible = scheduleType == ScheduleType.SPECIFIC_DAYS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Select Days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    Pair("M", 1), Pair("T", 2), Pair("W", 3),
                                    Pair("T", 4), Pair("F", 5), Pair("S", 6), Pair("S", 7)
                                ).forEach { (label, day) ->
                                    val isSelected = specificDays.contains(day)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { viewModel.toggleSpecificDay(day) }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Alarms / Reminder Times Selectors
                    AnimatedVisibility(
                        visible = scheduleType != ScheduleType.AS_NEEDED,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Reminder Alarms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = {
                                    val now = LocalTime.now()
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val formatted = String.format("%02d:%02d", hourOfDay, minute)
                                            viewModel.addReminderTime(formatted)
                                        },
                                        now.hour,
                                        now.minute,
                                        true
                                    ).show()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Time", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Time")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                mainAxisSpacing = 8.dp,
                                crossAxisSpacing = 8.dp
                            ) {
                                reminderTimes.forEach { time ->
                                    ChipTime(
                                        time = time,
                                        onDelete = { viewModel.removeReminderTime(time) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inventory Tracking Card
            item {
                CardSection(title = "Inventory & Stocks") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Enable Inventory Alerts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Deduct stock automatically when taken",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isInventoryTrackingEnabled,
                            onCheckedChange = viewModel::toggleInventoryTracking
                        )
                    }

                    AnimatedVisibility(
                        visible = isInventoryTrackingEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = currentStock,
                                onValueChange = viewModel::setCurrentStock,
                                label = { Text("Current Pill Count") },
                                placeholder = { Text("e.g. 50, 100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Notes Section
            item {
                CardSection(title = "Notes & Doctor Instructions") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = viewModel::setNotes,
                        placeholder = { Text("Write additional notes here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save Button
            item {
                Button(
                    onClick = viewModel::saveMedication,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Save Medication Schedule",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CardSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun ChipTime(
    time: String,
    onDelete: () -> Unit
) {
    val displayStr = LocalTime.parse(time).format(DateTimeFormatter.ofPattern("hh:mm a"))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

// FlowRow is part of compose standard now or we can implement a custom layout or wrap in standard flow
// Let's write a simple FlexRow layout for simple times grid to stay 100% robust.
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        
        var currentX = 0
        var currentY = 0
        var maxRowHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()
        
        placeables.forEach { placeable ->
            if (currentX + placeable.width > layoutWidth) {
                currentX = 0
                currentY += maxRowHeight + crossAxisSpacing.roundToPx()
                maxRowHeight = 0
            }
            positions.add(Pair(currentX, currentY))
            currentX += placeable.width + mainAxisSpacing.roundToPx()
            maxRowHeight = maxOf(maxRowHeight, placeable.height)
        }
        
        val finalHeight = currentY + maxRowHeight
        layout(layoutWidth, finalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.place(x, y)
            }
        }
    }
}
