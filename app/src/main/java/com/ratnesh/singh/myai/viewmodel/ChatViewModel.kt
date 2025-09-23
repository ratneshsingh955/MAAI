package com.ratnesh.singh.myai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ratnesh.singh.myai.database.entity.Conversation
import com.ratnesh.singh.myai.model.Message
import com.ratnesh.singh.myai.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    // Current conversation messages
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    // All conversations
    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    // Current conversation
    private val _currentConversation = MutableLiveData<Conversation?>()
    val currentConversation: LiveData<Conversation?> = _currentConversation

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadConversations()
    }

    // Load all conversations
    private fun loadConversations() {
        viewModelScope.launch {
            repository.getAllConversations().collect { conversationsList ->
                _conversations.postValue(conversationsList)
            }
        }
    }

    // Start a new conversation
    fun startNewConversation(title: String = "New Chat") {
        viewModelScope.launch {
            try {
                val conversationId = repository.startNewConversation(title)
                val conversation = repository.getConversationById(conversationId)
                _currentConversation.value = conversation
                loadMessagesForCurrentConversation()
            } catch (e: Exception) {
                _error.value = "Failed to start new conversation: ${e.message}"
            }
        }
    }

    // Load a specific conversation
    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                repository.setCurrentConversation(conversationId)
                val conversation = repository.getConversationById(conversationId)
                _currentConversation.value = conversation
                loadMessagesForCurrentConversation()
            } catch (e: Exception) {
                _error.value = "Failed to load conversation: ${e.message}"
            }
        }
    }

    // Load messages for current conversation
    private fun loadMessagesForCurrentConversation() {
        val conversationId = repository.getCurrentConversationId() ?: return

        viewModelScope.launch {
            repository.getMessagesForConversation(conversationId).collect { messagesList ->
                _messages.postValue(messagesList)
            }
        }
    }

    // Send a text message
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val conversationId = repository.getCurrentConversationId()
        if (conversationId == null) {
            startNewConversation()
            return
        }

        viewModelScope.launch {
            try {
                // Check if this is the first user message in the conversation
                val currentMessages = _messages.value ?: emptyList()
                val isFirstUserMessage = currentMessages.none { it.isFromUser }

                // Add user message
                val userMessage = Message(
                    text = text,
                    isFromUser = true,
                    conversationId = conversationId
                )
                repository.addMessage(userMessage)

                // Update conversation title if this is the first user message
                if (isFirstUserMessage) {
                    repository.updateConversationTitleFromFirstMessage(conversationId, text)
                    loadConversations() // Refresh conversation list
                }

                // Generate AI response
                _isLoading.value = true
                val aiResponse = repository.generateAIResponse(text, context = getApplication())

                // Add AI response
                val aiMessage = Message(
                    text = aiResponse,
                    isFromUser = false,
                    conversationId = conversationId
                )
                repository.addMessage(aiMessage)

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    // Send a message with image
    fun sendMessageWithImage(text: String, imageUri: Uri) {
        if (text.isBlank()) return

        val conversationId = repository.getCurrentConversationId()
        if (conversationId == null) {
            startNewConversation()
            return
        }

        viewModelScope.launch {
            try {
                // Check if this is the first user message in the conversation
                val currentMessages = _messages.value ?: emptyList()
                val isFirstUserMessage = currentMessages.none { it.isFromUser }

                // Add user image message
                val userImageMessage = Message(
                    text = "Image",
                    isFromUser = true,
                    imageUri = imageUri,
                    conversationId = conversationId
                )
                repository.addMessage(userImageMessage)

                // Add user text message
                val userMessage = Message(
                    text = text,
                    isFromUser = true,
                    conversationId = conversationId
                )
                repository.addMessage(userMessage)

                // Update conversation title if this is the first user message
                if (isFirstUserMessage) {
                    repository.updateConversationTitleFromFirstMessage(conversationId, text)
                    loadConversations() // Refresh conversation list
                }

                // Generate AI response
                _isLoading.value = true
                val aiResponse = repository.generateAIResponse(text, imageUri = imageUri, context = getApplication())

                // Add AI response
                val aiMessage = Message(
                    text = aiResponse,
                    isFromUser = false,
                    conversationId = conversationId
                )
                repository.addMessage(aiMessage)

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to send image message: ${e.message}"
            }
        }
    }

    // Send a message with file
    fun sendMessageWithFile(text: String, fileUri: Uri) {
        if (text.isBlank()) return

        val conversationId = repository.getCurrentConversationId()
        if (conversationId == null) {
            startNewConversation()
            return
        }

        viewModelScope.launch {
            try {
                // Check if this is the first user message in the conversation
                val currentMessages = _messages.value ?: emptyList()
                val isFirstUserMessage = currentMessages.none { it.isFromUser }

                // Add user file message
                val userFileMessage = Message(
                    text = "File",
                    isFromUser = true,
                    fileUri = fileUri,
                    conversationId = conversationId
                )
                repository.addMessage(userFileMessage)

                // Add user text message
                val userMessage = Message(
                    text = text,
                    isFromUser = true,
                    conversationId = conversationId
                )
                repository.addMessage(userMessage)

                // Update conversation title if this is the first user message
                if (isFirstUserMessage) {
                    repository.updateConversationTitleFromFirstMessage(conversationId, text)
                    loadConversations() // Refresh conversation list
                }

                // Generate AI response
                _isLoading.value = true
                val aiResponse = repository.generateAIResponse(text, fileUri = fileUri, context = getApplication())

                // Add AI response
                val aiMessage = Message(
                    text = aiResponse,
                    isFromUser = false,
                    conversationId = conversationId
                )
                repository.addMessage(aiMessage)

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to send file message: ${e.message}"
            }
        }
    }

    // Delete conversation
    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(conversationId)
                if (_currentConversation.value?.id == conversationId) {
                    _currentConversation.value = null
                    _messages.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete conversation: ${e.message}"
            }
        }
    }

    // Update conversation title
    fun updateConversationTitle(conversationId: Long, title: String) {
        viewModelScope.launch {
            try {
                repository.updateConversationTitle(conversationId, title)
            } catch (e: Exception) {
                _error.value = "Failed to update conversation title: ${e.message}"
            }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }
}
