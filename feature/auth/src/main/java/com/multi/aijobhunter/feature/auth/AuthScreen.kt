package com.multi.aijobhunter.feature.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.multi.aijobhunter.feature.shared_ui.MutedText
import com.multi.aijobhunter.feature.shared_ui.NeonGreen
import com.multi.aijobhunter.feature.shared_ui.NeonRed
import com.multi.aijobhunter.feature.shared_ui.PureBlack
import com.multi.aijobhunter.feature.shared_ui.PureWhite
import com.multi.aijobhunter.feature.shared_ui.TerminalDarkGray
import com.multi.aijobhunter.feature.shared_ui.TerminalMediumGray
import com.multi.aijobhunter.feature.shared_ui.NeonAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    val logsListState = rememberLazyListState()
    val scrollState = rememberScrollState()

    var resumeText by remember { mutableStateOf("") }
    var expandedPreset by remember { mutableStateOf(false) }

    val urlPresets = listOf(
        "https://api.openai.com/" to "OpenAI (Official)",
        "https://api.deepseek.com/" to "DeepSeek API",
        "https://generativelanguage.googleapis.com/v1beta/" to "Gemini API (Free Tier)",
        "https://openrouter.ai/api/v1/" to "OpenRouter (Free Models)",
        "http://10.0.2.2:11434/" to "Ollama (Local Free)",
        "custom" to "Custom Endpoint..."
    )

    var selectedPreset by remember(uiState.baseUrl) {
        mutableStateOf(urlPresets.find { it.first == uiState.baseUrl } ?: urlPresets.last())
    }

    val presetModels = mapOf(
        "https://api.openai.com/" to listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"),
        "https://api.deepseek.com/" to listOf("deepseek-chat", "deepseek-reasoner"),
        "https://generativelanguage.googleapis.com/v1beta/" to listOf("gemini-1.5-flash", "gemini-1.5-pro"),
        "https://openrouter.ai/api/v1/" to listOf("google/gemma-2-9b-it:free", "meta-llama/llama-3-8b-instruct:free", "mistralai/mistral-7b-instruct:free"),
        "http://10.0.2.2:11434/" to listOf("gemma:2b", "llama3", "mistral", "gemma"),
        "custom" to listOf("gpt-4o-mini", "deepseek-chat", "gemini-1.5-flash")
    )

    var expandedModel by remember { mutableStateOf(false) }
    var customModelSelected by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var selectedPdfSize by remember { mutableStateOf<String?>(null) }
    var cvInputType by remember { mutableStateOf("text") } // "text" or "pdf"

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    PDFBoxResourceLoader.init(context)
                    val (name, size) = getFileInfo(context, uri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()
                        val text = stripper.getText(document)
                        document.close()
                        
                        withContext(Dispatchers.Main) {
                            selectedPdfName = name
                            selectedPdfSize = size
                            if (text.isNotBlank()) {
                                resumeText = text
                                Toast.makeText(context, "Successfully loaded text from $name", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not extract text from PDF (it might be empty or scanned)", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to parse PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthSideEffect.NavigateToFeed -> onNavigateToFeed()
                is AuthSideEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Auto-scroll logs
    LaunchedEffect(uiState.parsingLogs.size) {
        if (uiState.parsingLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(uiState.parsingLogs.size - 1)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0E15), Color(0xFF161A26))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Fields Section
            Column {
                // Header Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235).copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFF2E3550)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI CAREER SCOUT AGENT",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set up your LLM credentials and coordinates to start autonomous career tracking.",
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // STEP 1 CARD: Credentials Settings
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171A26).copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, Color(0xFF252B3D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔑 1. API SERVICE SETTINGS",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val isFreeOrLocalPreset = uiState.baseUrl.contains("googleapis.com") || 
                                                 uiState.baseUrl.contains("openrouter.ai") || 
                                                 uiState.baseUrl.contains("10.0.2.2") || 
                                                 uiState.baseUrl.contains("11434")
                        if (isFreeOrLocalPreset) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NeonAmber.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, NeonAmber),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "⚠️ Free/Local API detected. Background scans will be limited to 5 vacancies per cycle with a 3.5s delay to avoid HTTP 429 blocks.",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // API TOKEN Field
                        Text(
                            text = "LLM API TOKEN (Gemini/OpenAI/DeepSeek):",
                            color = MutedText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = uiState.apiKey,
                            onValueChange = { viewModel.handleIntent(AuthIntent.ChangeApiKey(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            textStyle = TextStyle(color = PureWhite, fontSize = 13.sp),
                            placeholder = {
                                Text(
                                    "Enter sk-... token key",
                                    color = Color(0xFF4C526A),
                                    fontSize = 13.sp
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F111A),
                                unfocusedContainerColor = Color(0xFF0F111A),
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0xFF2E3550)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // BASE URL Dropdown Selector
                        Text(
                            text = "ENDPOINT URL PRESET:",
                            color = MutedText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { expandedPreset = true }
                        ) {
                            OutlinedTextField(
                                value = selectedPreset.second,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = PureWhite, fontSize = 13.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPreset) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = PureWhite,
                                    disabledContainerColor = Color(0xFF0F111A),
                                    disabledBorderColor = Color(0xFF2E3550),
                                    disabledTrailingIconColor = MutedText
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            DropdownMenu(
                                expanded = expandedPreset,
                                onDismissRequest = { expandedPreset = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141724))
                                    .border(1.dp, Color(0xFF2E3550))
                            ) {
                                urlPresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                preset.second,
                                                color = PureWhite,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedPreset = preset
                                            expandedPreset = false
                                            if (preset.first != "custom") {
                                                viewModel.handleIntent(AuthIntent.ChangeBaseUrl(preset.first))
                                                val models = presetModels[preset.first] ?: emptyList()
                                                if (models.isNotEmpty()) {
                                                    viewModel.handleIntent(AuthIntent.ChangeModelName(models.first()))
                                                    customModelSelected = false
                                                }
                                            } else {
                                                customModelSelected = true
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        // Custom URL Input if "custom" is selected
                        if (selectedPreset.first == "custom") {
                            Text(
                                text = "CUSTOM BASE COMPLETIONS URL:",
                                color = MutedText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = uiState.baseUrl,
                                onValueChange = { viewModel.handleIntent(AuthIntent.ChangeBaseUrl(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                textStyle = TextStyle(color = PureWhite, fontSize = 13.sp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F111A),
                                    unfocusedContainerColor = Color(0xFF0F111A),
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E3550)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // MODEL NAME Selector
                        Text(
                            text = "MODEL ENGINE NAME:",
                            color = MutedText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        val modelsList = presetModels[selectedPreset.first] ?: listOf("gpt-4o-mini", "deepseek-chat", "gemini-1.5-flash")
                        val showModelDropdown = !customModelSelected && selectedPreset.first != "custom"
                        
                        if (showModelDropdown) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedModel = true }
                            ) {
                                OutlinedTextField(
                                    value = uiState.modelName,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = PureWhite, fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = PureWhite,
                                        disabledContainerColor = Color(0xFF0F111A),
                                        disabledBorderColor = Color(0xFF2E3550),
                                        disabledTrailingIconColor = MutedText
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                DropdownMenu(
                                    expanded = expandedModel,
                                    onDismissRequest = { expandedModel = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141724))
                                        .border(1.dp, Color(0xFF2E3550))
                                ) {
                                    modelsList.forEach { model ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    model,
                                                    color = PureWhite,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                viewModel.handleIntent(AuthIntent.ChangeModelName(model))
                                                expandedModel = false
                                            },
                                        )
                                    }
                                    
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Custom Model Name...",
                                                color = NeonGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            customModelSelected = true
                                            expandedModel = false
                                        }
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.modelName,
                                onValueChange = { viewModel.handleIntent(AuthIntent.ChangeModelName(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = PureWhite, fontSize = 13.sp),
                                singleLine = true,
                                trailingIcon = {
                                    if (selectedPreset.first != "custom") {
                                        Text(
                                            text = "[LIST]",
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { customModelSelected = false }
                                                .padding(8.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F111A),
                                    unfocusedContainerColor = Color(0xFF0F111A),
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E3550)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // STEP 2 CARD: Candidate Resume Coordinates
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171A26).copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, Color(0xFF252B3D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📄 2. CANDIDATE RESUME SOURCE",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (!uiState.isParsingCV && uiState.parsingLogs.isEmpty()) {
                            // Input Method Tab Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F111A), RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (cvInputType == "text") Color(0xFF282E44) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { cvInputType = "text" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "TEXT RESUME",
                                        color = if (cvInputType == "text") PureWhite else MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (cvInputType == "pdf") Color(0xFF282E44) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { cvInputType = "pdf" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PDF UPLOAD",
                                        color = if (cvInputType == "pdf") PureWhite else MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (cvInputType == "text") {
                                // Paste CV Text
                                Text(
                                    text = "PASTE EXPERIENCE SUMMARY OR CV:",
                                    color = MutedText,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = resumeText,
                                    onValueChange = { resumeText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    textStyle = TextStyle(color = PureWhite, fontSize = 12.sp),
                                    placeholder = {
                                        Text(
                                            "Example: Senior Android Dev, 6 years experience, Kotlin, Compose, Hilt. Target: Remote / Moscow.",
                                            color = Color(0xFF4C526A),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F111A),
                                        unfocusedContainerColor = Color(0xFF0F111A),
                                        focusedBorderColor = NeonGreen,
                                        unfocusedBorderColor = Color(0xFF2E3550)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            } else {
                                // PDF upload zone style
                                Card(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F111A)),
                                    border = BorderStroke(1.dp, Color(0xFF2E3550)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (selectedPdfName != null) "📄" else "📤",
                                                fontSize = 28.sp,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            if (selectedPdfName != null) {
                                                Text(
                                                    text = selectedPdfName!!,
                                                    color = PureWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                                Text(
                                                    text = selectedPdfSize ?: "",
                                                    color = NeonGreen,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "TAP TO SELECT PDF CV/RESUME",
                                                    color = PureWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Client-side local parsing (PDF formats)",
                                                    color = MutedText,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Agent analysis logs window
                            Text(
                                text = "AGENT SCANNER LOG FLOW:",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .border(1.dp, Color(0xFF2E3550), RoundedCornerShape(10.dp))
                                    .background(Color(0xFF090A10))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(
                                    state = logsListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.parsingLogs) { log ->
                                        Text(
                                            text = log,
                                            color = when {
                                                log.contains("[SUCCESS]") -> NeonGreen
                                                log.contains("[SYSTEM]") -> MutedText
                                                log.contains("[ERROR]") -> NeonRed
                                                else -> PureWhite
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Actions & Error Output
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "[ERROR] // $error",
                            color = NeonRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!uiState.isParsingCV && uiState.parsingLogs.isEmpty()) {
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF14B8A6), Color(0xFF10B981))
                    )

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.handleIntent(AuthIntent.SaveCredentials)
                            viewModel.handleIntent(AuthIntent.UploadResume(resumeText))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradientBrush)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "INITIALIZE CAREER AGENT",
                                color = PureBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            resumeText = """
                                Artur, Senior Android Developer.
                                Опыт работы: 6 лет в мобильной разработке.
                                Ключевой стек: Kotlin, Coroutines, Flow, Dagger Hilt, Room, Clean Architecture, MVI, Jetpack Compose, Custom Canvas, CI/CD, Git.
                                Проекты: разработка банковских финтех приложений, оптимизация UI производительности до 120 FPS.
                            """.trimIndent()
                            selectedPdfName = "demo_senior_cv.pdf"
                            selectedPdfSize = "1.2 KB"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3550)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PureWhite
                        )
                    ) {
                        Text(
                            text = "LOAD DEMO RESUME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (uiState.isParsingCV) {
                    LinearProgressIndicator(
                        color = NeonGreen,
                        trackColor = Color(0xFF172620),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF14B8A6), Color(0xFF10B981))
                    )
                    Button(
                        onClick = { onNavigateToFeed() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradientBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENTER CAREER FEED",
                                color = PureBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
    var name = "Unknown"
    var sizeStr = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                val size = it.getLong(sizeIndex)
                sizeStr = if (size > 1024 * 1024) {
                    String.format(Locale.US, "%.1f MB", size.toFloat() / (1024 * 1024))
                } else if (size > 1024) {
                    "${size / 1024} KB"
                } else {
                    "$size B"
                }
            }
        }
    }
    return Pair(name, sizeStr)
}
