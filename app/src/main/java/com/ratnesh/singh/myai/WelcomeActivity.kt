package com.ratnesh.singh.myai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
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
    private lateinit var toolbar: MaterialToolbar
    private lateinit var imagePreviewCard: MaterialCardView
    private lateinit var ivUploadedImage: ImageView
    private lateinit var btnRemoveImage: ImageButton
    private lateinit var tvPreviewTitle: TextView
    private lateinit var tvPreviewSubtitle: TextView
    private var uploadedImageUri: Uri? = null
    private var uploadedFileUri: Uri? = null
    private var isWaitingForImagePrompt: Boolean = false
    private var isWaitingForFilePrompt: Boolean = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            handleImageSelection(selectedImageUri)
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedFileUri ->
            handleFileSelection(selectedFileUri)
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
     //   toolbar = findViewById(R.id.toolbar)
        etMessageInput = findViewById(R.id.etMessageInput)
        imagePreviewCard = findViewById(R.id.imagePreviewCard)
        ivUploadedImage = findViewById(R.id.ivUploadedImage)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle)
        tvPreviewSubtitle = findViewById(R.id.tvPreviewSubtitle)

        // Set up toolbar
    //    setSupportActionBar(toolbar)
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
            showAttachmentMenu()
        }

        etMessageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // Image preview click to open full screen
        ivUploadedImage.setOnClickListener {
            uploadedImageUri?.let { uri ->
                val intent = Intent(this, ImageViewerActivity::class.java)
                intent.putExtra("image_uri", uri)
                startActivity(intent)
            }
        }

        // Remove image button
        btnRemoveImage.setOnClickListener {
            removeUploadedImage()
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

    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    private fun showAttachmentMenu() {
        val options = arrayOf("Upload Image", "Upload File")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Choose attachment type")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openImagePicker()
                1 -> openFilePicker()
            }
        }
        builder.show()
    }

    private fun handleImageSelection(imageUri: Uri) {
        // Store the uploaded image URI
        uploadedImageUri = imageUri
        isWaitingForImagePrompt = true

        // Show image in preview card
        showImagePreview(imageUri)

        // Clear input and update hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "What would you like to know about this image?"
    }

    private fun showImagePreview(imageUri: Uri) {
        // Load image using Glide
        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(ivUploadedImage)

        // Show the preview card
        imagePreviewCard.visibility = View.VISIBLE
    }

    private fun handleFileSelection(fileUri: Uri) {
        // Store the uploaded file URI
        uploadedFileUri = fileUri
        isWaitingForFilePrompt = true

        // Get file name
        val fileName = getFileName(fileUri)

        // Show file in preview card
        showFilePreview(fileUri, fileName)

        // Clear input and update hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "What would you like to know about this file?"
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else "Unknown file"
            } else "Unknown file"
        } ?: "Unknown file"
    }

    private fun showFilePreview(fileUri: Uri, fileName: String) {
        // Update the preview card for file
        imagePreviewCard.visibility = View.VISIBLE

        // Set file icon instead of image
        ivUploadedImage.setImageResource(R.drawable.ic_attachment)

        // Update the text to show file name
        tvPreviewTitle.text = "File uploaded"
        tvPreviewSubtitle.text = fileName
    }

    private fun removeUploadedImage() {
        // Hide the preview card
        imagePreviewCard.visibility = View.GONE

        // Clear the uploaded image/file
        uploadedImageUri = null
        uploadedFileUri = null
        isWaitingForImagePrompt = false
        isWaitingForFilePrompt = false

        // Reset input hint
        etMessageInput.hint = "Type your message..."
        etMessageInput.text?.clear()
    }

    private fun sendMessage() {
        val messageText = etMessageInput.text?.toString()?.trim()
        if (!messageText.isNullOrEmpty()) {
            if (isWaitingForImagePrompt && uploadedImageUri != null) {
                // User is providing a prompt for the uploaded image
                handleImagePrompt(messageText, uploadedImageUri!!)
            } else if (isWaitingForFilePrompt && uploadedFileUri != null) {
                // User is providing a prompt for the uploaded file
                handleFilePrompt(messageText, uploadedFileUri!!)
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
                    text = "MAAI is typing...",
                    isFromUser = false
                )
                chatAdapter.addMessage(typingMessage)

                // Generate AI response
                generateAIResponseForText(messageText)
            }
        }
    }

    private fun handleImagePrompt(userPrompt: String, imageUri: Uri) {
        // Add user's image message to chat first
        val userImageMessage = Message(
            text = "Image",
            isFromUser = true,
            imageUri = imageUri
        )
        chatAdapter.addMessage(userImageMessage)

        // Add user's prompt to chat
        val userMessage = Message(
            text = userPrompt,
            isFromUser = true
        )
        chatAdapter.addMessage(userMessage)

        // Hide the image preview card
        imagePreviewCard.visibility = View.GONE

        // Clear input and reset hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "Type your message..."

        // Reset image prompt state
        isWaitingForImagePrompt = false
        uploadedImageUri = null

        // Show typing indicator
        val typingMessage = Message(
            text = "MAAI is analyzing the image and your question...",
            isFromUser = false
        )
        chatAdapter.addMessage(typingMessage)

        // Generate AI response for image with user's custom prompt
        generateAIResponseForImage(userPrompt, imageUri)
    }

    private fun handleFilePrompt(userPrompt: String, fileUri: Uri) {
        // Add user's file message to chat first
        val fileName = getFileName(fileUri)
        val userFileMessage = Message(
            text = "File: $fileName",
            isFromUser = true,
            fileUri = fileUri
        )
        chatAdapter.addMessage(userFileMessage)

        // Add user's prompt to chat
        val userMessage = Message(
            text = userPrompt,
            isFromUser = true
        )
        chatAdapter.addMessage(userMessage)

        // Hide the file preview card
        imagePreviewCard.visibility = View.GONE

        // Clear input and reset hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "Type your message..."

        // Reset file prompt state
        isWaitingForFilePrompt = false
        uploadedFileUri = null

        // Show typing indicator
        val typingMessage = Message(
            text = "MAAI is analyzing the file and your question...",
            isFromUser = false
        )
        chatAdapter.addMessage(typingMessage)

        // Generate AI response for file with user's custom prompt
        generateAIResponseForFile(userPrompt, fileUri)
    }

    private fun generateAIResponseForText(userMessage: String) {
        lifecycleScope.launch {
            try {
                val aiResponse = geminiService.generateText(userMessage)

                // Remove typing indicator
                val messages = chatAdapter.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().text == "MAAI is typing...") {
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
                if (messages.isNotEmpty() && messages.last().text == "MAAI is typing...") {
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
                if (messages.isNotEmpty() && messages.last().text == "MAAI is analyzing the image and your question...") {
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
                if (messages.isNotEmpty() && messages.last().text == "MAAI is analyzing the image and your question...") {
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

    private fun generateAIResponseForFile(userMessage: String, fileUri: Uri) {
        lifecycleScope.launch {
            try {
                val aiResponse = geminiService.generateTextWithFile(userMessage, fileUri, this@WelcomeActivity)

                // Remove typing indicator
                val messages = chatAdapter.messages.toMutableList()
                if (messages.isNotEmpty() && messages.last().text == "MAAI is analyzing the file and your question...") {
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
                if (messages.isNotEmpty() && messages.last().text == "MAAI is analyzing the file and your question...") {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.messages.clear()
                    chatAdapter.messages.addAll(messages)
                    chatAdapter.notifyDataSetChanged()
                }

                // Add error message
                val errorMessage = Message(
                    text = "Sorry, I couldn't analyze the file. Please try again.",
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
