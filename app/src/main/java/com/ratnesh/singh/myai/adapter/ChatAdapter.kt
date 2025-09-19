package com.ratnesh.singh.myai.adapter

import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ratnesh.singh.myai.ImageViewerActivity
import com.ratnesh.singh.myai.R
import com.ratnesh.singh.myai.model.Message
import io.noties.markwon.Markwon

class ChatAdapter(private val markwon: Markwon) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    val messages = mutableListOf<Message>()
    private var recyclerView: RecyclerView? = null
    
    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }
    
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }
    
    fun addMessages(newMessages: List<Message>) {
        val startPosition = messages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(startPosition, newMessages.size)
        scrollToBottom()
    }
    
    private fun scrollToBottom() {
        recyclerView?.post {
            if (messages.isNotEmpty()) {
                recyclerView?.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isFromUser && message.imageUri != null -> VIEW_TYPE_USER_IMAGE
            !message.isFromUser && message.imageUri != null -> VIEW_TYPE_AI_IMAGE
            message.isFromUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_message_user
            VIEW_TYPE_AI -> R.layout.item_message_ai
            VIEW_TYPE_USER_IMAGE -> R.layout.item_message_user_image
            VIEW_TYPE_AI_IMAGE -> R.layout.item_message_ai_image
            else -> R.layout.item_message_user
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view, viewType)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class MessageViewHolder(itemView: View, private val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private val tvMessageText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val ivMessageImage: ImageView? = itemView.findViewById(R.id.ivMessageImage)
        
        fun bind(message: Message) {
            // Handle text content
            if (!message.isFromUser) {
                markwon.setMarkdown(tvMessageText, message.text)
            } else {
                tvMessageText.text = message.text
            }
            
            // Handle image if present
            message.imageUri?.let { imageUri ->
                ivMessageImage?.let { imageView ->
                    Glide.with(itemView.context)
                        .load(imageUri)
                        .into(imageView)
                    
                    // Set click listener to open full screen
                    imageView.setOnClickListener {
                        val intent = Intent(itemView.context, ImageViewerActivity::class.java)
                        intent.putExtra("image_uri", imageUri)
                        itemView.context.startActivity(intent)
                    }
                }
            }
            
            // Add subtle animation for new messages
            itemView.alpha = 0f
            itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_USER_IMAGE = 3
        private const val VIEW_TYPE_AI_IMAGE = 4
    }
}
