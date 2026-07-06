package com.multi.aijobhunter.feature.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.multi.aijobhunter.feature.shared_ui.MutedText
import com.multi.aijobhunter.feature.shared_ui.NeonGreen
import com.multi.aijobhunter.feature.shared_ui.NeonRed
import com.multi.aijobhunter.feature.shared_ui.PureBlack
import com.multi.aijobhunter.feature.shared_ui.PureWhite
import com.multi.aijobhunter.feature.shared_ui.TerminalDarkGray
import com.multi.aijobhunter.feature.shared_ui.TerminalMediumGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ProfileSideEffect.NavigateBack -> onNavigateBack()
                is ProfileSideEffect.ShowMessage -> {
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
                        text = "PROFILE_CONFIG //",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "> FULL NAME:",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeFullName(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> TARGET ROLE / TITLE:",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.targetPosition,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeTargetPosition(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> SKILLS VECTOR (COMMA SEPARATED):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.skillsText,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeSkillsText(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> CUSTOM AI INSTRUCTIONS / FILTER PROMPT:",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.customAiPrompt,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeCustomAiPrompt(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(bottom = 8.dp),
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> HEADHUNTER CONTACT EMAIL (REQUIRED FOR SEARCH):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.hhContactEmail,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeHhContactEmail(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "e.g., mail@example.com (Avoids 403 Forbidden)",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> HEADHUNTER ACCESS TOKEN (OAUTH TOKEN):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.hhAccessToken,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeHhAccessToken(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Paste OAuth token (Optional)",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> HEADHUNTER OAUTH CLIENT ID (OPTIONAL):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.hhClientId,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeHhClientId(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Enter Client ID from dev.hh.ru",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> HEADHUNTER OAUTH CLIENT SECRET (OPTIONAL):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.hhClientSecret,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeHhClientSecret(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Enter Client Secret from dev.hh.ru",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Text(
                        text = "> OAUTH PROXY URL (BACKEND PROXY URL):",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.hhBackendUrl,
                        onValueChange = { viewModel.handleIntent(ProfileIntent.ChangeHhBackendUrl(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Enter backend proxy URL (Optional)",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalDarkGray,
                            unfocusedContainerColor = TerminalDarkGray,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalMediumGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    if (uiState.hhClientId.isNotBlank() && (uiState.hhClientSecret.isNotBlank() || uiState.hhBackendUrl.isNotBlank())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val authUrl = "https://hh.ru/oauth/authorize?response_type=code&client_id=${uiState.hhClientId}&redirect_uri=aijobhunter://oauth"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = PureBlack
                            )
                        ) {
                            Text(
                                text = "CONNECT HEADHUNTER ACCOUNT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "To search jobs, enter a valid email for the User-Agent. If you want to link your account, you can also paste a token or enter your Client ID/Secret and click Connect.",
                        color = MutedText,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )

                    // DIAGNOSTICS & SCOUT LOGS
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A26).copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, Color(0xFF252B3D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🤖 SCOUT AGENT RUN LOGS",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (uiState.scoutLogs.isNotEmpty()) {
                                    Text(
                                        text = "[CLEAR]",
                                        color = NeonRed,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { viewModel.handleIntent(ProfileIntent.ClearLogs) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.scoutLogs.isEmpty()) {
                                Text(
                                    text = "No scouting cycles recorded yet. The background agent scans HH/LinkedIn every 15 minutes.",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .border(1.dp, Color(0xFF2E3550), RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F111A))
                                        .padding(8.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(uiState.scoutLogs) { log ->
                                            val formattedTime = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "[$formattedTime] status: ${log.status}",
                                                        color = if (log.status == "SUCCESS") NeonGreen else NeonRed,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "scanned: ${log.scannedCount} | matches: ${log.matchedCount}",
                                                        color = MutedText,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Text(
                                                    text = log.message,
                                                    color = PureWhite,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp)
                    )
                } else {
                    Button(
                        onClick = { viewModel.handleIntent(ProfileIntent.SaveProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(
                            text = "[COMMIT CONFIGURATION //]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
