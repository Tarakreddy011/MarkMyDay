package com.project.markmyday.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.markmyday.data.model.Question
import com.project.markmyday.data.repository.QuizRepository
import com.project.markmyday.utils.FileParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {
    private val repository = QuizRepository()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus

    private val _bankQuestions = MutableStateFlow<List<Question>>(emptyList())
    val bankQuestions: StateFlow<List<Question>> = _bankQuestions

    private val _availableTests = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableTests: StateFlow<List<Pair<String, String>>> = _availableTests

    fun uploadCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Loading
            val questions = FileParser.parseCsv(context, uri)
            if (questions.isEmpty()) {
                _uploadStatus.value = UploadStatus.Error("No questions found in file or invalid format")
                return@launch
            }
            
            val result = repository.uploadQuestions(questions)
            if (result.isSuccess) {
                _uploadStatus.value = UploadStatus.Success
            } else {
                _uploadStatus.value = UploadStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun saveManualQuestion(question: Question) {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Loading
            // Manual questions now save exactly as filled in UI (subject/class)
            val result = repository.saveManualQuestion(question)
            if (result.isSuccess) {
                _uploadStatus.value = UploadStatus.Success
                fetchAvailableTests()
            } else {
                _uploadStatus.value = UploadStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun saveManualQuestions(questions: List<Question>) {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Loading
            val result = repository.saveManualQuestions(questions)
            if (result.isSuccess) {
                _uploadStatus.value = UploadStatus.Success
                fetchAvailableTests()
            } else {
                _uploadStatus.value = UploadStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun fetchAvailableTests() {
        viewModelScope.launch {
            repository.getAvailableSubjectsForAdmin().collect {
                _availableTests.value = it
            }
        }
    }

    fun deleteTest(subject: String, className: String) {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Loading
            val result = repository.deleteQuestionsBySubjectAndClass(subject, className)
            if (result.isSuccess) {
                fetchAvailableTests()
                _uploadStatus.value = UploadStatus.Idle // Or success
            } else {
                _uploadStatus.value = UploadStatus.Error(result.exceptionOrNull()?.message ?: "Delete failed")
            }
        }
    }

    fun fetchQuestionsFromBank(subject: String, className: String) {
        viewModelScope.launch {
            repository.getQuestions(subject, className).collect {
                _bankQuestions.value = it
            }
        }
    }

    fun resetStatus() {
        _uploadStatus.value = UploadStatus.Idle
    }
}

sealed class UploadStatus {
    object Idle : UploadStatus()
    object Loading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}
