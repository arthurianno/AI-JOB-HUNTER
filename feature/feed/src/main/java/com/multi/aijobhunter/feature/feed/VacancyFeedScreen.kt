package com.multi.aijobhunter.feature.feed

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.feature.shared_ui.MatchScoreBadge
import com.multi.aijobhunter.feature.shared_ui.MutedText
import com.multi.aijobhunter.feature.shared_ui.NeonAmber
import com.multi.aijobhunter.feature.shared_ui.NeonGreen
import com.multi.aijobhunter.feature.shared_ui.PureBlack
import com.multi.aijobhunter.feature.shared_ui.PureWhite
import com.multi.aijobhunter.feature.shared_ui.TerminalDarkGray
import com.multi.aijobhunter.feature.shared_ui.TerminalMediumGray
import com.multi.aijobhunter.feature.shared_ui.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacancyFeedScreen(
    viewModel: FeedViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val vacancies = viewModel.vacanciesFlow.collectAsLazyPagingItems()

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is FeedSideEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Мигающая зеленая точка активности WorkManager/Agent
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (uiState.isSyncing) NeonGreen else NeonGreen.copy(alpha = 0.4f))
                        )
                        Text(
                            text = "JOB_HUNTER_AGENT //",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Офлайн баннер
            if (uiState.isOffline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonAmber.copy(alpha = 0.1f))
                        .border(1.dp, NeonAmber)
                        .padding(vertical = 6.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "OFFLINE_MODE // Displaying cached jobs",
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Фильтры
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalFilterChip(
                    text = "[Remote]",
                    selected = uiState.filterRemote,
                    onClick = { viewModel.handleIntent(FeedIntent.ToggleRemoteFilter) }
                )
                TerminalFilterChip(
                    text = "[Match > 85%]",
                    selected = uiState.filterMatch85,
                    onClick = { viewModel.handleIntent(FeedIntent.ToggleMatch85Filter) }
                )
                TerminalFilterChip(
                    text = "[\$2500+]",
                    selected = uiState.filterHighSalary,
                    onClick = { viewModel.handleIntent(FeedIntent.ToggleHighSalaryFilter) }
                )
            }

            // Кнопки быстрой навигации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[TRACKER KANBAN BOARD //]",
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToTracker() }
                        .padding(vertical = 4.dp)
                )
                
                Text(
                    text = "[FORCE SCAN NOW //]",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.handleIntent(FeedIntent.ForceScanNow) }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список вакансий
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (vacancies.loadState.refresh is LoadState.Loading && vacancies.itemCount == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            ShimmerVacancyCard()
                        }
                    }
                } else if (vacancies.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "> _",
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "No high-match jobs found.",
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Relax, AI agent is scouting the market.",
                                color = MutedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = vacancies.itemCount,
                            key = { index -> vacancies.peek(index)?.id ?: index }
                        ) { index ->
                            val vacancy = vacancies[index]
                            if (vacancy != null) {
                                VacancyCard(
                                    vacancy = vacancy,
                                    onClick = { onNavigateToDetails(vacancy.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) NeonGreen else TerminalDarkGray,
        contentColor = if (selected) PureBlack else PureWhite,
        border = BorderStroke(1.dp, if (selected) NeonGreen else TerminalMediumGray),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VacancyCard(
    vacancy: Vacancy,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(
            containerColor = TerminalDarkGray
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Источник и компания + Дата
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sdf = remember { java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()) }
                    val dateStr = remember(vacancy.createdAt) { sdf.format(java.util.Date(vacancy.createdAt)) }
                    val isHot = (vacancy.aiAnalysis?.matchScore ?: 0) >= 80
                    
                    Text(
                        text = if (isHot) "🔥 [${vacancy.source.name}]" else "[${vacancy.source.name}]",
                        color = if (isHot) NeonAmber else NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF333333),
                        fontSize = 10.sp
                    )
                    Text(
                        text = vacancy.companyName,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Название вакансии
                Text(
                    text = vacancy.title,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Зарплата
                val salaryText = if (vacancy.salary != null) {
                    val from = vacancy.salary!!.from?.toInt()
                    val to = vacancy.salary!!.to?.toInt()
                    when {
                        from != null && to != null -> "$from - $to ${vacancy.salary!!.currency}"
                        from != null -> "от $from ${vacancy.salary!!.currency}"
                        to != null -> "до $to ${vacancy.salary!!.currency}"
                        else -> "ЗП не указана"
                    }
                } else {
                    "ЗП не указана"
                }

                Text(
                    text = salaryText,
                    color = PureWhite.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                vacancy.aiAnalysis?.let { analysis ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analysis.summary,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            vacancy.aiAnalysis?.let { analysis ->
                Spacer(modifier = Modifier.width(12.dp))
                MatchScoreBadge(score = analysis.matchScore)
            }
        }
    }
}

@Composable
fun ShimmerVacancyCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(
            containerColor = TerminalDarkGray
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp)
                        .shimmer()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .shimmer()
            )
        }
    }
}
