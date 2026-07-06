package com.multi.aijobhunter.core.network

import com.multi.aijobhunter.core.common.JobSearchCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

@Serializable
data class RawVacancy(
    val id: String,
    val title: String,
    val companyName: String,
    val salaryFrom: Double?,
    val salaryTo: Double?,
    val salaryCurrency: String,
    val description: String,
    val url: String,
    val source: String,
    val createdAt: Long
)

interface JobSourcePlugin {
    val sourceName: String
    suspend fun fetchVacancies(query: String): List<RawVacancy>
}

private fun stripHtml(html: String): String {
    return html.replace(Regex("<[^>]*>"), "").trim()
}

// ══════════════════════════════════════════════════════════════
// HeadHunter (hh.ru) — REAL PUBLIC API, без ключа
// ══════════════════════════════════════════════════════════════
class HhRuJobSourcePlugin(
    private val client: OkHttpClient,
    private val credentialsProvider: JobSearchCredentialsProvider
) : JobSourcePlugin {
    override val sourceName = "HeadHunter"

    override suspend fun fetchVacancies(query: String): List<RawVacancy> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.hh.ru/vacancies?text=$encoded&per_page=20&order_by=publication_time"
            
            val email = credentialsProvider.getHhContactEmail().ifBlank { "support@aijobhunter.com" }
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "AiJobHunter/1.0 ($email)")
                
            val token = credentialsProvider.getHhAccessToken()
            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext emptyList()

            (0 until items.length()).mapNotNull { i ->
                parseHhVacancy(items.getJSONObject(i))
            }
        } catch (e: Exception) {
            System.err.println("HH.ru API error: ${e.localizedMessage}")
            emptyList()
        }
    }

    private fun parseHhVacancy(item: JSONObject): RawVacancy? {
        return try {
            val salary = item.optJSONObject("salary")
            val employer = item.optJSONObject("employer")
            val snippet = item.optJSONObject("snippet")
            val area = item.optJSONObject("area")
            val experience = item.optJSONObject("experience")
            val schedule = item.optJSONObject("schedule")
            val employment = item.optJSONObject("employment")

            val description = buildString {
                area?.optString("name")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("📍 Локация: $it\n")
                }
                experience?.optString("name")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("📊 Опыт: $it\n")
                }
                schedule?.optString("name")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("⏰ График: $it\n")
                }
                employment?.optString("name")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("💼 Занятость: $it\n")
                }
                append("\n")
                snippet?.optString("requirement")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("Требования:\n${stripHtml(it)}\n\n")
                }
                snippet?.optString("responsibility")?.takeIf { it != "null" && it.isNotBlank() }?.let {
                    append("Обязанности:\n${stripHtml(it)}")
                }
            }

            RawVacancy(
                id = "hh-${item.getString("id")}",
                title = item.getString("name"),
                companyName = employer?.optString("name", "Не указана") ?: "Не указана",
                salaryFrom = salary?.optDouble("from")?.takeIf { !it.isNaN() },
                salaryTo = salary?.optDouble("to")?.takeIf { !it.isNaN() },
                salaryCurrency = salary?.optString("currency", "RUR") ?: "RUR",
                description = description.ifBlank { item.getString("name") },
                url = item.optString("alternate_url", "https://hh.ru/vacancy/${item.getString("id")}"),
                source = "HH",
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Habr Career — REAL PUBLIC API via RSS, без регистрации и ключей
// ══════════════════════════════════════════════════════════════
class HabrCareerJobSourcePlugin(private val client: OkHttpClient) : JobSourcePlugin {
    override val sourceName = "Habr Career"

    override suspend fun fetchVacancies(query: String): List<RawVacancy> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://career.habr.com/vacancies/rss?q=$encoded"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AiJobHunter/1.0")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val body = response.body?.string() ?: return@withContext emptyList()
            parseRss(body)
        } catch (e: Exception) {
            System.err.println("Habr Career API error: ${e.localizedMessage}")
            emptyList()
        }
    }

    private fun parseRss(xml: String): List<RawVacancy> {
        val vacancies = mutableListOf<RawVacancy>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))
            
            var eventType = xpp.eventType
            var currentTitle = ""
            var currentDescription = ""
            var currentCompany = ""
            var currentLink = ""
            var currentGuid = ""
            
            var insideItem = false
            var currentTagName = ""
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTagName = xpp.name
                    if (currentTagName == "item") {
                        insideItem = true
                        currentTitle = ""
                        currentDescription = ""
                        currentCompany = ""
                        currentLink = ""
                        currentGuid = ""
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (insideItem) {
                        val text = xpp.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTagName) {
                                "title" -> currentTitle = text
                                "description" -> currentDescription = text
                                "author" -> currentCompany = text
                                "link" -> currentLink = text
                                "guid" -> currentGuid = text
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.name == "item") {
                        insideItem = false
                        var cleanTitle = currentTitle
                        if (cleanTitle.startsWith("Требуется «")) {
                            cleanTitle = cleanTitle.removePrefix("Требуется «")
                            cleanTitle = cleanTitle.replace("»", "")
                        }
                        
                        if (cleanTitle.isNotBlank()) {
                            val fallbackId = currentLink.substringAfterLast("/", "").substringBefore("?")
                            val stableGuid = currentGuid.ifBlank { fallbackId }.ifBlank { System.currentTimeMillis().toString() }
                            
                            vacancies.add(
                                RawVacancy(
                                    id = "habr-$stableGuid",
                                    title = cleanTitle,
                                    companyName = currentCompany.ifBlank { "Не указана" },
                                    salaryFrom = null,
                                    salaryTo = null,
                                    salaryCurrency = "RUR",
                                    description = currentDescription,
                                    url = currentLink.ifBlank { "https://career.habr.com/vacancies/$stableGuid" },
                                    source = "Habr Career",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    currentTagName = ""
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return vacancies
    }
}

// ══════════════════════════════════════════════════════════════
// Работа России (trudvsem.ru) — REAL PUBLIC API, без регистрации и ключей
// ══════════════════════════════════════════════════════════════
class TrudVsemJobSourcePlugin(private val client: OkHttpClient) : JobSourcePlugin {
    override val sourceName = "TrudVsem"

    override suspend fun fetchVacancies(query: String): List<RawVacancy> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://opendata.trudvsem.ru/api/v1/vacancies?text=$encoded&limit=20"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AiJobHunter/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONObject("results") ?: return@withContext emptyList()
            val vacanciesJson = results.optJSONArray("vacancies") ?: return@withContext emptyList()

            (0 until vacanciesJson.length()).mapNotNull { i ->
                try {
                    val entry = vacanciesJson.getJSONObject(i).getJSONObject("vacancy")
                    val company = entry.optJSONObject("company")
                    val requirements = entry.optString("requirements", "")
                    val duty = entry.optString("duty", "")
                    
                    val description = buildString {
                        if (requirements.isNotBlank()) append("Требования:\n$requirements\n\n")
                        if (duty.isNotBlank()) append("Обязанности:\n$duty")
                    }

                    RawVacancy(
                        id = "trudvsem-${entry.getString("id")}",
                        title = entry.getString("job-name"),
                        companyName = company?.optString("name", "Не указана") ?: "Не указана",
                        salaryFrom = entry.optDouble("salary_min").takeIf { !it.isNaN() && it > 0.0 },
                        salaryTo = entry.optDouble("salary_max").takeIf { !it.isNaN() && it > 0.0 },
                        salaryCurrency = "RUR",
                        description = description.ifBlank { entry.getString("job-name") },
                        url = entry.optString("vac_url", "https://trudvsem.ru/vacancy/card/${entry.getString("id")}"),
                        source = "Работа России",
                        createdAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            System.err.println("TrudVsem API error: ${e.localizedMessage}")
            emptyList()
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Remotive (remotive.com) — REAL PUBLIC API, без регистрации и ключей
// ══════════════════════════════════════════════════════════════
class RemotiveJobSourcePlugin(private val client: OkHttpClient) : JobSourcePlugin {
    override val sourceName = "Remotive"

    override suspend fun fetchVacancies(query: String): List<RawVacancy> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://remotive.com/api/remote-jobs?search=$encoded&limit=20"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AiJobHunter/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val jobs = json.optJSONArray("jobs") ?: return@withContext emptyList()

            (0 until jobs.length()).mapNotNull { i ->
                try {
                    val entry = jobs.getJSONObject(i)
                    val rawDesc = entry.optString("description", "")
                    
                    RawVacancy(
                        id = "remotive-${entry.getString("id")}",
                        title = entry.getString("title"),
                        companyName = entry.optString("company_name", "Не указана"),
                        salaryFrom = null,
                        salaryTo = null,
                        salaryCurrency = "USD",
                        description = stripHtml(rawDesc).trim(),
                        url = entry.optString("url", "https://remotive.com/"),
                        source = "Remotive",
                        createdAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            System.err.println("Remotive API error: ${e.localizedMessage}")
            emptyList()
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Plugin Manager — оркестратор всех источников
// ══════════════════════════════════════════════════════════════
class JobPluginManager(client: OkHttpClient, credentialsProvider: JobSearchCredentialsProvider) {
    private val plugins: List<JobSourcePlugin> = listOf(
        HhRuJobSourcePlugin(client, credentialsProvider),
        HabrCareerJobSourcePlugin(client),
        TrudVsemJobSourcePlugin(client),
        RemotiveJobSourcePlugin(client)
    )

    suspend fun executeAllPlugins(query: String): List<RawVacancy> {
        val allVacancies = mutableListOf<RawVacancy>()
        for (plugin in plugins) {
            try {
                allVacancies.addAll(plugin.fetchVacancies(query))
            } catch (e: Exception) {
                System.err.println("Error running plugin ${plugin.sourceName}: ${e.localizedMessage}")
            }
        }
        return allVacancies
    }
}

