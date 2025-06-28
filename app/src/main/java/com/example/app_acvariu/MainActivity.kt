package com.example.app_acvariu

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_acvariu.ui.components.FishAnimation
import com.example.app_acvariu.ui.components.LoadingFishAnimation
import com.example.app_acvariu.ui.components.FeedingFishAnimation
import com.example.app_acvariu.ui.theme.App_acvariuTheme
import com.example.app_acvariu.viewmodel.AquariumViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_acvariuTheme {
                MainScreen()
            }
        }
    }
}

private val LightBlue = Color(0xFFE0F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: AquariumViewModel = viewModel()
    val status by viewModel.status.collectAsState()


    var showTdsDialog by remember { mutableStateOf(false) }
    val tdsValue = status?.tds
        ?.removeSuffix("ppm")
        ?.toIntOrNull() ?: 0
    val TDS_THRESHOLD = 500


    var showFeedComplete by remember { mutableStateOf(false) }
    var lastFeedTime by remember { mutableStateOf("") }


    var showFoodEmptyDialog  by remember { mutableStateOf(false) }
    var showDrainFullDialog  by remember { mutableStateOf(false) }
    var showFillEmptyDialog  by remember { mutableStateOf(false) }


    var extractionOn by remember { mutableStateOf(false) }
    var addingOn     by remember { mutableStateOf(false) }
    var fishCount    by remember { mutableStateOf(1) }
    var scheduledTime by remember { mutableStateOf("") }

    var motorOn by remember { mutableStateOf(status?.motor == "ON") }


    // 1) Fetch every 10s
    LaunchedEffect(Unit) {
        viewModel.fetchAquariumStatus()
        while(true) {
            delay(60_000)
            viewModel.fetchAquariumStatus()
        }
    }

// Only fire when the Arduino actually sent ALERT:TDS_HIGH
    LaunchedEffect(status?.alertTdsHigh) {
        if (status?.alertTdsHigh == true) {
            showTdsDialog = true
        }
    }

    // 3) Watch for empty/full/food alerts in status
    LaunchedEffect(status) {
        if (status?.foodEmpty == true)  showFoodEmptyDialog = true
        if (status?.drainEmpty == true) showDrainFullDialog = true
        if (status?.fillFull   == true) showFillEmptyDialog = true
    }

    // 4) Feed‐complete overlay when time matches
    LaunchedEffect(status, scheduledTime) {
        val now = status?.time
        if (scheduledTime.isNotEmpty() && now == scheduledTime && now != lastFeedTime) {
            showFeedComplete = true
            fishCount        = 1
            lastFeedTime     = now
            launch {
                delay(5_000)
                showFeedComplete = false
            }
        }
    }

    // whenever a fresh status arrives, re‐sync:
    LaunchedEffect(status?.motor) {
        motorOn = (status?.motor == "ON")
    }

    Box(Modifier.fillMaxSize()) {
        // ─── Main UI ───────────────────────
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Monitor Acvariu", fontWeight = FontWeight.Bold) })
            },
            containerColor = LightBlue
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(LightBlue)
            ) {
                // Header animation
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    FishAnimation(modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))

                // Status card
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Status Acvariu", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (status == null) {
                            LoadingFishAnimation(modifier = Modifier.size(80.dp))
                        } else {
                            Text("Temperatura: ${status?.temperature}", fontSize = 16.sp)
                            Text("Nivel Apa: ${status?.waterLevel}", fontSize = 16.sp)
                            Text("Timp: ${status?.time}", fontSize = 16.sp)
                            Text("Lumina: ${status?.light}", fontSize = 16.sp)
                        }

                        // Feeding picker
                        FeedingTimePicker(
                            scheduledTime = scheduledTime,
                            onTimeSelected = { scheduledTime = it },
                            viewModel = viewModel
                        )

                        // Pumps row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    extractionOn = !extractionOn
                                    viewModel.updateExtractionPumpState(extractionOn)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (extractionOn) "Stop Extractie" else "Extractie Apa")
                            }
                            OutlinedButton(
                                onClick = {
                                    addingOn = !addingOn
                                    viewModel.updateAddingPumpState(addingOn)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (addingOn) "Stop Adaugare" else "Adaugare Apa")
                            }
                        }

                        // Light controls
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateLightState(true) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Lumina On") }
                            OutlinedButton(
                                onClick = { viewModel.updateLightState(false) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Lumina Off") }
                        }

                        // Fish count controls
                        Text("Număr Pești: $fishCount", fontSize = 16.sp)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = {
                                if (fishCount > 1) {
                                    fishCount--
                                    viewModel.updateFishCount(fishCount)
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text("–")
                            }
                            OutlinedButton(onClick = {
                                fishCount++
                                viewModel.updateFishCount(fishCount)
                            }, modifier = Modifier.weight(1f)) {
                                Text("+")
                            }
                        }

                        // show current motor state
                        Text("Motor: ${status?.motor ?: "OFF"}", fontSize = 16.sp)
                        // motor control buttons
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    motorOn = true
                                    viewModel.updateMotorState(true)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Start Motor") }

                            OutlinedButton(
                                onClick = {
                                    motorOn = false
                                    viewModel.updateMotorState(false)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Stop Motor") }
                        }


                    }
                }
            }
        }

        // ─── Feed complete overlay ─────────────────────
        if (showFeedComplete) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeedingFishAnimation(modifier = Modifier.size(180.dp))
                    Text("Peștii au fost hrăniți!", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showFeedComplete = false }) {
                        Text("OK")
                    }
                }
            }
        }

        // ─── Food-empty dialog ─────────────────────────
        if (showFoodEmptyDialog) {
            AlertDialog(
                onDismissRequest = {},
                title            = { Text("Dozator gol") },
                text             = { Text("Recipientul de hrană este gol. L-ai reumplut?") },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.resetFoodCounter()
                        showFoodEmptyDialog = false
                    }) { Text("Da, umplut") }
                }
            )
        }

        // ─── Drain-full dialog ─────────────────────────
        if (showDrainFullDialog) {
            viewModel.updateExtractionPumpState(false)
            extractionOn = false
            AlertDialog(
                onDismissRequest = {},
                title            = { Text("Container drenare plin") },
                text             = { Text("Recipientul de drenare este plin. L-ai golit?") },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.resetDrainTimer()
                        showDrainFullDialog = false
                    }) { Text("Da, golit") }
                }
            )
        }

        // ─── Fill-empty dialog ─────────────────────────
        if (showFillEmptyDialog) {
            viewModel.updateAddingPumpState(false)
            addingOn = false
            AlertDialog(
                onDismissRequest = {},
                title            = { Text("Container umplere gol") },
                text             = { Text("Recipientul de umplere este gol. L-ai schimbat?") },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.resetFillTimer()
                        showFillEmptyDialog = false
                    }) { Text("Da, schimbat") }
                }
            )
        }

        // ─── TDS-high alert ────────────────────────────
        if (showTdsDialog) {
            AlertDialog(
                onDismissRequest = {},
                title            = { Text("Alertă TDS ridicat") },
                text             = { Text("TDS: $tdsValue ppm.\nAlege modul de schimbare a apei:") },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.setAutoMode()   // sends "!MODE:A!"
                        showTdsDialog = false
                    }) { Text("Auto") }
                },
                dismissButton    = {
                    TextButton(onClick = {
                        viewModel.setManualMode() // sends "!MODE:M!"
                        showTdsDialog = false
                    }) { Text("Manual") }
                }
            )
        }
    }
}

@Composable
fun FeedingTimePicker(
    scheduledTime: String,
    onTimeSelected: (String) -> Unit,
    viewModel: AquariumViewModel
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(scheduledTime) }
    val parts = selected.split(":").mapNotNull { it.toIntOrNull() }
    val h = parts.getOrNull(0) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val m = parts.getOrNull(1) ?: Calendar.getInstance().get(Calendar.MINUTE)

    val dlg = TimePickerDialog(context, { _, hh, mm ->
        val n = "%02d:%02d".format(hh, mm)
        selected = n
        onTimeSelected(n)
        viewModel.updateFeedingTime(hh, mm)
    }, h, m, true)

    OutlinedButton(onClick = { dlg.show() }, Modifier.fillMaxWidth()) {
        Text(if (selected.isEmpty()) "Setează timp hranire" else "Timp: $selected")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    App_acvariuTheme { MainScreen() }
}
