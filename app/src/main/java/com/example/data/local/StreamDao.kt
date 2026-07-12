package com.example.data.local

import androidx.room.*
import com.example.data.model.StreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {
    @Query("SELECT * FROM streams WHERE eventId = :eventId")
    fun getStreamsForEvent(eventId: Int): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams WHERE eventId = :eventId")
    suspend fun getStreamsForEventSync(eventId: Int): List<StreamEntity>

    @Query("SELECT * FROM streams WHERE id = :id")
    suspend fun getStreamById(id: Int): StreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamEntity): Long

    @Update
    suspend fun updateStream(stream: StreamEntity)

    @Delete
    suspend fun deleteStream(stream: StreamEntity)

    @Query("DELETE FROM streams WHERE id = :id")
    suspend fun deleteStreamById(id: Int)

    @Query("DELETE FROM streams WHERE eventId = :eventId")
    suspend fun deleteStreamsForEvent(eventId: Int)

    @Query("DELETE FROM streams")
    suspend fun clearAllStreams()
}
