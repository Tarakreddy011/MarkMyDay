package com.project.markmyday.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.data.model.Question
import com.project.markmyday.viewmodel.QuizViewModel
import com.project.markmyday.viewmodel.UploadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizQuestionUploadScreen(
    onBack: () -> Unit,
) {
    val viewModel: QuizViewModel = viewModel()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val availableTests by viewModel.availableTests.collectAsState()
    val bankQuestions by viewModel.bankQuestions.collectAsState()
    val context = LocalContext.current

    // Manual Entry State
    var targetQuestionCount by remember { mutableIntStateOf(1) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val draftedQuestions = remember { mutableStateListOf<Question>() }

    var subject by remember { mutableStateOf("") }
    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    
    val options = listOf("Option A", "Option B", "Option C", "Option D")
    var selectedCorrectOption by remember { mutableStateOf(options[0]) }
    var expanded by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var viewingTest by remember { mutableStateOf<Pair<String, String>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadCsv(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchAvailableTests()
    }

    LaunchedEffect(viewingTest) {
        viewingTest?.let {
            viewModel.fetchQuestionsFromBank(it.first, it.second)
        }
    }

    // Handle Toast messages
    LaunchedEffect(uploadStatus) {
        when (uploadStatus) {
            is UploadStatus.Success -> {
                Toast.makeText(context, "Quiz saved successfully bank! ✨", Toast.LENGTH_SHORT).show()
                // Reset everything
                questionText = ""; optA = ""; optB = ""; optC = ""; optD = ""; subject = ""
                draftedQuestions.clear()
                currentQuestionIndex = 0
                viewModel.resetStatus()
            }
            is UploadStatus.Error -> {
                Toast.makeText(context, "Error: ${(uploadStatus as UploadStatus.Error).message}", Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    if (viewingTest != null) {
        ViewQuestionsDialog(
            subject = viewingTest?.first ?: "",
            className = viewingTest?.second ?: "",
            questions = bankQuestions,
            onDismiss = { viewingTest = null }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Test") },
            text = { Text("Delete all questions for ${showDeleteConfirm?.first} (${showDeleteConfirm?.second})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm?.let { viewModel.deleteTest(it.first, it.second) }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Quiz", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. CSV Upload Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { launcher.launch("*/*") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Upload CSV/Excel File", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Format: Subject | Class | Question | Options | Correct", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // 2. Manual Entry Section
            item {
                Text("Manual Entry 📝", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Question Counter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Questions:", fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (targetQuestionCount > 1) targetQuestionCount-- }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text(targetQuestionCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (targetQuestionCount < 30) targetQuestionCount++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / targetQuestionCount },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text("Drafting Question ${currentQuestionIndex + 1} of $targetQuestionCount", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (e.g. GK)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = currentQuestionIndex == 0 // Fix subject on first question
                    )

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question Text") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCorrectOption,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Correct Option") },
                            trailingIcon = { 
                                IconButton(onClick = { expanded = true }) {
                                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                            shape = RoundedCornerShape(16.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedCorrectOption = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (questionText.isNotEmpty() && subject.isNotEmpty() && optA.isNotEmpty() && optB.isNotEmpty()) {
                                val correctValue = when(selectedCorrectOption) {
                                    "Option A" -> optA
                                    "Option B" -> optB
                                    "Option C" -> optC
                                    else -> optD
                                }
                                
                                val newQuestion = Question(
                                    text = questionText,
                                    options = listOf(optA, optB, optC, optD),
                                    correctAnswer = correctValue,
                                    subject = subject,
                                    className = "ALL"
                                )

                                if ((currentQuestionIndex < targetQuestionCount - 1)) {
                                    draftedQuestions.add(newQuestion)
                                    // Move to next
                                    currentQuestionIndex++
                                    questionText = ""; optA = ""; optB = ""; optC = ""; optD = ""
                                } else {
                                    // Last question, submit all
                                    draftedQuestions.add(newQuestion)
                                    viewModel.saveManualQuestions(draftedQuestions.toList())
                                }
                            } else {
                                Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uploadStatus !is UploadStatus.Loading
                    ) {
                        if (uploadStatus is UploadStatus.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            val btnText = if (currentQuestionIndex < targetQuestionCount - 1) "Next Question ➡️" else "Save Quiz to Bank ✨"
                            Text(btnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Manage Existing Tests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            if (availableTests.isEmpty()) {
                item {
                    Text("No tests found in bank.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                items(availableTests) { test ->
                    TestEntryItem(
                        subject = test.first,
                        className = test.second,
                        onView = { viewingTest = test },
                        onDelete = { showDeleteConfirm = test }
                    )
                }
            }
        }
    }
}

@Composable
fun TestEntryItem(subject: String, className: String, onView: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(className, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewQuestionsDialog(
    subject: String,
    className: String,
    questions: List<Question>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Questions for $subject", style = MaterialTheme.typography.titleMedium)
                            Text(className, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            if (questions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F5F9)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions) { question ->
                        QuestionViewCard(question)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionViewCard(question: Question) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            question.options.forEachIndexed { index, option ->
                val isCorrect = option == question.correctAnswer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isCorrect) Color(0xFFE8F5E9) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isCorrect) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${'A' + index}. $option",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCorrect) {
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
