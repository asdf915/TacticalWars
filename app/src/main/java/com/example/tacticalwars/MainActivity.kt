package com.example.tacticalwars

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var musicPlayer: MediaPlayer? = null
    var musicVolume: Float = 0.5f
        set(value) {
            field = value
            musicPlayer?.setVolume(value, value)
        }
    var sfxVolume: Float = 0.5f
    var gameDifficulty: String = "Normal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startMusic()
    }

    fun startMusic() {
        if (musicPlayer == null) {
            try {
                musicPlayer = MediaPlayer.create(this, R.raw.musica_guerra)
                musicPlayer?.isLooping = true
                musicPlayer?.setVolume(musicVolume, musicVolume)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        musicPlayer?.start()
    }

    fun playSfx(resId: Int) {
        try {
            val mp = MediaPlayer.create(this, resId)
            mp?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            mp?.setVolume(sfxVolume, sfxVolume)
            mp?.setOnCompletionListener {
                it.stop()
                it.release()
            }
            mp?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        musicPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        musicPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer?.release()
        musicPlayer = null
    }
}
