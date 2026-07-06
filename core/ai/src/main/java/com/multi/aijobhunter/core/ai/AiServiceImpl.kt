package com.multi.aijobhunter.core.ai

import com.multi.aijobhunter.core.model.AiAnalysis
import com.multi.aijobhunter.core.model.RadarMetrics
import com.multi.aijobhunter.core.model.Vacancy
import com.multi.aijobhunter.core.model.UserProfile
import com.multi.aijobhunter.core.network.LlmApiService
import com.multi.aijobhunter.core.network.OpenAiChatRequest
import com.multi.aijobhunter.core.network.ChatMessage
import com.multi.aijobhunter.core.network.ResponseFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiServiceImpl @Inject constructor(
    private val apiService: LlmApiService,
    private val apiKeyProvider: LlmApiKeyProvider
) : AiService {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun analyze(vacancy: Vacancy, profile: UserProfile): AiAnalysis {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank() || apiKey == "demo") {
            return generateLocalHeuristicAnalysis(vacancy, profile)
        }

        val systemPrompt = """
            You are an expert tech recruiter and technical architect.
            Analyze the provided Job Vacancy against the Candidate User Profile.
            
            CRITICAL RULE FOR MATCHING:
            You MUST strictly enforce and respect the candidate's "Custom Prompt/Instructions". These instructions represent the candidate's hard constraints (such as "remote only", "fintech only", "no outsource", "product only", specific tech stack, etc.).
            If the Job Vacancy violates any of these custom instructions (for example, the job is not in Fintech when "only Fintech" is specified, or is an outsourcing agency when "no outsource" is specified), you MUST heavily penalize the matchScore (set it below 50, down to 0 for clear violations) and list this violation as the primary item in the "cons" array.

            Your output MUST be a strict single JSON object matching this schema exactly, with no markdown formatting around it, no backticks, no markdown blocks:
            {
              "matchScore": Int (0 to 100),
              "summary": "String short 1-sentence analytical overview",
              "pros": ["String line 1 Match", "String line 2 Match"],
              "cons": ["String risk 1", "String risk 2"],
              "radarMetrics": {
                 "hardSkills": Float (0.0 to 1.0),
                 "softSkills": Float (0.0 to 1.0),
                 "experience": Float (0.0 to 1.0),
                 "salaryMatch": Float (0.0 to 1.0),
                 "industryMatch": Float (0.0 to 1.0)
              }
            }
        """.trimIndent()

        val userPrompt = """
            Candidate Name: ${profile.fullName}
            Target Position: ${profile.targetPosition}
            Candidate Resume: ${profile.rawResumeText}
            Candidate Skills: ${profile.skills.joinToString()}
            Custom Prompt/Instructions: ${profile.customAiPrompt}

            Vacancy Title: ${vacancy.title}
            Company: ${vacancy.companyName}
            Description: ${vacancy.description}
        """.trimIndent()

        val baseUrl = apiKeyProvider.getBaseUrl().takeIf { it.isNotBlank() } ?: "https://api.openai.com/"
        val completionsUrl = if (baseUrl.endsWith("/")) "${baseUrl}v1/chat/completions" else "$baseUrl/v1/chat/completions"

        return try {
            val response = apiService.getChatCompletion(
                url = completionsUrl,
                apiKey = "Bearer $apiKey",
                request = OpenAiChatRequest(
                    model = apiKeyProvider.getModelName(),
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = userPrompt)
                    ),
                    response_format = ResponseFormat("json_object")
                )
            )
            val jsonContent = response.choices.firstOrNull()?.message?.content ?: ""
            parseAiAnalysisJson(jsonContent)
        } catch (e: Exception) {
            // Мягкий фоллбек на локальные эвристики при ошибках сети/лимитов
            generateLocalHeuristicAnalysis(vacancy, profile)
        }
    }

    override suspend fun createCoverLetter(vacancy: Vacancy, profile: UserProfile, style: String): String {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank() || apiKey == "demo") {
            return generateLocalCoverLetter(vacancy, profile, style)
        }

        val systemPrompt = """
            You are an expert career counselor. Generate a hyper-personalized cover letter in the requested style:
            Style options:
            - "Corporate" (strict, professional, formal)
            - "Startup" (direct, enthusiastic, friendly)
            - "Creative" (story-driven, out-of-the-box)

            Output ONLY the text of the cover letter. No introduction, no markdown backticks, no comments.
        """.trimIndent()

        val userPrompt = """
            Style: $style
            Vacancy: ${vacancy.title} at ${vacancy.companyName}
            Description: ${vacancy.description}

            Candidate Name: ${profile.fullName}
            Target Position: ${profile.targetPosition}
            Skills: ${profile.skills.joinToString()}
            Resume: ${profile.rawResumeText}
        """.trimIndent()

        val baseUrl = apiKeyProvider.getBaseUrl().takeIf { it.isNotBlank() } ?: "https://api.openai.com/"
        val completionsUrl = if (baseUrl.endsWith("/")) "${baseUrl}v1/chat/completions" else "$baseUrl/v1/chat/completions"

        return try {
            val response = apiService.getChatCompletion(
                url = completionsUrl,
                apiKey = "Bearer $apiKey",
                request = OpenAiChatRequest(
                    model = apiKeyProvider.getModelName(),
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = userPrompt)
                    )
                )
            )
            response.choices.firstOrNull()?.message?.content ?: "Could not generate cover letter."
        } catch (e: Exception) {
            generateLocalCoverLetter(vacancy, profile, style)
        }
    }

    private fun parseAiAnalysisJson(jsonContent: String): AiAnalysis {
        // Очищаем markdown-разметку, если LLM всё-таки вернула её
        var cleaned = jsonContent.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace(Regex("^```[a-zA-Z]*\\s*"), "")
            cleaned = cleaned.replace(Regex("\\s*```$"), "")
        }
        cleaned = cleaned.trim()

        val parsed = jsonParser.decodeFromString<MatchResultJson>(cleaned)
        return AiAnalysis(
            matchScore = parsed.matchScore,
            summary = parsed.summary,
            pros = parsed.pros,
            cons = parsed.cons,
            radarMetrics = RadarMetrics(
                hardSkills = parsed.radarMetrics.hardSkills,
                softSkills = parsed.radarMetrics.softSkills,
                experience = parsed.radarMetrics.experience,
                salaryMatch = parsed.radarMetrics.salaryMatch,
                industryMatch = parsed.radarMetrics.industryMatch
            )
        )
    }

    private fun generateLocalHeuristicAnalysis(vacancy: Vacancy, profile: UserProfile): AiAnalysis {
        // Локальный алгоритм оценки совпадения на основе стека
        val jobDescLower = vacancy.description.lowercase()
        val skillsLower = profile.skills.map { it.lowercase() }

        var matchedCount = 0
        val pros = mutableListOf<String>()
        val cons = mutableListOf<String>()

        // Сравниваем навыки
        for (skill in skillsLower) {
            if (jobDescLower.contains(skill)) {
                matchedCount++
                pros.add("Соответствие по ключевому навыку: ${skill.uppercase()}")
            }
        }

        // Базовая эвристика скоринга
        val matchPercent = when {
            vacancy.title.lowercase().contains("ios") || vacancy.title.lowercase().contains("flutter") -> {
                cons.add("Вакансия под другую мобильную платформу")
                10 // Не совпадает стек по платформе
            }
            else -> {
                val base = 40 + (matchedCount * 12).coerceAtMost(45)
                if (jobDescLower.contains("senior") && vacancy.title.lowercase().contains("senior")) {
                    pros.add("Уровень Senior соответствует вашему профилю")
                }
                if (jobDescLower.contains("remote") || jobDescLower.contains("удален")) {
                    pros.add("Доступен удаленный формат работы")
                } else {
                    cons.add("Не указана удаленная работа")
                }
                base.coerceAtMost(98)
            }
        }

        if (pros.isEmpty()) {
            pros.add("Интересный стек технологий и известная компания")
        }
        if (cons.isEmpty()) {
            cons.add("Требуется уточнение деталей о бенефитах и соцпакете")
        }

        // Радарные метрики
        val hardSkills = (0.5f + (matchedCount * 0.1f)).coerceAtMost(1.0f)
        val softSkills = 0.8f
        val experience = if (jobDescLower.contains("senior")) 0.9f else 0.6f
        val salaryMatch = if (vacancy.salary != null) 0.85f else 0.7f
        val industryMatch = 0.75f

        return AiAnalysis(
            matchScore = matchPercent,
            summary = "Локальный скоринг: вакансия совпадает на $matchPercent%. ${if (matchPercent > 75) "Отличный вариант для отклика!" else "Имеются расхождения по стеку или уровню."}",
            pros = pros,
            cons = cons,
            radarMetrics = RadarMetrics(hardSkills, softSkills, experience, salaryMatch, industryMatch)
        )
    }

    private fun generateLocalCoverLetter(vacancy: Vacancy, profile: UserProfile, style: String): String {
        return when (style.lowercase()) {
            "corporate" -> """
                Уважаемый нанимающий менеджер команды ${vacancy.companyName},
                
                Меня заинтересовала вакансия "${vacancy.title}" в Вашей компании. Мой опыт работы в качестве ${profile.targetPosition} составляет более 5 лет, с фокусом на разработку масштабируемых и отказоустойчивых мобильных решений.
                
                В своей работе я активно применяю: ${profile.skills.joinToString(", ")}. Ознакомившись с описанием вакансии, я уверен, что смогу внести существенный вклад в развитие Ваших продуктов.
                
                Буду рад обсудить подробности на интервью.
                
                С уважением,
                ${profile.fullName}
            """.trimIndent()
            "startup" -> """
                Привет, команда ${vacancy.companyName}!
                
                Увидел вашу вакансию на позицию "${vacancy.title}" и понял, что это отличный мэтч! Я ${profile.targetPosition} с горящими глазами и крепким стеком: ${profile.skills.take(5).joinToString(", ")}.
                
                Люблю продуктовый темп, быстрые итерации и чистый код. В ваших требованиях увидел много знакомого опыта, особенно в оптимизации UI и реактивных потоках.
                
                Давайте созвонимся и пообщаемся предметно!
                
                Cheers,
                ${profile.fullName}
            """.trimIndent()
            else -> """
                Здравствуйте!
                
                Вакансия "${vacancy.title}" в ${vacancy.companyName} привлекла мое внимание своей нестандартной задачей. Я ${profile.targetPosition}, и вместо шаблонных писем скажу просто — я люблю решать сложные инженерные задачи.
                
                Мой стек включает ${profile.skills.joinToString(", ")}, и я знаю, как превратить описанные вами требования в стабильный и быстрый продукт. Мой подход к UI и архитектуре позволит вашей команде быстрее поставлять новые фичи.
                
                Жду вашего ответа для знакомства!
                
                С наилучшими пожеланиями,
                ${profile.fullName}
            """.trimIndent()
        }
    }
}

@Serializable
private data class MatchResultJson(
    val matchScore: Int,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val radarMetrics: RadarMetricsJson
)

@Serializable
private data class RadarMetricsJson(
    val hardSkills: Float,
    val softSkills: Float,
    val experience: Float,
    val salaryMatch: Float,
    val industryMatch: Float
)
