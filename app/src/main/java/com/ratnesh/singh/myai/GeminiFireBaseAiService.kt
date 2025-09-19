package com.ratnesh.singh.myai.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiFireBaseAiService {

    private val model: GenerativeModel by lazy {
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-2.5-flash")
    }

    /**
     * Generate text from Gemini given a prompt.
     */
    suspend fun generateText(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                response.text ?: "No response returned."
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text", e)
                "Error: ${e.message}"
            }
        }
    }
}
