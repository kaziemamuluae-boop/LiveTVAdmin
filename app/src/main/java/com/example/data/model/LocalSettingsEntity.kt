package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_settings")
data class LocalSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true,
    val isAutoRefresh: Boolean = true,
    val isNotificationsEnabled: Boolean = true,
    val isPlayerMuted: Boolean = false,
    val playerVolume: Float = 0.8f,
    val playerBrightness: Float = 0.8f,
    val playbackSpeed: Float = 1.0f
)
