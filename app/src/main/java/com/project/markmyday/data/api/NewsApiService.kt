package com.project.markmyday.data.api

import com.project.markmyday.data.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("lang") lang: String = "en",
        @Query("country") country: String? = null,
        @Query("max") max: Int = 10,
        @Query("token") apiKey: String = "9f23f4386cc86284722ffe7a62474f28"
    ): Response<NewsResponse>

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String = "general",
        @Query("lang") lang: String = "en",
        @Query("country") country: String? = null,
        @Query("max") max: Int = 10,
        @Query("token") apiKey: String = "9f23f4386cc86284722ffe7a62474f28"
    ): Response<NewsResponse>
}
//comment for check
