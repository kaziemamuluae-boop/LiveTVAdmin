package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteEventJson(
    val id: Int,
    val team1Name: String,
    val team1Flag: String,
    val team2Name: String,
    val team2Flag: String,
    val status: String,
    val date: String,
    val time: String,
    val category: String,
    val league: String,
    val round: String,
    val streams: List<RemoteStreamJson>
)

@JsonClass(generateAdapter = true)
data class RemoteStreamJson(
    val id: Int,
    val quality: String,
    val label: String,
    val url: String
)
