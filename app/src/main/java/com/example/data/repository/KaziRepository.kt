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

class KaziRepository(private val context: Context) {

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

    private fun parseEventJsonArray(jsonString: String): org.json.JSONArray {
        val trimmed = jsonString.trim()
        return if (trimmed.startsWith("{")) {
            try {
                val rootObj = org.json.JSONObject(trimmed)
                rootObj.optJSONArray("events") ?: org.json.JSONArray()
            } catch (e: Exception) {
                org.json.JSONArray()
            }
        } else {
            try {
                org.json.JSONArray(trimmed)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
        }
    }

    // Load Mock Data if Database is completely empty on first run
    suspend fun loadMockDataIfEmpty() {
        // Find and delete all demo/mock events from the database
        val events = eventDao.getAllEvents().firstOrNull() ?: emptyList()
        events.forEach { event ->
            if (event.team1Name == "Manchester United" || 
                event.team1Name == "Los Angeles Lakers" ||
                event.team1Name == "Argentinaa" ||
                event.team1Name == "France" ||
                (event.team1Name == "Brazil" && event.team2Name == "Norway") ||
                (event.team1Name == "India" && event.team2Name == "Pakistan")
            ) {
                eventDao.deleteEvent(event)
                // Delete corresponding streams
                val streams = streamDao.getStreamsForEventSync(event.id)
                streams.forEach { stream ->
                    streamDao.deleteStream(stream)
                }
            }
        }

        // Save default configurations
        if (settingsDao.getLocalSettingsSync() == null) {
            settingsDao.saveLocalSettings(LocalSettingsEntity())
        }
        if (settingsDao.getGitHubConfigSync() == null) {
            settingsDao.saveGitHubConfig(GitHubConfigEntity())
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
                path = "Live/LiveEvents.json"
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
            val jsonArray = parseEventJsonArray(jsonString)

            // Clear and overwrite local DB tables
            eventDao.clearAllEvents()
            streamDao.clearAllStreams()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val team1 = obj.optString("team1", "")
                val team1Flag = obj.optString("team1_flag", "")
                val team2 = obj.optString("team2", "")
                val team2Flag = obj.optString("team2_flag", "")
                val date = obj.optString("date", "")
                val time = obj.optString("time", "")
                val category = obj.optString("category", "")
                val league = obj.optString("league", "")
                val round = obj.optString("round", "")

                var status = "LIVE"
                if (obj.has("status")) {
                    status = obj.getString("status")
                } else {
                    val day = obj.optString("day", "")
                    if (day.lowercase() == "today") {
                        status = "LIVE"
                    } else {
                        status = "UPCOMING"
                    }
                }

                val event = EventEntity(
                    id = 0,
                    team1Name = team1,
                    team1Flag = team1Flag,
                    team2Name = team2,
                    team2Flag = team2Flag,
                    status = status,
                    date = date,
                    time = time,
                    category = category,
                    league = league,
                    round = round
                )
                val eventId = eventDao.insertEvent(event).toInt()

                val streamsArray = obj.optJSONArray("streams")
                if (streamsArray != null) {
                    for (j in 0 until streamsArray.length()) {
                        val streamObj = streamsArray.getJSONObject(j)
                        val quality = streamObj.optString("quality", "FHD")
                        val label = streamObj.optString("label", "Main Stream")
                        val rawUrl = streamObj.optString("url", "")
                        val decryptedUrl = com.example.data.util.KaziCrypto.decrypt(rawUrl)

                        val stream = StreamEntity(
                            id = 0,
                            eventId = eventId,
                            quality = quality,
                            serverName = label,
                            streamUrl = decryptedUrl
                        )
                        streamDao.insertStream(stream)
                    }
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
                path = "Live/LiveEvents.json"
            )
            val fileSha = if (shaResponse.isSuccessful) shaResponse.body()?.sha else null

            // Compile local events & streams
            val localEventsList = eventDao.getAllEvents().firstOrNull() ?: emptyList()
            val jsonArray = org.json.JSONArray()

            for (event in localEventsList) {
                val obj = org.json.JSONObject()
                obj.put("id", event.id.toString())
                obj.put("team1", event.team1Name)
                obj.put("team1_flag", event.team1Flag)
                obj.put("team2", event.team2Name)
                obj.put("team2_flag", event.team2Flag)
                obj.put("date", event.date)
                obj.put("time", event.time)
                obj.put("category", event.category)
                obj.put("league", event.league)
                obj.put("round", event.round)
                obj.put("status", event.status)

                val localStreams = streamDao.getStreamsForEventSync(event.id)
                val streamsArray = org.json.JSONArray()
                for (stream in localStreams) {
                    val streamObj = org.json.JSONObject()
                    streamObj.put("quality", stream.quality)
                    streamObj.put("label", stream.serverName)
                    val encryptedUrl = com.example.data.util.KaziCrypto.encrypt(stream.streamUrl)
                    streamObj.put("url", encryptedUrl)
                    streamsArray.put(streamObj)
                }
                obj.put("streams", streamsArray)
                jsonArray.put(obj)
            }

            // Serialize to JSON with beautiful indentation
            val jsonString = jsonArray.toString(2)

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
                path = "Live/LiveEvents.json",
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
            val jsonArray = org.json.JSONArray()

            for (event in localEventsList) {
                val obj = org.json.JSONObject()
                obj.put("id", event.id.toString())
                obj.put("team1", event.team1Name)
                obj.put("team1_flag", event.team1Flag)
                obj.put("team2", event.team2Name)
                obj.put("team2_flag", event.team2Flag)
                obj.put("date", event.date)
                obj.put("time", event.time)
                obj.put("category", event.category)
                obj.put("league", event.league)
                obj.put("round", event.round)
                obj.put("status", event.status)

                val localStreams = streamDao.getStreamsForEventSync(event.id)
                val streamsArray = org.json.JSONArray()
                for (stream in localStreams) {
                    val streamObj = org.json.JSONObject()
                    streamObj.put("quality", stream.quality)
                    streamObj.put("label", stream.serverName)
                    streamObj.put("url", stream.streamUrl)
                    streamsArray.put(streamObj)
                }
                obj.put("streams", streamsArray)
                jsonArray.put(obj)
            }

            val json = jsonArray.toString(2)

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

                val jsonArray = org.json.JSONArray(json)

                eventDao.clearAllEvents()
                streamDao.clearAllStreams()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val team1 = obj.optString("team1", "")
                    val team1Flag = obj.optString("team1_flag", "")
                    val team2 = obj.optString("team2", "")
                    val team2Flag = obj.optString("team2_flag", "")
                    val date = obj.optString("date", "")
                    val time = obj.optString("time", "")
                    val category = obj.optString("category", "")
                    val league = obj.optString("league", "")
                    val round = obj.optString("round", "")

                    var status = "LIVE"
                    if (obj.has("status")) {
                        status = obj.getString("status")
                    } else {
                        val day = obj.optString("day", "")
                        if (day.lowercase() == "today") {
                            status = "LIVE"
                        } else {
                            status = "UPCOMING"
                        }
                    }

                    val event = EventEntity(
                        id = 0,
                        team1Name = team1,
                        team1Flag = team1Flag,
                        team2Name = team2,
                        team2Flag = team2Flag,
                        status = status,
                        date = date,
                        time = time,
                        category = category,
                        league = league,
                        round = round
                    )
                    val eventId = eventDao.insertEvent(event).toInt()

                    val streamsArray = obj.optJSONArray("streams")
                    if (streamsArray != null) {
                        for (j in 0 until streamsArray.length()) {
                            val streamObj = streamsArray.getJSONObject(j)
                            val quality = streamObj.optString("quality", "FHD")
                            val label = streamObj.optString("label", "Main Stream")
                            val rawUrl = streamObj.optString("url", "")
                            val decryptedUrl = com.example.data.util.KaziCrypto.decrypt(rawUrl)

                            val stream = StreamEntity(
                                id = 0,
                                eventId = eventId,
                                quality = quality,
                                serverName = label,
                                streamUrl = decryptedUrl
                            )
                            streamDao.insertStream(stream)
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
