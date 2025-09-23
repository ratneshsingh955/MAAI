package com.ratnesh.singh.myai.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val sender: String, // "user" or "ai"
    val text: String,
    val imageUri: String? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
