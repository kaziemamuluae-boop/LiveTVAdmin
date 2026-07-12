package com.example.data.local

import androidx.room.*
import com.example.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Int): EventEntity?

    @Query("SELECT * FROM events WHERE isFavorite = 1 ORDER BY date ASC, time ASC")
    fun getFavoriteEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE category = :category ORDER BY date ASC, time ASC")
    fun getEventsByCategory(category: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Int)

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()
}
