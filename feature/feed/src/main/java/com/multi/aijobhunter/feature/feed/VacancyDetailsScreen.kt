package com.multi.aijobhunter.feature.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.feature.shared_ui.MatchScoreBadge
import com.multi.aijobhunter.feature.shared_ui.MutedText
import com.multi.aijobhunter.feature.shared_ui.NeonAmber
import com.multi.aijobhunter.feature.shared_ui.NeonGreen
import com.multi.aijobhunter.feature.shared_ui.NeonRed
import com.multi.aijobhunter.feature.shared_ui.PureBlack
import com.multi.aijobhunter.feature.shared_ui.PureWhite
import com.multi.aijobhunter.feature.shared_ui.RadarChart
import com.multi.aijobhunter.feature.shared_ui.TerminalDarkGray
import com.multi.aijobhunter.feature.shared_ui.TerminalMediumGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacancyDetailsScreen(
    viewModel: VacancyDetailsViewModel,
    vacancyId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = vacancyId) {
        viewModel.handleIntent(VacancyDetailsIntent.LoadDetails(vacancyId))
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is VacancyDetailsSideEffect.NavigateBack -> onNavigateBack()
                is VacancyDetailsSideEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is VacancyDetailsSideEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Cover Letter", effect.text)
                    clipboard.setPrimaryClip(clip)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Cover letter copied to clipboard //", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VACANCY_DETAILS //",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, uiState.vacancy?.url ?: "")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Job URL"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = NeonGreen
                )
            )
        },
        floatingActionButton = {
            if (uiState.vacancy != null) {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = NeonGreen,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "[GENERATE COVER LETTER]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        containerColor = PureBlack,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else if (uiState.vacancy == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Details not loaded.",
                    color = NeonRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        } else {
            val vacancy = uiState.vacancy!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Заголовок вакансии
                Text(
                    text = vacancy.title,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                    val dateFormatted = sdf.format(java.util.Date(vacancy.createdAt))
                    val isHot = (vacancy.aiAnalysis?.matchScore ?: 0) >= 80
                    
                    Text(
                        text = "${vacancy.companyName} // ${vacancy.source.name}",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "•",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isHot) "🔥 $dateFormatted" else dateFormatted,
                        color = if (isHot) NeonAmber else MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button to open vacancy in browser
                val context = LocalContext.current
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(vacancy.url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open URL: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E212C),
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN ORIGINAL LISTING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Статус и изменение
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS: ${vacancy.status.name}",
                        color = MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "[APPLIED]",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.handleIntent(VacancyDetailsIntent.ChangeStatus(VacancyStatus.APPLIED)) }
                            .padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "[REJECTED]",
                        color = NeonRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.handleIntent(VacancyDetailsIntent.ChangeStatus(VacancyStatus.REJECTED)) }
                            .padding(horizontal = 4.dp)
                    )
                }

                HorizontalDivider(color = TerminalMediumGray, modifier = Modifier.padding(vertical = 8.dp))

                // AI Deep Analysis Section
                Text(
                    text = "AI DEEP SCOUT ANALYSIS //",
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                vacancy.aiAnalysis?.let { analysis ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MatchScoreBadge(score = analysis.matchScore, size = 80.dp, strokeWidth = 7.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = analysis.summary,
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Radar Chart
                    RadarChart(
                        metrics = analysis.radarMetrics,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Pros & Cons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[+] STACK FIT",
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            analysis.pros.forEach { pro ->
                                Text(
                                    text = "• $pro",
                                    color = PureWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[-] RISKS / GAP",
                                color = NeonRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            analysis.cons.forEach { con ->
                                Text(
                                    text = "• $con",
                                    color = PureWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = TerminalMediumGray, modifier = Modifier.padding(vertical = 12.dp))

                // Основное описание вакансии с подсветкой ключевых слов
                Text(
                    text = "JOB DESCRIPTION //",
                    color = MutedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Подсвечиваем ключевые слова
                val highlightedText = buildAnnotatedString {
                    val text = vacancy.description
                    val keywords = listOf("Kotlin", "Compose", "Coroutines", "Flow", "Hilt", "Room", "MVI", "Clean Architecture", "WebRTC")
                    
                    var lastIndex = 0
                    
                    // Простой алгоритм поиска ключевых слов
                    while (lastIndex < text.length) {
                        var earliestMatch = -1
                        var matchKeyword = ""
                        
                        for (kw in keywords) {
                            val index = text.indexOf(kw, lastIndex, ignoreCase = true)
                            if (index != -1 && (earliestMatch == -1 || index < earliestMatch)) {
                                earliestMatch = index
                                matchKeyword = text.substring(index, index + kw.length)
                            }
                        }
                        
                        if (earliestMatch != -1) {
                            append(text.substring(lastIndex, earliestMatch))
                            withStyle(style = SpanStyle(color = NeonGreen, fontWeight = FontWeight.Bold)) {
                                append(matchKeyword)
                            }
                            lastIndex = earliestMatch + matchKeyword.length
                        } else {
                            append(text.substring(lastIndex))
                            break
                        }
                    }
                }

                Text(
                    text = highlightedText,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 80.dp) // Leave space for FAB
                )
            }
        }

        // BottomSheet для генерации письма
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = TerminalDarkGray,
                contentColor = PureWhite,
                scrimColor = PureBlack.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "AI_COVER_LETTER_COMPOSER //",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (uiState.isGeneratingLetter) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NeonGreen)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "[PARSING HIGHLIGHTS & COMPOSING LETTER...]",
                                color = MutedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else if (uiState.generatedLetter != null) {
                        var editableLetter by remember { mutableStateOf(uiState.generatedLetter!!) }

                        OutlinedTextField(
                            value = editableLetter,
                            onValueChange = { editableLetter = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = TerminalMediumGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Cover Letter", editableLetter)
                                    clipboard.setPrimaryClip(clip)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Cover letter copied to clipboard //", Toast.LENGTH_SHORT).show()
                                    showBottomSheet = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = PureBlack
                                )
                            ) {
                                Text(
                                    text = "[COPY TO CLIPBOARD]",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            
                            OutlinedButton(
                                onClick = { showBottomSheet = false },
                                modifier = Modifier.weight(0.5f),
                                border = BorderStroke(1.dp, TerminalMediumGray),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                            ) {
                                Text(
                                    text = "[CLOSE]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        // Выбор тональности
                        Text(
                            text = "Select Cover Letter tone / orientation:",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToneButton(
                                text = "Corporate (Formal)",
                                onClick = { viewModel.handleIntent(VacancyDetailsIntent.GenerateLetterClick("Corporate")) }
                            )
                            ToneButton(
                                text = "Startup (Direct)",
                                onClick = { viewModel.handleIntent(VacancyDetailsIntent.GenerateLetterClick("Startup")) }
                            )
                            ToneButton(
                                text = "Creative (Out-of-box)",
                                onClick = { viewModel.handleIntent(VacancyDetailsIntent.GenerateLetterClick("Creative")) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ToneButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        border = BorderStroke(1.dp, TerminalMediumGray),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PureBlack,
            contentColor = PureWhite
        )
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
