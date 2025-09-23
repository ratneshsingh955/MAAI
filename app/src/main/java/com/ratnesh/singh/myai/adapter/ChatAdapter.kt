package com.ratnesh.singh.myai.adapter

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ratnesh.singh.myai.ImageViewerActivity
import com.ratnesh.singh.myai.R
import com.ratnesh.singh.myai.model.Message
import io.noties.markwon.Markwon

class ChatAdapter(private val markwon: Markwon) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    private val messages = mutableListOf<Message>()
    private var recyclerView: RecyclerView? = null
    private var isLoading = false
    
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
    
    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
        scrollToBottom()
    }
    
    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }
    
    fun showLoading() {
        if (!isLoading) {
            isLoading = true
            notifyItemInserted(messages.size)
            scrollToBottom()
        }
    }
    
    fun hideLoading() {
        if (isLoading) {
            isLoading = false
            notifyItemRemoved(messages.size)
        }
    }
    
    private fun scrollToBottom() {
        recyclerView?.post {
            val itemCount = if (isLoading) messages.size + 1 else messages.size
            if (itemCount > 0) {
                recyclerView?.smoothScrollToPosition(itemCount - 1)
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        // Check if this is the loading indicator
        if (isLoading && position == messages.size) {
            return VIEW_TYPE_AI_LOADING
        }
        
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
            VIEW_TYPE_AI_LOADING -> R.layout.item_message_ai_loading
            else -> R.layout.item_message_user
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view, viewType)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        if (isLoading && position == messages.size) {
            holder.bindLoading()
        } else {
            holder.bind(messages[position])
        }
    }
    
    override fun getItemCount(): Int = messages.size + if (isLoading) 1 else 0
    
    inner class MessageViewHolder(itemView: View, private val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private val tvMessageText: TextView? = if (viewType != VIEW_TYPE_AI_LOADING) itemView.findViewById(R.id.tvMessageText) else null
        private val ivMessageImage: ImageView? = if (viewType != VIEW_TYPE_AI_LOADING) itemView.findViewById(R.id.ivMessageImage) else null
        private val dot1: View? = if (viewType == VIEW_TYPE_AI_LOADING) itemView.findViewById(R.id.dot1) else null
        private val dot2: View? = if (viewType == VIEW_TYPE_AI_LOADING) itemView.findViewById(R.id.dot2) else null
        private val dot3: View? = if (viewType == VIEW_TYPE_AI_LOADING) itemView.findViewById(R.id.dot3) else null
        
        fun bindLoading() {
            if (viewType == VIEW_TYPE_AI_LOADING) {
                startDotAnimation()
            }
        }
        
        private fun startDotAnimation() {
            dot1?.let { startDotPulseAnimation(it, 0) }
            dot2?.let { startDotPulseAnimation(it, 200) }
            dot3?.let { startDotPulseAnimation(it, 400) }
        }
        
        private fun startDotPulseAnimation(dot: View, delay: Long) {
            val animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1.0f)
            animator.duration = 600
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.repeatMode = ObjectAnimator.REVERSE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.startDelay = delay
            animator.start()
        }
        
        fun bind(message: Message) {
            // Handle text content
            tvMessageText?.let { textView ->
                if (!message.isFromUser) {
                    markwon.setMarkdown(textView, message.text)
                } else {
                    textView.text = message.text
                }
            }
            
            // Handle image if present
            message.imageUri?.let { imageUri ->
                ivMessageImage?.let { imageView ->
                    Log.d("ChatAdapter", "Loading image in chat: $imageUri")
                    Glide.with(itemView.context)
                        .load(imageUri)
                        .into(imageView)
                    
                    // Set click listener to open full screen
                    imageView.setOnClickListener {
                        Log.d("ChatAdapter", "Image clicked, opening full screen: $imageUri")
                        val intent = Intent(itemView.context, ImageViewerActivity::class.java)
                        intent.putExtra("image_uri", imageUri)
                        itemView.context.startActivity(intent)
                    }
                } ?: run {
                    Log.e("ChatAdapter", "ImageView is null for image message")
                }
            } ?: run {
                Log.d("ChatAdapter", "No image URI for message: ${message.text}")
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
        private const val VIEW_TYPE_AI_LOADING = 5
    }
}
