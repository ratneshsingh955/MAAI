package com.ratnesh.singh.myai.model

import android.net.Uri

data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: Uri? = null,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val fileType: String? = null
)
