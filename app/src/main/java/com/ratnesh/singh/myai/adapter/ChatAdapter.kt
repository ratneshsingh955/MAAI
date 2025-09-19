package com.ratnesh.singh.myai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ratnesh.singh.myai.R
import com.ratnesh.singh.myai.model.Message

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    val messages = mutableListOf<Message>()
    
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun addMessages(newMessages: List<Message>) {
        val startPosition = messages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(startPosition, newMessages.size)
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isFromUser) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_AI
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_ai
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessageText: TextView = itemView.findViewById(R.id.tvMessageText)
        
        fun bind(message: Message) {
            tvMessageText.text = message.text
        }
    }
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }
}
