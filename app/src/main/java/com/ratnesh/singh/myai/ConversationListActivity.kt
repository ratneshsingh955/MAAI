package com.ratnesh.singh.myai

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ratnesh.singh.myai.adapter.ConversationAdapter
import com.ratnesh.singh.myai.database.entity.Conversation
import com.ratnesh.singh.myai.viewmodel.ChatViewModel

class ConversationListActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_list)

//        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        observeData()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat History"
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            onConversationClick = { conversation ->
                openConversation(conversation)
            },
            onConversationDelete = { conversation ->
                showDeleteDialog(conversation)
            }
        )

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvConversations)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConversationListActivity)
            adapter = conversationAdapter
        }
    }

    private fun observeData() {
        viewModel.conversations.observe(this) { conversations ->
            conversationAdapter.submitList(conversations)
        }
    }

    private fun openConversation(conversation: Conversation) {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.putExtra("conversation_id", conversation.id)
        startActivity(intent)
    }

    private fun showDeleteDialog(conversation: Conversation) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteConversation(conversation.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_conversation_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_new_chat -> {
                startNewChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startNewChat() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
    }
}
