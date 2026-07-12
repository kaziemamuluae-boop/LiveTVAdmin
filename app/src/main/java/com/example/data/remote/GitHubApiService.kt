package com.example.data.remote

import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Response<GitHubContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFileContent(
        @Header("Authorization") authHeader: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: GitHubUpdateRequest
    ): Response<GitHubUpdateResponse>
}
