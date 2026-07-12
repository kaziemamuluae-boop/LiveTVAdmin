package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val content: String?, // Base64 encoded string
    val encoding: String?
)

@JsonClass(generateAdapter = true)
data class GitHubUpdateRequest(
    val message: String,
    val content: String, // Base64 encoded JSON contents
    val sha: String? // Required if updating an existing file
)

@JsonClass(generateAdapter = true)
data class GitHubUpdateResponse(
    val content: GitHubContentResponse?
)
