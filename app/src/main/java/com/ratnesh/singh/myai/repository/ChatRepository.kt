package com.ratnesh.singh.myai.repository

import android.content.Context
import android.net.Uri
import com.ratnesh.singh.myai.ai.GeminiFireBaseAiService
import com.ratnesh.singh.myai.database.ChatDatabase
import com.ratnesh.singh.myai.database.entity.Conversation
import com.ratnesh.singh.myai.database.entity.MessageEntity
import com.ratnesh.singh.myai.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class ChatRepository(context: Context) {
    
    private val database = ChatDatabase.getDatabase(context)
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val geminiService = GeminiFireBaseAiService()
    
    // Maximum number of recent messages to include in context
    private val maxContextMessages = 10
    
    // Current conversation ID
    private var currentConversationId: Long? = null
    
    // Get all conversations
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
    }
    
    // Get messages for a specific conversation
    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
            .map { entities -> entities.map { Message.fromEntity(it) } }
    }
    
    // Start a new conversation
    suspend fun startNewConversation(title: String = "New Chat"): Long {
        val conversation = Conversation(
            title = title,
            createdAt = System.currentTimeMillis(),
            lastMessageAt = System.currentTimeMillis()
        )
        val conversationId = conversationDao.insertConversation(conversation)
        currentConversationId = conversationId
        return conversationId
    }
    
    // Generate conversation title from first user message
    private fun generateConversationTitle(firstMessage: String): String {
        return when {
            firstMessage.length <= 30 -> firstMessage
            firstMessage.contains("?") -> {
                // For questions, take the first part before the question mark
                firstMessage.substring(0, minOf(30, firstMessage.indexOf("?"))) + "..."
            }
            firstMessage.contains(".") -> {
                // For statements, take the first sentence
                val firstSentence = firstMessage.substring(0, firstMessage.indexOf("."))
                if (firstSentence.length <= 30) firstSentence else firstSentence.substring(0, 27) + "..."
            }
            else -> {
                // For other cases, take first 30 characters
                firstMessage.substring(0, 30) + "..."
            }
        }.trim()
    }
    
    // Update conversation title based on first user message
    suspend fun updateConversationTitleFromFirstMessage(conversationId: Long, firstMessage: String) {
        val title = generateConversationTitle(firstMessage)
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(title = title))
        }
    }
    
    // Set current conversation
    fun setCurrentConversation(conversationId: Long) {
        currentConversationId = conversationId
    }
    
    // Get current conversation ID
    fun getCurrentConversationId(): Long? = currentConversationId
    
    // Add a message to the current conversation
    suspend fun addMessage(message: Message): Long {
        val conversationId = currentConversationId ?: throw IllegalStateException("No active conversation")
        val messageEntity = message.copy(conversationId = conversationId).toEntity()
        val messageId = messageDao.insertMessage(messageEntity)
        
        // Update conversation's last message time
        conversationDao.updateLastMessageTime(conversationId, System.currentTimeMillis())
        
        return messageId
    }
    
    // Generate AI response with conversation context
    suspend fun generateAIResponse(
        userMessage: String,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        context: Context
    ): String {
        val conversationId = currentConversationId ?: throw IllegalStateException("No active conversation")
        
        // Get recent conversation context
        val recentMessages = messageDao.getRecentMessages(conversationId, maxContextMessages)
        
        // Build context for Gemini
        val contextMessages = buildContextForGemini(recentMessages)
        
        return when {
            imageUri != null -> {
                geminiService.generateTextWithImageAndContext(userMessage, imageUri, contextMessages, context)
            }
            fileUri != null -> {
                geminiService.generateTextWithFileAndContext(userMessage, fileUri, contextMessages, context)
            }
            else -> {
                geminiService.generateTextWithContext(userMessage, contextMessages)
            }
        }
    }
    
    // Build context string for Gemini
    private fun buildContextForGemini(messages: List<MessageEntity>): String {
        if (messages.isEmpty()) return ""
        
        val contextBuilder = StringBuilder()
        contextBuilder.append("Previous conversation context:\n")
        
        messages.forEach { message ->
            val sender = if (message.sender == "user") "User" else "Assistant"
            contextBuilder.append("$sender: ${message.text}\n")
        }
        
        contextBuilder.append("\nCurrent conversation continues...\n")
        return contextBuilder.toString()
    }
    
    // Delete conversation
    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversationById(conversationId)
        if (currentConversationId == conversationId) {
            currentConversationId = null
        }
    }
    
    // Update conversation title
    suspend fun updateConversationTitle(conversationId: Long, title: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(title = title))
        }
    }
    
    // Get conversation by ID
    suspend fun getConversationById(conversationId: Long): Conversation? {
        return conversationDao.getConversationById(conversationId)
    }
}
