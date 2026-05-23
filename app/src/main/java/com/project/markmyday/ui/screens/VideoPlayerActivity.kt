package com.project.markmyday.ui.screens

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.project.markmyday.data.repository.EngagementRepository
import com.project.markmyday.databinding.ActivityVideoPlayerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var youTubePlayer: YouTubePlayer? = null
    private var isVideoPlaying = false
    
    // Watch Time Tracking
    private var activeSeconds: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isVideoPlaying) {
                activeSeconds++
            }
            handler.postDelayed(this, 1000)
        }
    }
    
    private val engagementRepository = EngagementRepository()
    private lateinit var videoId: String
    private lateinit var videoTitle: String
    private lateinit var studentId: String
    private lateinit var studentName: String
    private lateinit var className: String

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoId = intent.getStringExtra("VIDEO_ID") ?: return
        videoTitle = intent.getStringExtra("TITLE") ?: "Lesson"
        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        studentName = intent.getStringExtra("STUDENT_NAME") ?: "Student"
        className = intent.getStringExtra("CLASS_NAME") ?: ""

        supportActionBar?.title = videoTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val iFramePlayerOptions = IFramePlayerOptions.Builder(this)
            .controls(1)
            .fullscreen(1) // Enable fullscreen button
            .build()

        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@VideoPlayerActivity.youTubePlayer = youTubePlayer
                
                // Load video
                youTubePlayer.loadVideo(videoId, 0f)
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                isVideoPlaying = state == PlayerConstants.PlayerState.PLAYING
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                android.util.Log.e("VideoPlayer", "YouTube Player Error: $error")
                
                // Catch embedding errors (copyright, restrictions, or embedding disabled)
                if ((error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) || 
                    (error == PlayerConstants.PlayerError.UNKNOWN)) {
                    
                    runOnUiThread {
                        Toast.makeText(this@VideoPlayerActivity, "This video cannot be played in-app. Opening YouTube...", Toast.LENGTH_LONG).show()
                        
                        // Automatically open in native YouTube app/browser
                        val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                        startActivity(youtubeIntent)
                        
                        // Close activity since we moved out
                        finish()
                    }
                }
            }
        }, iFramePlayerOptions)

        binding.youtubePlayerView.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                isFullscreen = true
                binding.fullScreenViewContainer.visibility = View.VISIBLE
                binding.fullScreenViewContainer.addView(fullscreenView)
                binding.youtubePlayerView.visibility = View.GONE
                
                toggleFullscreen(true)
            }

            override fun onExitFullscreen() {
                isFullscreen = false
                binding.fullScreenViewContainer.visibility = View.GONE
                binding.fullScreenViewContainer.removeAllViews()
                binding.youtubePlayerView.visibility = View.VISIBLE
                
                toggleFullscreen(false)
            }
        })

        handler.post(timerRunnable)
    }

    private fun toggleFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            hideSystemUI()
            supportActionBar?.hide()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            showSystemUI()
            supportActionBar?.show()
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!isFullscreen) {
                binding.youtubePlayerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        } else {
            binding.youtubePlayerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onPause() {
        super.onPause()
        if (isVideoPlaying) {
            youTubePlayer?.pause()
            showAntiCheatWarning()
        }
        syncWatchTime()
    }

    override fun onStop() {
        super.onStop()
        syncWatchTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    private fun syncWatchTime() {
        if (activeSeconds > 0) {
            val secondsToSync = activeSeconds
            activeSeconds = 0
            
            CoroutineScope(Dispatchers.IO).launch {
                engagementRepository.updateWatchTime(
                    studentId = studentId,
                    studentName = studentName,
                    className = className,
                    videoId = videoId,
                    title = videoTitle,
                    secondsToAdd = secondsToSync
                )
            }
        }
    }

    private fun showAntiCheatWarning() {
        AlertDialog.Builder(this)
            .setTitle("Distraction detected")
            .setMessage("Please stay in the app to complete your lesson.")
            .setPositiveButton("Resume") { dialog, _ ->
                dialog.dismiss()
                youTubePlayer?.play()
            }
            .setCancelable(false)
            .show()
    }
}
