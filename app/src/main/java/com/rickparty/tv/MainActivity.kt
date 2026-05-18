package com.rickparty.tv

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity(), CommandServer.CommandListener {

    private lateinit var imageView: ImageView
    private lateinit var glitchView: View
    private var images = listOf(
        R.drawable.pic1,
        R.drawable.pic2,
        R.drawable.pic3,
        R.drawable.pic4,
        R.drawable.pic5
    )
    private var index = 0

    private val handler = Handler(Looper.getMainLooper())
    private var slideshowRunnable: Runnable? = null
    @Volatile
    private var chaosMode = false

    private var mediaPlayer: MediaPlayer? = null
    private var server: CommandServer? = null

    private var backCount = 0
    private var backResetRunnable: Runnable? = null

    private var normalDelay = 3000L

    private var currentDelay = normalDelay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        glitchView = findViewById(R.id.glitchView)

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageResource(images[index])

        startMedia()
        startSlideshow()

        server = CommandServer(8080)
        server?.setListener(this)
        server?.start()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun startMedia() {
        mediaPlayer = MediaPlayer.create(this, R.raw.never_gonna_give_you_up)
        mediaPlayer?.isLooping = true
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSlideshow() {
        slideshowRunnable = object : Runnable {
            override fun run() {
                try {
                    nextImageInternal()
                } finally {
                    val delay = if (chaosMode) {
                        Random.nextLong(800L, 4000L)
                    } else {
                        normalDelay
                    }
                    currentDelay = delay
                    handler.postDelayed(this, delay)
                }
            }
        }
        handler.post(slideshowRunnable!!)
    }

    private fun nextImageInternal() {
        val next = (index + 1) % images.size
        crossfadeTo(next)
        index = next
        if (chaosMode) {
            triggerChaosAudioJump()
        }
    }

    private fun prevImageInternal() {
        val prev = if (index - 1 < 0) images.size - 1 else index - 1
        crossfadeTo(prev)
        index = prev
    }

    private fun crossfadeTo(nextIndex: Int) {
        imageView.animate().alpha(0f).setDuration(400).withEndAction {
            imageView.setImageResource(images[nextIndex])
            imageView.animate().alpha(1f).setDuration(400).start()
        }.start()
    }

    private fun doGlitchFlash() {
        runOnUiThread {
            glitchView.visibility = View.VISIBLE
            glitchView.alpha = 0f
            glitchView.animate().alpha(1f).setDuration(80).withEndAction {
                glitchView.animate().alpha(0f).setDuration(250).withEndAction {
                    glitchView.visibility = View.GONE
                }.start()
            }.start()
        }
    }

    private fun triggerChaosAudioJump() {
        if (mediaPlayer == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val speed = Random.nextDouble(0.8, 1.6).toFloat()
                val params = PlaybackParams().setSpeed(speed)
                mediaPlayer?.playbackParams = params
                handler.postDelayed({
                    try {
                        val normal = PlaybackParams().setSpeed(1.0f)
                        mediaPlayer?.playbackParams = normal
                    } catch (e: Exception) {
                    }
                }, Random.nextLong(150, 900))
            } catch (e: Exception) {
            }
        }
    }

    private fun boostAudioTemporary() {
        mediaPlayer?.let { mp ->
            val duration = 3000L
            runOnUiThread {
                mp.setVolume(1.0f, 1.0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val params = PlaybackParams().setSpeed(1.15f)
                        mp.playbackParams = params
                    } catch (_: Exception) {
                    }
                }
            }
            handler.postDelayed({
                runOnUiThread {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val params = PlaybackParams().setSpeed(1.0f)
                            mp.playbackParams = params
                        }
                    } catch (_: Exception) {
                    }
                }
            }, duration)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                handler.removeCallbacks(slideshowRunnable!!)
                nextImageInternal()
                handler.postDelayed(slideshowRunnable!!, currentDelay)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                handler.removeCallbacks(slideshowRunnable!!)
                prevImageInternal()
                handler.postDelayed(slideshowRunnable!!, currentDelay)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                toggleChaos()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                boostAudioTemporary()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                handleBackPress()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleBackPress() {
        backCount++
        if (backCount == 1) {
            Toast.makeText(this, "Hold BACK to escape the vibe zone", Toast.LENGTH_SHORT).show()
            backResetRunnable = Runnable { backCount = 0 }
            handler.postDelayed(backResetRunnable!!, 2000)
        } else if (backCount >= 3) {
            finish()
        }
    }

    private fun toggleChaos() {
        chaosMode = !chaosMode
        if (chaosMode) {
            Toast.makeText(this, "Chaos mode activated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Chaos mode deactivated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        server?.stop()
        server = null
    }

    override fun onNext() {
        runOnUiThread {
            handler.removeCallbacks(slideshowRunnable!!)
            nextImageInternal()
            handler.postDelayed(slideshowRunnable!!, currentDelay)
        }
    }

    override fun onGlitch() {
        doGlitchFlash()
    }

    override fun onChaosToggle() {
        runOnUiThread { toggleChaos() }
    }

    override fun onBoost() {
        runOnUiThread { boostAudioTemporary() }
    }
}
