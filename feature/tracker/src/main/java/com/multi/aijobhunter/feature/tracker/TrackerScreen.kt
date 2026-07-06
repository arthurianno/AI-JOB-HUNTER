package com.multi.aijobhunter.feature.tracker

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.VacancyStatus
import com.multi.aijobhunter.feature.shared_ui.MutedText
import com.multi.aijobhunter.feature.shared_ui.NeonGreen
import com.multi.aijobhunter.feature.shared_ui.NeonRed
import com.multi.aijobhunter.feature.shared_ui.PureBlack
import com.multi.aijobhunter.feature.shared_ui.PureWhite
import com.multi.aijobhunter.feature.shared_ui.TerminalDarkGray
import com.multi.aijobhunter.feature.shared_ui.TerminalMediumGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is TrackerSideEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JOB_FUNNEL_KANBAN //",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = NeonGreen
                )
            )
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
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Колонки Kanban-доски
                KanbanColumn(
                    title = "MATCHED",
                    vacancies = uiState.vacancies.filter { it.status == VacancyStatus.MATCHED },
                    onMoveStatus = { id, status -> viewModel.handleIntent(TrackerIntent.UpdateVacancyStatus(id, status)) }
                )

                KanbanColumn(
                    title = "APPLIED",
                    vacancies = uiState.vacancies.filter { it.status == VacancyStatus.APPLIED },
                    onMoveStatus = { id, status -> viewModel.handleIntent(TrackerIntent.UpdateVacancyStatus(id, status)) }
                )

                KanbanColumn(
                    title = "INTERVIEW",
                    vacancies = uiState.vacancies.filter { it.status == VacancyStatus.INTERVIEW },
                    onMoveStatus = { id, status -> viewModel.handleIntent(TrackerIntent.UpdateVacancyStatus(id, status)) }
                )

                KanbanColumn(
                    title = "REJECTED",
                    vacancies = uiState.vacancies.filter { it.status == VacancyStatus.REJECTED },
                    onMoveStatus = { id, status -> viewModel.handleIntent(TrackerIntent.UpdateVacancyStatus(id, status)) }
                )
            }
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    vacancies: List<Vacancy>,
    onMoveStatus: (String, VacancyStatus) -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp))
            .background(TerminalDarkGray)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок колонки
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[$title]",
                    color = when (title) {
                        "MATCHED" -> NeonGreen
                        "APPLIED" -> PureWhite
                        "INTERVIEW" -> NeonGreen
                        else -> NeonRed
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${vacancies.size}",
                    color = MutedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vacancies) { vacancy ->
                    KanbanCard(vacancy = vacancy, onMoveStatus = onMoveStatus)
                }
            }
        }
    }
}

@Composable
fun KanbanCard(
    vacancy: Vacancy,
    onMoveStatus: (String, VacancyStatus) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PureBlack
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = vacancy.companyName,
                color = MutedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = vacancy.title,
                color = PureWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            vacancy.aiAnalysis?.let { analysis ->
                Text(
                    text = "Score: ${analysis.matchScore}%",
                    color = if (analysis.matchScore >= 85) NeonGreen else NeonGreen.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопки переходов
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (vacancy.status) {
                    VacancyStatus.MATCHED -> {
                        Text(
                            text = "[APPLY]",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.APPLIED) }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "[DECLINE]",
                            color = NeonRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.REJECTED) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                    VacancyStatus.APPLIED -> {
                        Text(
                            text = "[INTERVIEW]",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.INTERVIEW) }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "[REJECT]",
                            color = NeonRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.REJECTED) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                    VacancyStatus.INTERVIEW -> {
                        Text(
                            text = "[OFFER!]",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { 
                                    onMoveStatus(vacancy.id, VacancyStatus.MATCHED) // reset or celebrate
                                    Toast.makeText(context, "Offer accepted! Congratulations! // 🎉", Toast.LENGTH_LONG).show()
                                }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "[REJECT]",
                            color = NeonRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.REJECTED) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                    VacancyStatus.REJECTED -> {
                        Text(
                            text = "[RETRY]",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onMoveStatus(vacancy.id, VacancyStatus.MATCHED) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
