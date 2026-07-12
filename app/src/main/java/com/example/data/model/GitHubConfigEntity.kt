package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "github_config")
data class GitHubConfigEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "",
    val repository: String = "",
    val token: String = "",
    val isSyncEnabled: Boolean = false
)
