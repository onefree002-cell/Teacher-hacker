package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiChatService {

    private const val TAG = "GeminiChatService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.fields.firstOrNull { 
                it.name.equals("GEMINI_API_KEY", ignoreCase = true) || it.name.equals("apiKey", ignoreCase = true) 
            }
            field?.get(null) as? String ?: System.getenv("GEMINI_API_KEY") ?: ""
        } catch (e: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String = "أنت المساعد الذكي للمعلم 'Teacher Hacker AI'. مهمتك مساعدة المعلم في إعداد الأسئلة، تحضير الدروس، تقديم أفكار تفاعلية وشرح المفاهيم المعقدة، وصياغة الرسائل لأولياء الأمور باللغة العربية والإنجليزية."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isBlank() || apiKey == "DEFAULT_KEY") {
            val lastUserMessage = history.lastOrNull { it.isUser }?.text ?: ""
            return@withContext getSmartLocalResponse(lastUserMessage)
        }

        try {
            val jsonBody = JSONObject().apply {
                // System Instruction
                val sysInstructionObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", parts)
                }
                put("systemInstruction", sysInstructionObj)

                // Conversation contents history
                val contentsArray = JSONArray()
                history.takeLast(16).forEach { msg ->
                    val contentObj = JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "model")
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", msg.text)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    contentsArray.put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
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
                val lastUserMessage = history.lastOrNull { it.isUser }?.text ?: ""
                return@withContext getSmartLocalResponse(lastUserMessage)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val reply = parts.getJSONObject(0).optString("text", "")
                    if (reply.isNotBlank()) return@withContext reply.trim()
                }
            }

            val lastUserMsg = history.lastOrNull { it.isUser }?.text ?: ""
            return@withContext getSmartLocalResponse(lastUserMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Chat error", e)
            val lastUserMsg = history.lastOrNull { it.isUser }?.text ?: ""
            return@withContext getSmartLocalResponse(lastUserMsg)
        }
    }

    private fun getSmartLocalResponse(userPrompt: String): String {
        val q = userPrompt.lowercase()
        return when {
            q.contains("امتحان") || q.contains("سؤال") || q.contains("اختبار") || q.contains("exam") || q.contains("quiz") ->
                "📚 إليك نموذج أسئلة مقترح لحصتك:\n" +
                "1. (اختيار من متعدد) ما هو المفهوم الأساسي في هذا الدرس؟ [أ، ب، ج، د]\n" +
                "2. (علل لما يأتي) وضح السبب العلمي أو المنطقي للظاهرة المعطاة.\n" +
                "3. (مسألة تطبيقية) احسب الناتج مع كتابة خطوات القانون الرياضي بالتفصيل.\n\n" +
                "💡 يمكنك نسخ هذه الأسئلة إلى قسم بنك الأسئلة والامتحانات بالتطبيق بنقرة واحدة!"

            q.contains("واجب") || q.contains("تمرين") || q.contains("homework") ->
                "📝 مقترح واجب للحصة القادمة:\n" +
                "• حل تدريبات الصفحة المحددة من الكتاب المدرسي أو المذكرة.\n" +
                "• تلخيص النقاط الـ 3 الأساسية في كشكول الطالب.\n" +
                "• إعداد سؤال تحدي تفكير ناقد للمناقشة في بداية الحصة القادمة."

            q.contains("ولي أمر") || q.contains("رسالة") || q.contains("واتساب") || q.contains("parent") ->
                "💬 صيغة رسالة مهذبة لولي الأمر:\n" +
                "«السلام عليكم ورحمة الله، نود إحاطة سيادتكم علماً بالمستوى المتميز لنجلكم في حصة اليوم مع التوصية بالتركيز على مراجعة واجب الدرس المسجل في التطبيق. شكراً لتعاونكم المثمر.»"

            q.contains("تحضير") || q.contains("درس") || q.contains("خطة") || q.contains("prep") || q.contains("lesson") ->
                "🎯 خطة درس مقترحة:\n" +
                "1. التهيئة الحافزة (5 دقائق): طرح لغز أو سؤال مثير للاهتمام.\n" +
                "2. الشرح التفاعلي والنمذجة (20 دقيقة): استخدام السبورة الذكية وعرض المفاهيم.\n" +
                "3. نشاط مجموعات مصغر (15 دقيقة): حل تدريبات تطبيقية.\n" +
                "4. التقويم الختامي والغلق (10 دقائق): استخراج الخلاصات وتحديد واجب الحصة."

            else ->
                "أهلاً بك يا أستاذي الفاضل! 🌟\nأنا مساعدك الذكي في Teacher Hacker. يسعدني مساعدتك في صياغة أسئلة الامتحانات، توليد خطط الدروس التفاعلية، تنظيم مواعيدك، أو إعداد رسائل متابعة الطلاب وأولياء الأمور. تفضل بكتابة أي طلب وسأقوم بتجهيزه فوراً!"
        }
    }
}
