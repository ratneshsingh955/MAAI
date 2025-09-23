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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ratnesh.singh.myai.adapter.ChatAdapter
import com.ratnesh.singh.myai.model.Message
import com.ratnesh.singh.myai.viewmodel.ChatViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var viewModel: ChatViewModel
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
        setupViewModel()
        setupMarkwon()
        setupRecyclerView()
        setupClickListeners()
        observeData()
        
        // Check if we're loading an existing conversation
        val conversationId = intent.getLongExtra("conversation_id", -1L)
        if (conversationId != -1L) {
            viewModel.loadConversation(conversationId)
        } else {
            // Start a new conversation
            viewModel.startNewConversation()
        }
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

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
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

    private fun observeData() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                chatAdapter.showLoading()
            } else {
                chatAdapter.hideLoading()
            }
        }
        
        viewModel.conversations.observe(this) { conversations ->
            // Conversations list updated - this will refresh the conversation list
            // when titles are updated
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
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
                viewModel.sendMessage(messageText)
                etMessageInput.text?.clear()
            }
        }
    }

    private fun handleImagePrompt(userPrompt: String, imageUri: Uri) {
        // Hide the image preview card
        imagePreviewCard.visibility = View.GONE

        // Clear input and reset hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "Type your message..."

        // Reset image prompt state
        isWaitingForImagePrompt = false
        uploadedImageUri = null

        // Send message with image using ViewModel
        viewModel.sendMessageWithImage(userPrompt, imageUri)
    }

    private fun handleFilePrompt(userPrompt: String, fileUri: Uri) {
        // Hide the file preview card
        imagePreviewCard.visibility = View.GONE

        // Clear input and reset hint
        etMessageInput.text?.clear()
        etMessageInput.hint = "Type your message..."

        // Reset file prompt state
        isWaitingForFilePrompt = false
        uploadedFileUri = null

        // Send message with file using ViewModel
        viewModel.sendMessageWithFile(userPrompt, fileUri)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_welcome, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_chat_history -> {
                openChatHistory()
                true
            }
            R.id.action_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun openChatHistory() {
        val intent = Intent(this, ConversationListActivity::class.java)
        startActivity(intent)
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
