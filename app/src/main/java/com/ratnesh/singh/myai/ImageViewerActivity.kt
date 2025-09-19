package com.ratnesh.singh.myai

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ImageViewerActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        val imageUri = intent.getParcelableExtra<Uri>("image_uri")
        val ivFullImage = findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        
        // Load the image
        imageUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .into(ivFullImage)
        }
        
        // Close button click
        btnClose.setOnClickListener {
            finish()
        }
        
        // Tap to close
        ivFullImage.setOnClickListener {
            finish()
        }
    }
}
