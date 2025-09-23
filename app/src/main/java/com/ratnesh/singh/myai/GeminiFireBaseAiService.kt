package com.ratnesh.singh.myai.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
     * Generate text from Gemini with conversation context.
     */
    suspend fun generateTextWithContext(prompt: String, context: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val fullPrompt = if (context.isNotEmpty()) {
                    "$context\n\nUser: $prompt"
                } else {
                    prompt
                }
                
                val response = model.generateContent(fullPrompt)
                response.text ?: "No response returned."
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with context", e)
                "Error: ${e.message}"
            }
        }
    }
    
    /**
     * Generate text from Gemini given a prompt and an image.
     * This method uses the actual Firebase AI multimodal capabilities.
     */
    suspend fun generateTextWithImage(prompt: String, imageUri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // Read the image data from URI
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes() ?: byteArrayOf()
                
                if (imageBytes.isEmpty()) {
                    return@withContext "Error: Could not read image data. Please try uploading the image again."
                }
                
                // Convert image bytes to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    return@withContext "Error: Could not decode image. Please try uploading a different image."
                }
                
                // Use Firebase AI multimodal content with both text and image
                val multimodalContent = content {
                    text(prompt)
                    image(bitmap)
                }
                
                // Generate content using multimodal input
                val response = model.generateContent(multimodalContent)
                response.text ?: "I'm having trouble analyzing the image. Could you provide more details about what you see in the image so I can help you better?"
                
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with image", e)
                "I encountered an error while analyzing the image. Please try uploading the image again or describe what you see in the image so I can help you better."
            }
        }
    }
    
    /**
     * Generate text from Gemini with image and conversation context.
     */
    suspend fun generateTextWithImageAndContext(
        prompt: String, 
        imageUri: Uri, 
        context: String, 
        androidContext: Context
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Read the image data from URI
                val inputStream: InputStream? = androidContext.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes() ?: byteArrayOf()
                
                if (imageBytes.isEmpty()) {
                    return@withContext "Error: Could not read image data. Please try uploading the image again."
                }
                
                // Convert image bytes to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    return@withContext "Error: Could not decode image. Please try uploading a different image."
                }
                
                // Build full prompt with context
                val fullPrompt = if (context.isNotEmpty()) {
                    "$context\n\nUser: $prompt"
                } else {
                    prompt
                }
                
                // Use Firebase AI multimodal content with both text and image
                val multimodalContent = content {
                    text(fullPrompt)
                    image(bitmap)
                }
                
                // Generate content using multimodal input
                val response = model.generateContent(multimodalContent)
                response.text ?: "I'm having trouble analyzing the image. Could you provide more details about what you see in the image so I can help you better?"
                
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with image and context", e)
                "I encountered an error while analyzing the image. Please try uploading the image again or describe what you see in the image so I can help you better."
            }
        }
    }
    
    /**
     * Generate text from Gemini given a prompt and a file.
     * This method reads the file content and sends it to Gemini for analysis.
     */
    suspend fun generateTextWithFile(prompt: String, fileUri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // Read the file content
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                val fileContent = inputStream?.readBytes() ?: byteArrayOf()
                
                if (fileContent.isEmpty()) {
                    return@withContext "Error: Could not read file content. Please try uploading the file again."
                }
                
                // Convert file content to text (assuming it's a text file)
                val fileText = String(fileContent, Charsets.UTF_8)
                
                // Create a comprehensive prompt that includes the file content
                val enhancedPrompt = """
                    File Content:
                    $fileText
                    
                    User Question: $prompt
                    
                    Please analyze the file content and answer the user's question based on the information in the file.
                """.trimIndent()
                
                // Generate content using the enhanced prompt
                val response = model.generateContent(enhancedPrompt)
                response.text ?: "I'm having trouble analyzing the file. Please make sure the file contains readable text content."
                
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with file", e)
                "I encountered an error while analyzing the file. Please make sure the file contains readable text content and try again."
            }
        }
    }
    
    /**
     * Generate text from Gemini with file and conversation context.
     */
    suspend fun generateTextWithFileAndContext(
        prompt: String, 
        fileUri: Uri, 
        context: String, 
        androidContext: Context
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Read the file content
                val inputStream: InputStream? = androidContext.contentResolver.openInputStream(fileUri)
                val fileContent = inputStream?.readBytes() ?: byteArrayOf()
                
                if (fileContent.isEmpty()) {
                    return@withContext "Error: Could not read file content. Please try uploading the file again."
                }
                
                // Convert file content to text (assuming it's a text file)
                val fileText = String(fileContent, Charsets.UTF_8)
                
                // Build full prompt with context
                val fullPrompt = if (context.isNotEmpty()) {
                    """
                    $context
                    
                    File Content:
                    $fileText
                    
                    User Question: $prompt
                    
                    Please analyze the file content and answer the user's question based on the information in the file.
                    """.trimIndent()
                } else {
                    """
                    File Content:
                    $fileText
                    
                    User Question: $prompt
                    
                    Please analyze the file content and answer the user's question based on the information in the file.
                    """.trimIndent()
                }
                
                // Generate content using the enhanced prompt
                val response = model.generateContent(fullPrompt)
                response.text ?: "I'm having trouble analyzing the file. Please make sure the file contains readable text content."
                
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating text with file and context", e)
                "I encountered an error while analyzing the file. Please make sure the file contains readable text content and try again."
            }
        }
    }
}
