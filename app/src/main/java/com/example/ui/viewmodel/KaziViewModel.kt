package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EventEntity
import com.example.data.model.GitHubConfigEntity
import com.example.data.model.LocalSettingsEntity
import com.example.data.model.StreamEntity
import com.example.data.repository.KaziRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KaziViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KaziRepository(application)
    
    // Active navigation states
    val activeEventForStreams = MutableStateFlow<EventEntity?>(null)
    val activeStreamForEdit = MutableStateFlow<StreamEntity?>(null)
    val activeEventForEdit = MutableStateFlow<EventEntity?>(null)
    val activePlayingStream = MutableStateFlow<StreamEntity?>(null)

    // Core state flows
    val allEvents: StateFlow<List<EventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteEvents: StateFlow<List<EventEntity>> = repository.favoriteEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gitHubConfig: StateFlow<GitHubConfigEntity?> = repository.gitHubConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val localSettings: StateFlow<LocalSettingsEntity?> = repository.localSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    val filteredEvents: StateFlow<List<EventEntity>> = combine(
        allEvents,
        searchQuery,
        selectedCategory
    ) { events, query, category ->
        events.filter { event ->
            val matchesSearch = query.isEmpty() ||
                    event.team1Name.contains(query, ignoreCase = true) ||
                    event.team2Name.contains(query, ignoreCase = true) ||
                    event.league.contains(query, ignoreCase = true) ||
                    event.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || event.category.equals(category, ignoreCase = true)

            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Action state indicators
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable = _isInternetAvailable.asStateFlow()

    init {
        checkInternetConnection()
        viewModelScope.launch {
            repository.loadMockDataIfEmpty()
        }
    }

    fun checkInternetConnection() {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isInternetAvailable.value = hasInternet
        } catch (e: Exception) {
            _isInternetAvailable.value = true // Safe fallback if permission is missing
        }
    }

    fun syncFromGitHub() {
        checkInternetConnection()
        if (!_isInternetAvailable.value) {
            _syncState.value = SyncState.Error("No Internet connection. Sync is not available.")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Downloading LiveEvents.json...")
            repository.downloadLiveEventsFromGitHub()
                .onSuccess {
                    _syncState.value = SyncState.Success("Synced successfully from GitHub!")
                }
                .onFailure {
                    _syncState.value = SyncState.Error(it.message ?: "Failed to download.")
                }
        }
    }

    fun uploadToGitHub() {
        checkInternetConnection()
        if (!_isInternetAvailable.value) {
            _syncState.value = SyncState.Error("No Internet connection. Sync is not available.")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Uploading local events to GitHub...")
            repository.uploadLiveEventsToGitHub()
                .onSuccess {
                    _syncState.value = SyncState.Success("Uploaded local events to GitHub!")
                }
                .onFailure {
                    _syncState.value = SyncState.Error(it.message ?: "Failed to upload.")
                }
        }
    }

    fun backupDatabase() {
        viewModelScope.launch {
            repository.backupLocalDatabaseToJson(getApplication())
            _syncState.value = SyncState.Success("Local database backup saved!")
        }
    }

    fun restoreDatabase() {
        viewModelScope.launch {
            repository.restoreDatabaseFromBackup(getApplication())
                .onSuccess {
                    _syncState.value = SyncState.Success("Database restored from backup!")
                }
                .onFailure {
                    _syncState.value = SyncState.Error(it.message ?: "Failed to restore backup.")
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event.copy(isFavorite = !event.isFavorite))
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    // CRUD
    fun addEvent(event: EventEntity, streams: List<StreamEntity>) {
        viewModelScope.launch {
            val eventId = repository.insertEvent(event).toInt()
            streams.forEach {
                repository.insertStream(it.copy(eventId = eventId))
            }
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun updateEvent(event: EventEntity, streams: List<StreamEntity>) {
        viewModelScope.launch {
            repository.updateEvent(event)
            repository.getStreamsForEventSync(event.id).forEach {
                repository.deleteStream(it)
            }
            streams.forEach {
                repository.insertStream(it.copy(eventId = event.id))
            }
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun addStream(stream: StreamEntity) {
        viewModelScope.launch {
            repository.insertStream(stream)
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun updateStream(stream: StreamEntity) {
        viewModelScope.launch {
            repository.updateStream(stream)
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun deleteStream(stream: StreamEntity) {
        viewModelScope.launch {
            repository.deleteStream(stream)
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun deleteStreamById(id: Int) {
        viewModelScope.launch {
            repository.deleteStreamById(id)
            if (gitHubConfig.value?.isSyncEnabled == true) {
                uploadToGitHub()
            }
        }
    }

    fun saveGitHubConfig(config: GitHubConfigEntity) {
        viewModelScope.launch {
            repository.saveGitHubConfig(config)
        }
    }

    fun saveLocalSettings(settings: LocalSettingsEntity) {
        viewModelScope.launch {
            repository.saveLocalSettings(settings)
        }
    }

    fun getStreamsForEventFlow(eventId: Int): Flow<List<StreamEntity>> {
        return repository.getStreamsForEvent(eventId)
    }

    suspend fun getStreamsForEventSync(eventId: Int): List<StreamEntity> {
        return repository.getStreamsForEventSync(eventId)
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val errorMessage: String) : SyncState()
}
