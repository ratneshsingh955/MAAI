package com.ratnesh.singh.myai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ratnesh.singh.myai.adapter.ChatAdapter
import com.ratnesh.singh.myai.ai.GeminiFireBaseAiService
import com.ratnesh.singh.myai.model.Message
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var geminiService: GeminiFireBaseAiService
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var etMessageInput: TextInputEditText
    private lateinit var markwon: Markwon
    private var uploadedImageUri: Uri? = null
    private var isWaitingForImagePrompt: Boolean = false
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            handleImageSelection(selectedImageUri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        initializeViews()
        setupFirebaseAuth()
        setupGeminiService()
        setupMarkwon()
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
    
    private fun setupMarkwon() {
        markwon = Markwon.builder(this)
            .usePlugin(TablePlugin.create(this))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(this))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(markwon)
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvChatMessages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@WelcomeActivity)
            adapter = chatAdapter
        }
        // Set the recyclerView reference in the adapter for auto-scrolling
        chatAdapter.setRecyclerView(recyclerView)
    }
    
    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            // Add button press animation
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
            sendMessage()
        }
        
        findViewById<ImageButton>(R.id.btnAttachment).setOnClickListener {
            // Add button press animation
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
            openImagePicker()
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
    
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun handleImageSelection(imageUri: Uri) {
        // Store the uploaded image URI
        uploadedImageUri = imageUri
        isWaitingForImagePrompt = true
        
        // Add user message with image to chat
        val userMessage = Message(
            text = "Image",
            isFromUser = true,
            imageUri = imageUri
        )
        chatAdapter.addMessage(userMessage)
        
        // Clear input and update hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "What would you like to know about this image?"
        
        // AI asks for specific prompt about the image
        val aiPromptMessage = Message(
            text = "I can see you've uploaded an image! What specific question do you have about it? Please describe what you'd like me to analyze or explain.",
            isFromUser = false
        )
        chatAdapter.addMessage(aiPromptMessage)
    }
    
    private fun sendMessage() {
        val messageText = etMessageInput.text?.toString()?.trim()
        if (!messageText.isNullOrEmpty()) {
            if (isWaitingForImagePrompt && uploadedImageUri != null) {
                // User is providing a prompt for the uploaded image
                handleImagePrompt(messageText, uploadedImageUri!!)
            } else {
                // Regular text message
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
    }
    
    private fun handleImagePrompt(userPrompt: String, imageUri: Uri) {
        // Add user's prompt to chat
        val userMessage = Message(
            text = userPrompt,
            isFromUser = true
        )
        chatAdapter.addMessage(userMessage)
        
        // Clear input and reset hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "Type your message..."
        
        // Reset image prompt state
        isWaitingForImagePrompt = false
        uploadedImageUri = null
        
        // Show typing indicator
        val typingMessage = Message(
            text = "AI is analyzing the image...",
            isFromUser = false
        )
        chatAdapter.addMessage(typingMessage)
        
        // Generate AI response for image with user's custom prompt
        generateAIResponseForImage(userPrompt, imageUri)
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
    
    private fun generateAIResponseForImage(userMessage: String, imageUri: Uri) {
        lifecycleScope.launch {
            try {
                val aiResponse = geminiService.generateTextWithImage(userMessage, imageUri, this@WelcomeActivity)
                
                // Remove typing indicator
                val messages = chatAdapter.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().text == "AI is analyzing the image...") {
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
                if (messages.isNotEmpty() && messages.last().text == "AI is analyzing the image...") {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.messages.clear()
                    chatAdapter.messages.addAll(messages)
                    chatAdapter.notifyDataSetChanged()
                }
                
                // Add error message
                val errorMessage = Message(
                    text = "Sorry, I couldn't analyze the image. Please try again.",
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
