package com.project.markmyday.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.Fill
import com.project.markmyday.viewmodel.StudentAttendanceDashboardViewModel
import com.project.markmyday.viewmodel.StudentAttendanceState
import java.time.Month
import java.time.format.TextStyle as DateTextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceDashboardScreen(
    studentId: String,
    onBack: () -> Unit,
    viewModel: StudentAttendanceDashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.fetchAttendanceData(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is StudentAttendanceState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StudentAttendanceState.Success -> {
                    val successState = uiState as StudentAttendanceState.Success
                    AttendanceContent(
                        monthlySummary = successState.monthlySummary,
                        yearlyData = successState.yearlyData
                    )
                }
                is StudentAttendanceState.Error -> {
                    Text(
                        text = (uiState as StudentAttendanceState.Error).message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceContent(
    monthlySummary: com.project.markmyday.viewmodel.AttendanceSummary,
    yearlyData: List<com.project.markmyday.viewmodel.MonthlyAttendance>
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Card
        MonthlySummaryCard(monthlySummary)

        // Graph Card
        YearlyGraphCard(yearlyData)
    }
}

@Composable
fun MonthlySummaryCard(summary: com.project.markmyday.viewmodel.AttendanceSummary) {
    val currentMonthName = Month.of(Calendar.getInstance()[Calendar.MONTH] + 1)
        .getDisplayName(DateTextStyle.FULL, Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Summary for $currentMonthName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (summary.presentDays == 0 && summary.absentDays == 0) {
                Text(
                    text = "No records found for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Present", value = summary.presentDays, color = Color(0xFF4CAF50))
                    StatItem(label = "Absent", value = summary.absentDays, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun YearlyGraphCard(yearlyData: List<com.project.markmyday.viewmodel.MonthlyAttendance>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(yearlyData) {
        modelProducer.runTransaction {
            columnSeries {
                series(yearlyData.map { it.presentDays.toFloat() })
                series(yearlyData.map { it.absentDays.toFloat() })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Yearly Attendance Log",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp)) {
                    Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxSize()) {}
                }
                Text(" Present", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(12.dp)) {
                    Surface(color = Color(0xFFE57373), shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxSize()) {}
                }
                Text(" Absent", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(24.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(fill = Fill(Color(0xFF4CAF50)), thickness = 8.dp),
                            rememberLineComponent(fill = Fill(Color(0xFFE57373)), thickness = 8.dp)
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        label = rememberTextComponent(
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { _, value, _ ->
                            try {
                                val monthIndex = value.toInt() + 1
                                if (monthIndex in 1..12) {
                                    Month.of(monthIndex).getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
                                } else ""
                            } catch (_: Exception) {
                                ""
                            }
                        },
                        label = rememberTextComponent(
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    }
}
