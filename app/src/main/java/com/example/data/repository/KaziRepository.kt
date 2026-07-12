package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.EventDao
import com.example.data.local.SettingsDao
import com.example.data.local.StreamDao
import com.example.data.model.EventEntity
import com.example.data.model.GitHubConfigEntity
import com.example.data.model.LocalSettingsEntity
import com.example.data.model.StreamEntity
import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubUpdateRequest
import com.example.data.remote.RemoteEventJson
import com.example.data.remote.RemoteStreamJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class KaziRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val eventDao = db.eventDao()
    private val streamDao = db.streamDao()
    private val settingsDao = db.settingsDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GitHubApiService::class.java)

    // Local Data flows
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val favoriteEvents: Flow<List<EventEntity>> = eventDao.getFavoriteEvents()
    val gitHubConfig: Flow<GitHubConfigEntity?> = settingsDao.getGitHubConfig()
    val localSettings: Flow<LocalSettingsEntity?> = settingsDao.getLocalSettings()

    fun getStreamsForEvent(eventId: Int): Flow<List<StreamEntity>> = streamDao.getStreamsForEvent(eventId)
    fun getEventsByCategory(category: String): Flow<List<EventEntity>> = eventDao.getEventsByCategory(category)

    suspend fun getEventById(id: Int): EventEntity? = eventDao.getEventById(id)
    suspend fun getStreamById(id: Int): StreamEntity? = streamDao.getStreamById(id)
    suspend fun getStreamsForEventSync(eventId: Int): List<StreamEntity> = streamDao.getStreamsForEventSync(eventId)

    // Save Local Settings
    suspend fun saveLocalSettings(settings: LocalSettingsEntity) {
        settingsDao.saveLocalSettings(settings)
    }

    // Save GitHub Configuration
    suspend fun saveGitHubConfig(config: GitHubConfigEntity) {
        settingsDao.saveGitHubConfig(config)
    }

    // Local DB Operations (CRUD Events & Streams)
    suspend fun insertEvent(event: EventEntity): Long = eventDao.insertEvent(event)
    suspend fun updateEvent(event: EventEntity) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: EventEntity) {
        streamDao.deleteStreamsForEvent(event.id)
        eventDao.deleteEvent(event)
    }
    suspend fun deleteEventById(id: Int) {
        streamDao.deleteStreamsForEvent(id)
        eventDao.deleteEventById(id)
    }

    suspend fun insertStream(stream: StreamEntity): Long = streamDao.insertStream(stream)
    suspend fun updateStream(stream: StreamEntity) = streamDao.updateStream(stream)
    suspend fun deleteStream(stream: StreamEntity) = streamDao.deleteStream(stream)
    suspend fun deleteStreamById(id: Int) = streamDao.deleteStreamById(id)

    // Load Mock Data if Database is completely empty on first run
    suspend fun loadMockDataIfEmpty() {
        val currentEvents = allEvents.firstOrNull() ?: emptyList()
        if (currentEvents.isEmpty()) {
            val event1 = EventEntity(
                team1Name = "Manchester United",
                team1Flag = "https://media.api-sports.io/football/teams/33.png",
                team2Name = "Liverpool",
                team2Flag = "https://media.api-sports.io/football/teams/40.png",
                status = "LIVE",
                date = "2026-07-12",
                time = "18:00",
                category = "Football",
                league = "English Premier League",
                round = "Matchday 5"
            )
            val eventId1 = insertEvent(event1).toInt()
            insertStream(StreamEntity(eventId = eventId1, quality = "FHD", serverName = "Ultra Stream 1", streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"))
            insertStream(StreamEntity(eventId = eventId1, quality = "HD", serverName = "Backup Server", streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))

            val event2 = EventEntity(
                team1Name = "Los Angeles Lakers",
                team1Flag = "https://media.api-sports.io/basketball/teams/137.png",
                team2Name = "Golden State Warriors",
                team2Flag = "https://media.api-sports.io/basketball/teams/141.png",
                status = "UPCOMING",
                date = "2026-07-13",
                time = "20:00",
                category = "Basketball",
                league = "NBA",
                round = "Regular Season"
            )
            val eventId2 = insertEvent(event2).toInt()
            insertStream(StreamEntity(eventId = eventId2, quality = "HD", serverName = "Primary TV", streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"))

            // Save default configurations
            if (settingsDao.getLocalSettingsSync() == null) {
                settingsDao.saveLocalSettings(LocalSettingsEntity())
            }
            if (settingsDao.getGitHubConfigSync() == null) {
                settingsDao.saveGitHubConfig(GitHubConfigEntity())
            }
        }
    }

    // --- GitHub Synchronization Engine ---

    // Download/Fetch LiveEvents.json
    suspend fun downloadLiveEventsFromGitHub(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = settingsDao.getGitHubConfigSync() ?: return@withContext Result.failure(Exception("GitHub settings are not configured. Please go to GitHub Settings."))
            if (config.username.isEmpty() || config.repository.isEmpty()) {
                return@withContext Result.failure(Exception("GitHub configuration is incomplete. Please enter Username and Repository."))
            }

            val authHeader = if (config.token.isNotEmpty()) "token ${config.token}" else null
            val response = apiService.getFileContent(
                authHeader = authHeader,
                owner = config.username,
                repo = config.repository,
                path = "LiveEvents.json"
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub server returned: ${response.code()} ${response.message()}"))
            }

            val fileResponse = response.body() ?: return@withContext Result.failure(Exception("Repository file is empty or missing."))
            val contentEncoded = fileResponse.content ?: return@withContext Result.failure(Exception("No content found in LiveEvents.json."))

            // Decode base64
            val sanitizedBase64 = contentEncoded.replace("\\s".toRegex(), "")
            val decodedBytes = Base64.decode(sanitizedBase64, Base64.DEFAULT)
            val jsonString = String(decodedBytes, Charsets.UTF_8)

            // Parse json
            val listType = Types.newParameterizedType(List::class.java, RemoteEventJson::class.java)
            val adapter: JsonAdapter<List<RemoteEventJson>> = moshi.adapter(listType)
            val remoteEvents = adapter.fromJson(jsonString) ?: return@withContext Result.failure(Exception("Failed to parse JSON file structure."))

            // Clear and overwrite local DB tables
            eventDao.clearAllEvents()
            streamDao.clearAllStreams()

            for (remoteEvent in remoteEvents) {
                val event = EventEntity(
                    id = remoteEvent.id,
                    team1Name = remoteEvent.team1Name,
                    team1Flag = remoteEvent.team1Flag,
                    team2Name = remoteEvent.team2Name,
                    team2Flag = remoteEvent.team2Flag,
                    status = remoteEvent.status,
                    date = remoteEvent.date,
                    time = remoteEvent.time,
                    category = remoteEvent.category,
                    league = remoteEvent.league,
                    round = remoteEvent.round,
                    isFavorite = false
                )
                eventDao.insertEvent(event)

                for (remoteStream in remoteEvent.streams) {
                    val stream = StreamEntity(
                        id = remoteStream.id,
                        eventId = remoteEvent.id,
                        quality = remoteStream.quality,
                        serverName = remoteStream.label,
                        streamUrl = remoteStream.url
                    )
                    streamDao.insertStream(stream)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload local state to GitHub
    suspend fun uploadLiveEventsToGitHub(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = settingsDao.getGitHubConfigSync() ?: return@withContext Result.failure(Exception("GitHub settings are not configured."))
            if (config.username.isEmpty() || config.repository.isEmpty() || config.token.isEmpty()) {
                return@withContext Result.failure(Exception("GitHub login incomplete. Username, Repo, and Token are required to upload."))
            }

            val authHeader = "token ${config.token}"

            // Fetch current SHA of LiveEvents.json from GitHub if it exists
            val shaResponse = apiService.getFileContent(
                authHeader = authHeader,
                owner = config.username,
                repo = config.repository,
                path = "LiveEvents.json"
            )
            val fileSha = if (shaResponse.isSuccessful) shaResponse.body()?.sha else null

            // Compile local events & streams
            val localEventsList = eventDao.getAllEvents().firstOrNull() ?: emptyList()
            val remoteEventsJsonList = mutableListOf<RemoteEventJson>()

            for (event in localEventsList) {
                val localStreams = streamDao.getStreamsForEventSync(event.id)
                val streamsJsonList = localStreams.map {
                    RemoteStreamJson(
                        id = it.id,
                        quality = it.quality,
                        label = it.serverName,
                        url = it.streamUrl
                    )
                }
                remoteEventsJsonList.add(
                    RemoteEventJson(
                        id = event.id,
                        team1Name = event.team1Name,
                        team1Flag = event.team1Flag,
                        team2Name = event.team2Name,
                        team2Flag = event.team2Flag,
                        status = event.status,
                        date = event.date,
                        time = event.time,
                        category = event.category,
                        league = event.league,
                        round = event.round,
                        streams = streamsJsonList
                    )
                )
            }

            // Serialize to JSON
            val listType = Types.newParameterizedType(List::class.java, RemoteEventJson::class.java)
            val adapter: JsonAdapter<List<RemoteEventJson>> = moshi.adapter<List<RemoteEventJson>>(listType).indent("  ")
            val jsonString = adapter.toJson(remoteEventsJsonList)

            // Encode to Base64
            val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            // Send PUT update
            val updateRequest = GitHubUpdateRequest(
                message = "Synchronized LiveEvents.json from KaziTV Android App",
                content = base64Content,
                sha = fileSha
            )

            val updateResponse = apiService.updateFileContent(
                authHeader = authHeader,
                owner = config.username,
                repo = config.repository,
                path = "LiveEvents.json",
                body = updateRequest
            )

            if (updateResponse.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to upload: ${updateResponse.code()} ${updateResponse.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backup and Restore helpers (Auto Backup works on local files/storage simulated)
    suspend fun backupLocalDatabaseToJson(context: Context): String {
        return withContext(Dispatchers.IO) {
            val localEventsList = eventDao.getAllEvents().firstOrNull() ?: emptyList()
            val remoteEventsJsonList = mutableListOf<RemoteEventJson>()

            for (event in localEventsList) {
                val localStreams = streamDao.getStreamsForEventSync(event.id)
                val streamsJsonList = localStreams.map {
                    RemoteStreamJson(id = it.id, quality = it.quality, label = it.serverName, url = it.streamUrl)
                }
                remoteEventsJsonList.add(
                    RemoteEventJson(
                        id = event.id,
                        team1Name = event.team1Name,
                        team1Flag = event.team1Flag,
                        team2Name = event.team2Name,
                        team2Flag = event.team2Flag,
                        status = event.status,
                        date = event.date,
                        time = event.time,
                        category = event.category,
                        league = event.league,
                        round = event.round,
                        streams = streamsJsonList
                    )
                )
            }

            val listType = Types.newParameterizedType(List::class.java, RemoteEventJson::class.java)
            val adapter: JsonAdapter<List<RemoteEventJson>> = moshi.adapter<List<RemoteEventJson>>(listType).indent("  ")
            val json = adapter.toJson(remoteEventsJsonList)

            // Save to shared preference or private file for simulated local backup
            val prefs = context.getSharedPreferences("kazi_tv_backups", Context.MODE_PRIVATE)
            prefs.edit().putString("latest_backup", json).apply()
            json
        }
    }

    suspend fun restoreDatabaseFromBackup(context: Context): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("kazi_tv_backups", Context.MODE_PRIVATE)
                val json = prefs.getString("latest_backup", null) ?: return@withContext Result.failure(Exception("No backup found to restore."))

                val listType = Types.newParameterizedType(List::class.java, RemoteEventJson::class.java)
                val adapter: JsonAdapter<List<RemoteEventJson>> = moshi.adapter(listType)
                val remoteEvents = adapter.fromJson(json) ?: return@withContext Result.failure(Exception("Invalid backup JSON."))

                eventDao.clearAllEvents()
                streamDao.clearAllStreams()

                for (remoteEvent in remoteEvents) {
                    val event = EventEntity(
                        id = remoteEvent.id,
                        team1Name = remoteEvent.team1Name,
                        team1Flag = remoteEvent.team1Flag,
                        team2Name = remoteEvent.team2Name,
                        team2Flag = remoteEvent.team2Flag,
                        status = remoteEvent.status,
                        date = remoteEvent.date,
                        time = remoteEvent.time,
                        category = remoteEvent.category,
                        league = remoteEvent.league,
                        round = remoteEvent.round
                    )
                    eventDao.insertEvent(event)

                    for (remoteStream in remoteEvent.streams) {
                        val stream = StreamEntity(
                            id = remoteStream.id,
                            eventId = remoteEvent.id,
                            quality = remoteStream.quality,
                            serverName = remoteStream.label,
                            streamUrl = remoteStream.url
                        )
                        streamDao.insertStream(stream)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
