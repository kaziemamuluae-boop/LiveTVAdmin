package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streams")
data class StreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int, // Foreign key linking to EventEntity.id
    val quality: String, // "FHD", "HD", "SD"
    val serverName: String, // e.g., "Server 1"
    val streamUrl: String,
    val linkType: String = "HLS",
    val clearKeyId: String = "",
    val clearKey: String = ""
)
