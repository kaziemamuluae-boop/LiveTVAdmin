package com.example.data.local

import androidx.room.*
import com.example.data.model.GitHubConfigEntity
import com.example.data.model.LocalSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM github_config WHERE id = 1")
    fun getGitHubConfig(): Flow<GitHubConfigEntity?>

    @Query("SELECT * FROM github_config WHERE id = 1")
    suspend fun getGitHubConfigSync(): GitHubConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGitHubConfig(config: GitHubConfigEntity)

    @Query("SELECT * FROM local_settings WHERE id = 1")
    fun getLocalSettings(): Flow<LocalSettingsEntity?>

    @Query("SELECT * FROM local_settings WHERE id = 1")
    suspend fun getLocalSettingsSync(): LocalSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalSettings(settings: LocalSettingsEntity)
}
