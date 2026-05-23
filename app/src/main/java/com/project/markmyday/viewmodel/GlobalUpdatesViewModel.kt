package com.project.markmyday.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.markmyday.data.api.RetrofitInstance
import com.project.markmyday.data.model.GNewsArticle
import com.project.markmyday.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NewsState {
    object Loading : NewsState()
    data class Success(val articles: List<GNewsArticle>) : NewsState()
    data class Error(val message: String) : NewsState()
}

class GlobalUpdatesViewModel : ViewModel() {
    private val repository = NewsRepository(RetrofitInstance.api)

    private val _hotNews = MutableStateFlow<NewsState>(NewsState.Loading)
    val hotNews: StateFlow<NewsState> = _hotNews.asStateFlow()

    private val _nationalNews = MutableStateFlow<NewsState>(NewsState.Loading)
    val nationalNews: StateFlow<NewsState> = _nationalNews.asStateFlow()

    private val _internationalNews = MutableStateFlow<NewsState>(NewsState.Loading)
    val internationalNews: StateFlow<NewsState> = _internationalNews.asStateFlow()

    val newsState: StateFlow<NewsState> = internationalNews

    init {
        fetchAllNews()
    }

    fun fetchNews() {
        fetchAllNews()
    }

    fun fetchAllNews() {
        fetchHotNews()
        fetchNationalNews()
        fetchInternationalNews()
    }

    fun fetchHotNews() {
        viewModelScope.launch {
            _hotNews.value = NewsState.Loading
            try {
                val response = repository.getHotNews()
                if (response.isSuccessful && (response.body() != null)) {
                    _hotNews.value = NewsState.Success(response.body()!!.articles)
                } else {
                    _hotNews.value = NewsState.Error("Failed to load headlines: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GlobalUpdatesVM", "Error fetching hot news", e)
                _hotNews.value = NewsState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun fetchNationalNews() {
        viewModelScope.launch {
            _nationalNews.value = NewsState.Loading
            try {
                val response = repository.getNationalNews()
                if (response.isSuccessful && (response.body() != null)) {
                    _nationalNews.value = NewsState.Success(response.body()!!.articles)
                } else {
                    _nationalNews.value = NewsState.Error("Failed to load India news: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GlobalUpdatesVM", "Error fetching national news", e)
                _nationalNews.value = NewsState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun fetchInternationalNews() {
        viewModelScope.launch {
            _internationalNews.value = NewsState.Loading
            try {
                val response = repository.getInternationalNews()
                if (response.isSuccessful && (response.body() != null)) {
                    _internationalNews.value = NewsState.Success(response.body()!!.articles)
                } else {
                    _internationalNews.value = NewsState.Error("Failed to load world news: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GlobalUpdatesVM", "Error fetching international news", e)
                _internationalNews.value = NewsState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }
}
