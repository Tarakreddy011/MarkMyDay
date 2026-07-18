package com.project.markmyday.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Subscribes the user to FCM topics based on their role.
 * Should be called after successful login or session restoration.
 */
fun subscribeUserToTopics(role: String) {
    val messaging = FirebaseMessaging.getInstance()
    val normalizedRole = role.lowercase()

    // All users subscribe to topic_all
    messaging.subscribeToTopic("topic_all")
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM", "Successfully subscribed to topic_all")
            } else {
                Log.e("FCM", "Failed to subscribe to topic_all", task.exception)
            }
        }

    when (normalizedRole) {
        "teacher" -> {
            messaging.subscribeToTopic("topic_teachers")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Log.d("FCM", "Subscribed to topic_teachers")
                }
        }
        "principal", "headmaster", "admin" -> {
            // Subscribe to both teacher and admin topics
            messaging.subscribeToTopic("topic_teachers")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Log.d("FCM", "Subscribed to topic_teachers")
                }
            messaging.subscribeToTopic("topic_admin")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Log.d("FCM", "Subscribed to topic_admin")
                }
        }
        "student" -> {
            // Only topic_all (already handled)
        }
    }
}

/**
 * Unsubscribes from all role-specific topics.
 * Useful during logout.
 */
fun unsubscribeFromTopics() {
    val messaging = FirebaseMessaging.getInstance()
    val topics = listOf("topic_all", "topic_teachers", "topic_admin")
    topics.forEach { topic ->
        messaging.unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d("FCM", "Unsubscribed from $topic")
            }
    }
}
