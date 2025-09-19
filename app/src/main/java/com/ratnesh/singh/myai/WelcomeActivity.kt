package com.ratnesh.singh.myai

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ratnesh.singh.myai.adapter.ChatAdapter
import com.ratnesh.singh.myai.ai.GeminiFireBaseAiService
import com.ratnesh.singh.myai.model.Message
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var geminiService: GeminiFireBaseAiService
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var etMessageInput: TextInputEditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        initializeViews()
        setupFirebaseAuth()
        setupGeminiService()
        setupRecyclerView()
        setupClickListeners()
        addWelcomeMessage()
    }
    
    private fun initializeViews() {
        etMessageInput = findViewById(R.id.etMessageInput)
    }
    
    private fun setupFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
    }
    
    private fun setupGeminiService() {
        geminiService = GeminiFireBaseAiService()
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvChatMessages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@WelcomeActivity)
            adapter = chatAdapter
        }
    }
    
    private fun setupClickListeners() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSend).setOnClickListener {
            sendMessage()
        }
        
        etMessageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }
    
    private fun addWelcomeMessage() {
        val welcomeMessage = Message(
            text = "Hello! I'm your AI assistant. How can I help you today?",
            isFromUser = false
        )
        chatAdapter.addMessage(welcomeMessage)
    }
    
    private fun sendMessage() {
        val messageText = etMessageInput.text?.toString()?.trim()
        if (!messageText.isNullOrEmpty()) {
            // Add user message to chat
            val userMessage = Message(
                text = messageText,
                isFromUser = true
            )
            chatAdapter.addMessage(userMessage)
            
            // Clear input
            etMessageInput.text?.clear()
            
            // Show typing indicator
            val typingMessage = Message(
                text = "AI is typing...",
                isFromUser = false
            )
            chatAdapter.addMessage(typingMessage)
            
            // Generate AI response
            generateAIResponse(messageText)
        }
    }
    
    private fun generateAIResponse(userMessage: String) {
        lifecycleScope.launch {
            try {
                val aiResponse = geminiService.generateText(userMessage)
                
                // Remove typing indicator
                val messages = chatAdapter.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().text == "AI is typing...") {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.messages.clear()
                    chatAdapter.messages.addAll(messages)
                    chatAdapter.notifyDataSetChanged()
                }
                
                // Add AI response
                val aiMessage = Message(
                    text = aiResponse,
                    isFromUser = false
                )
                chatAdapter.addMessage(aiMessage)
                
            } catch (e: Exception) {
                // Remove typing indicator
                val messages = chatAdapter.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().text == "AI is typing...") {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.messages.clear()
                    chatAdapter.messages.addAll(messages)
                    chatAdapter.notifyDataSetChanged()
                }
                
                // Add error message
                val errorMessage = Message(
                    text = "Sorry, I encountered an error. Please try again.",
                    isFromUser = false
                )
                chatAdapter.addMessage(errorMessage)
                
                Toast.makeText(this@WelcomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_welcome, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun signOut() {
        firebaseAuth.signOut()
        Toast.makeText(this, getString(R.string.sign_out_successful), Toast.LENGTH_SHORT).show()
        
        // Navigate back to SignInActivity
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
