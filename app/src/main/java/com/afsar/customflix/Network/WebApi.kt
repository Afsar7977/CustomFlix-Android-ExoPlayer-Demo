package com.afsar.customflix.Network

import com.afsar.customflix.Modal.Videos
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WebApi {
    @GET("get/cfxDhsUTzC?")
    suspend fun getVideos(
        @Query("indent") indent: String
    ): Response<List<Videos>>
}