package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val team1Name: String,
    val team1Flag: String,
    val team2Name: String,
    val team2Flag: String,
    val status: String, // "LIVE", "UPCOMING", "FINISHED"
    val date: String,
    val time: String,
    val category: String, // "Football", "Cricket", "Basketball", "Tennis", "Esports", "Others"
    val league: String,
    val round: String,
    val isFavorite: Boolean = false
)
