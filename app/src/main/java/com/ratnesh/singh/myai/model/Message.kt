package com.ratnesh.singh.myai.model

import android.net.Uri
import com.ratnesh.singh.myai.database.entity.MessageEntity

data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: Uri? = null,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val id: Long = 0,
    val conversationId: Long = 0
) {
    companion object {
        fun fromEntity(entity: MessageEntity): Message {
            return Message(
                id = entity.id,
                conversationId = entity.conversationId,
                text = entity.text,
                isFromUser = entity.sender == "user",
                timestamp = entity.timestamp,
                imageUri = entity.imageUri?.let { Uri.parse(it) },
                fileUri = entity.fileUri?.let { Uri.parse(it) },
                fileName = entity.fileName,
                fileType = entity.fileType
            )
        }
    }
    
    fun toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            text = text,
            sender = if (isFromUser) "user" else "ai",
            imageUri = imageUri?.toString(),
            fileUri = fileUri?.toString(),
            fileName = fileName,
            fileType = fileType,
            timestamp = timestamp
        )
    }
}
