package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.LessonPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object AiLessonPlannerService {

    private const val TAG = "AiLessonPlanner"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateLessonPlan(
        subject: String,
        grade: String,
        topic: String,
        durationMinutes: Int = 60,
        extraNotes: String = ""
    ): LessonPlanEntity = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.fields.firstOrNull { it.name.equals("GEMINI_API_KEY", ignoreCase = true) || it.name.equals("apiKey", ignoreCase = true) }
            field?.get(null) as? String ?: System.getenv("GEMINI_API_KEY") ?: ""
        } catch (e: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "DEFAULT_KEY") {
            Log.d(TAG, "No API key found, generating pedagogical fallback template")
            return@withContext generatePedagogicalFallback(subject, grade, topic, durationMinutes, extraNotes)
        }

        try {
            val systemPrompt = """
                أنت خبير تربوي ومستشار تعليمي وتوجيه فني أول. قم بإنشاء خطة تحضير درس نموذجية واحترافية وفق أحدث معايير التعليم والتعلم النشط والتفكير الإبداعي.
                يجب أن تكون المخرجات بتنسيق JSON حصرياً وبالمفاتيح التالية:
                - title: عنوان الدرس الشامل
                - objectives: أهداف التعلم (معرفية، وجدانية، مهارية) بصيغة مرقمة
                - warmupHook: استراتيجية التمهيد والتهيئة الحافزة والعصف الذهني
                - keyPoints: عناصر الشرح الأساسية والمفاهيم الجوهرية بالتفصيل
                - teachingAids: الوسائل التعليمية والأدوات الرقمية المناسبة
                - activities: استراتيجيات التدريس والأنشطة الطلابية التفاعلية
                - assessmentQuestions: أسئلة التقويم التكويني والختامي ومستويات التفكير العليا
                - homework: التكليفات والواجبات المنزلية التطبيقية
                - notes: إرشادات وتوصيات المعلم ومراعاة الفروق الفردية
            """.trimIndent()

            val userContent = """
                المادة: $subject
                الصف الدراسي: $grade
                عنوان وموضوع الدرس: $topic
                المدة الزمنية للحصة: $durationMinutes دقيقة
                ملاحظات أو تركيز إضافي: $extraNotes
                
                قم بكتابة تحضير متكامل وشامل ومفصل ومفيد جداً للمعلم أثناء الحصة بصيغة JSON.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", "$systemPrompt\n\n$userContent")
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    val responseFormat = JSONObject().apply {
                        put("type", "application/json")
                    }
                    // Request json format
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val requestUrl = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.e(TAG, "API Error: ${response.code} -> $responseBody")
                return@withContext generatePedagogicalFallback(subject, grade, topic, durationMinutes, extraNotes)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val rawText = parts.getJSONObject(0).optString("text", "")
                    val parsedPlan = parseAiResponseJson(rawText, subject, grade, topic, durationMinutes)
                    if (parsedPlan != null) {
                        return@withContext parsedPlan
                    }
                }
            }

            return@withContext generatePedagogicalFallback(subject, grade, topic, durationMinutes, extraNotes)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
            return@withContext generatePedagogicalFallback(subject, grade, topic, durationMinutes, extraNotes)
        }
    }

    private fun parseAiResponseJson(
        rawJsonText: String,
        fallbackSubject: String,
        fallbackGrade: String,
        fallbackTopic: String,
        durationMinutes: Int
    ): LessonPlanEntity? {
        return try {
            var cleanJson = rawJsonText.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json").trim()
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```").trim()
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```").trim()
            }

            val obj = JSONObject(cleanJson)
            val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())

            LessonPlanEntity(
                title = obj.optString("title", "تحضير: $fallbackTopic").ifBlank { "تحضير: $fallbackTopic" },
                subject = obj.optString("subject", fallbackSubject).ifBlank { fallbackSubject },
                grade = obj.optString("grade", fallbackGrade).ifBlank { fallbackGrade },
                targetDate = today,
                durationMinutes = durationMinutes,
                objectives = obj.optString("objectives", ""),
                warmupHook = obj.optString("warmupHook", ""),
                keyPoints = obj.optString("keyPoints", ""),
                teachingAids = obj.optString("teachingAids", "السبورة الذكية، الشيتات، المجسمات، بنك الأسئلة"),
                activities = obj.optString("activities", ""),
                assessmentQuestions = obj.optString("assessmentQuestions", ""),
                homework = obj.optString("homework", ""),
                notes = obj.optString("notes", ""),
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed for AI response", e)
            null
        }
    }

    fun generatePedagogicalFallback(
        subject: String,
        grade: String,
        topic: String,
        durationMinutes: Int = 60,
        extraNotes: String = ""
    ): LessonPlanEntity {
        val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val cleanSubject = subject.ifBlank { "المادة التعليمية" }
        val cleanTopic = topic.ifBlank { "درس نموذجي" }

        val objectives = """
            1. أن يستوعب الطالب المفاهيم والمصطلحات الجوهرية لـ ($cleanTopic) بدقة.
            2. أن يحلل القواعد والنظريات والعلاقات الرياضية والعلمية المرتبطة بالدرس.
            3. أن يطبق المهارات العملية من خلال حل مسائل وتدريبات متنوعة وتفسير النتائج.
            4. أن يربط الطالب بين موضوع الدرس وتطبيقاته الحياتية في الواقع.
        """.trimIndent()

        val warmup = """
            • مراجعة تمهيدية (3-5 دقائق) على المكتسبات السابقة وربطها بموضوع ($cleanTopic).
            • طرح سؤال تحدٍ ذكي أو موقف إشكالي يثير فضول وتفاعل الطلاب.
            • عصف ذهني سريع وتدوين الفرضيات الأولية على السبورة لمناقشتها.
        """.trimIndent()

        val keyPoints = """
            ① التمهيد والمصطلحات الأساسية والتعريف الدقيق.
            ② شرح القوانين والأفكار الرئيسية مع أمثلة ونماذج توضيحية متدرجة.
            ③ التركيز على الحالات الخاصة ومفاتيح حل الأسئلة والأخطاء الشائعة.
            ④ تدريب عملي فوري وحل مسألة نموذجية خطوة بخطوة على السبورة.
            ⑤ تلخيص خريطة المفاهيم وخلاصة الدرس.
        """.trimIndent()

        val teachingAids = "السبورة التفاعلية الذكية، الأدوات الهندسية، الشيت التدريبي، بطاقات التحدي، نماذج توضيحية"

        val activities = """
            • استراتيجية فكّر - زاوج - شارك: حل تمرين رقم (1) ومقارنة النتائج مع الزميل.
            • التعلم التنافسي: مسابقة السرعة والدقة بين مجموعات الصف في حل المسائل التطبيقية.
            • مسرحة المناهج / النمذجة العملية للأفكار المعقدة.
        """.trimIndent()

        val assessmentQuestions = """
            س1 (مستوى المعرفة والفهم): عرّف المفهوم الأساسي لـ ($cleanTopic) واذكر أهم خصائصه؟
            س2 (مستوى التطبيق والتحليل): حل المسألة التطبيقية واستنتج النتيجة مع بيان خطوات الحل؟
            س3 (مستويات التفكير العليا والابتكار): ماذا يحدث إذا تم تغيير أحد الشروط الأساسية؟ برهن إجابتك.
        """.trimIndent()

        val homework = "حل تمارين صـ (15 - 18) في كشكول الواجب + حل أسئلة التميز في الشيت الأسبوعي"

        val notes = if (extraNotes.isNotBlank()) extraNotes else "مراعاة الفروق الفردية، وتقديم دعم إضافي للطلاب الذين يحتاجون تعزيزاً في النقطة رقم (2)."

        return LessonPlanEntity(
            title = "تحضير ذكي: $cleanTopic",
            subject = cleanSubject,
            grade = grade.ifBlank { "الصف العام" },
            targetDate = today,
            durationMinutes = durationMinutes,
            objectives = objectives,
            warmupHook = warmup,
            keyPoints = keyPoints,
            teachingAids = teachingAids,
            activities = activities,
            assessmentQuestions = assessmentQuestions,
            homework = homework,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
    }
}
