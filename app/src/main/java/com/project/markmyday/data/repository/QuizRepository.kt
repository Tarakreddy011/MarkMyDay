package com.project.markmyday.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.markmyday.data.model.Question
import com.project.markmyday.data.model.QuizResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QuizRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val questionCollection = firestore.collection("question_bank")

    suspend fun uploadQuestions(questions: List<Question>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            questions.forEach { question ->
                val docRef = if (question.id.isEmpty()) {
                    questionCollection.document()
                } else {
                    questionCollection.document(question.id)
                }
                
                val questionWithId = if (question.id.isEmpty()) {
                    question.copy(id = docRef.id)
                } else {
                    question
                }
                
                batch.set(docRef, questionWithId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttemptCountForMixed(studentId: String, className: String): Int {
        return try {
            val query = firestore.collection("quiz_results")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("className", className)
                .whereEqualTo("subject", "Mixed")
                .get()
                .await()
            query.size()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun saveManualQuestion(question: Question): Result<Unit> {
        return try {
            val docRef = if (question.id.isEmpty()) {
                questionCollection.document()
            } else {
                questionCollection.document(question.id)
            }
            
            val questionWithId = if (question.id.isEmpty()) {
                question.copy(id = docRef.id)
            } else {
                question
            }
            
            docRef.set(questionWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveManualQuestions(questions: List<Question>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            questions.forEach { question ->
                val docRef = if (question.id.isEmpty()) {
                    questionCollection.document()
                } else {
                    questionCollection.document(question.id)
                }
                
                val questionWithId = if (question.id.isEmpty()) {
                    question.copy(id = docRef.id)
                } else {
                    question
                }
                
                batch.set(docRef, questionWithId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getAttemptedQuestionIds(studentId: String, className: String): Set<String> {
        return try {
            // Get start of today in milliseconds
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.timeInMillis

            val query = firestore.collection("quiz_results")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("className", className)
                .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                .get()
                .await()
            
            val ids = mutableSetOf<String>()
            query.documents.forEach { doc ->
                @Suppress("UNCHECKED_CAST")
                val questionIds = doc.get("attemptedQuestionIds") as? List<String>
                questionIds?.let { ids.addAll(it) }
            }
            ids
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun saveQuizResult(result: QuizResult): Result<Unit> {
        return try {
            // Use timestamp to allow multiple attempts in the same collection
            firestore.collection("quiz_results")
                .document("${result.studentId}_${result.subject}_${result.className}_${result.timestamp}")
                .set(result)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun getAllResults(): Flow<List<QuizResult>> = flow {
        try {
            val query = firestore.collection("quiz_results")
                .get()
                .await()
            val results = query.toObjects(QuizResult::class.java)
            emit(results)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getQuestions(subject: String, className: String): Flow<List<Question>> = flow {
        try {
            var query: Query = questionCollection
            
            if (className.isNotBlank()) {
                val normalizedClass = if (!className.startsWith("Class") && className != "ALL") "Class $className" else className
                // Use whereIn to include both class-specific and global ("ALL") questions
                query = query.whereIn("className", listOf(normalizedClass, "ALL"))
            }
            
            if (subject.isNotBlank()) {
                query = query.whereEqualTo("subject", subject)
            }
            
            val result = query.get().await()
            val questions = result.toObjects(Question::class.java)
            emit(questions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getQuestionsForClass(className: String): Flow<List<Question>> = flow {
        try {
            val normalizedClass = if (!className.startsWith("Class") && className != "ALL") "Class $className" else className
            
            // Query by className or "ALL" for mixed subjects
            val query = questionCollection
                .whereIn("className", listOf(normalizedClass, "ALL"))
                .get()
                .await()
            
            val questions = query.toObjects(Question::class.java)
            emit(questions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getAvailableSubjectsForClass(className: String): Flow<List<Pair<String, String>>> = flow {
        try {
            val normalizedClass = if (!className.startsWith("Class") && className != "ALL") "Class $className" else className
            val query = questionCollection
                .whereIn("className", listOf(normalizedClass, "ALL"))
                .get()
                .await()
            
            val subjects = query.documents.map { doc ->
                val sub = doc.getString("subject") ?: "General"
                val cls = doc.getString("className") ?: "ALL"
                sub to cls
            }.distinct()
            
            emit(subjects)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getAvailableSubjectsForAdmin(): Flow<List<Pair<String, String>>> = flow {
        try {
            val query = questionCollection.get().await()
            val subjects = query.documents.map { doc ->
                val sub = doc.getString("subject") ?: "General"
                val cls = doc.getString("className") ?: "ALL"
                sub to cls
            }.distinct()
            emit(subjects)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun deleteQuestionsBySubjectAndClass(subject: String, className: String): Result<Unit> {
        return try {
            val query = questionCollection
                .whereEqualTo("subject", subject)
                .whereEqualTo("className", className)
                .get()
                .await()
            
            val batch = firestore.batch()
            query.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllResults(): Result<Unit> {
        return try {
            val collection = firestore.collection("quiz_results")
            val snapshots = collection.get().await()
            val batch = firestore.batch()
            for (doc in snapshots.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
