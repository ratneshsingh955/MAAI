package com.ratnesh.singh.myai.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

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
    
    /**
     * Generate text from Gemini given a prompt and an image.
     * For now, this will analyze the image description and provide a response.
     */
    suspend fun generateTextWithImage(prompt: String, imageUri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // For now, we'll provide a response about the image being uploaded
                // In a full implementation, you would use the Firebase AI Vision API
                val imagePrompt = if (prompt.isNotEmpty()) {
                    "I've uploaded an image with the following description: $prompt. Please analyze this image and provide insights about what you can see, including any objects, text, colors, or other notable features. If there's text in the image, please transcribe it. If it's a diagram or chart, please explain what it shows."
                } else {
                    "I've uploaded an image. Please analyze this image and provide insights about what you can see, including any objects, text, colors, or other notable features. If there's text in the image, please transcribe it. If it's a diagram or chart, please explain what it shows."
                }
                
                val response = model.generateContent(imagePrompt)
                response.text ?: "I can see you've uploaded an image, but I'm currently unable to analyze images directly. Please describe what you'd like to know about the image, and I'll do my best to help based on your description."
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with image", e)
                "I can see you've uploaded an image, but I'm currently unable to analyze images directly. Please describe what you'd like to know about the image, and I'll do my best to help based on your description."
            }
        }
    }
}
