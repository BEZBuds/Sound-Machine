package com.example.soundmachine

// import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var volumeControl: SeekBar
    private lateinit var pauseButton: Button
    private lateinit var timerHours: NumberPicker
    private lateinit var timerMinutes: NumberPicker
    private lateinit var startTimerButton: Button
    private var countDownTimer: CountDownTimer? = null
    private val contextRef = WeakReference(this)
    private var isPaused = false
    private var pausePosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val whiteNoiseButton: Button = findViewById(R.id.button_white_noise)
        val rainButton: Button = findViewById(R.id.button_rain)
        val rainThunderButton: Button = findViewById(R.id.button_rain_thunder)
        val oceanButton: Button = findViewById(R.id.button_ocean)
        val brookButton: Button = findViewById(R.id.button_brook)
        pauseButton = findViewById(R.id.button_pause)
        timerHours = findViewById(R.id.timer_hours)
        timerMinutes = findViewById(R.id.timer_minutes)
        startTimerButton = findViewById(R.id.button_start_timer)
        volumeControl = findViewById(R.id.volume_control)

        // Restore volume position
        val savedVolume = getVolumePosition()
        volumeControl.progress = savedVolume

        whiteNoiseButton.setOnClickListener { playSound(R.raw.white_noise) }
        rainButton.setOnClickListener { playSound(R.raw.rain) }
        rainThunderButton.setOnClickListener { playSound(R.raw.rain_thunder) }
        oceanButton.setOnClickListener { playSound(R.raw.ocean) }
        brookButton.setOnClickListener { playSound(R.raw.brook) }

        pauseButton.setOnClickListener { pauseOrResumeSound() }
        startTimerButton.setOnClickListener { setSleepTimer() }

        volumeControl.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.setVolume(progress / 100f, progress / 100f)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                saveVolumePosition(seekBar.progress)
            }
        })

        // Set up NumberPickers
        timerHours.minValue = 0
        timerHours.maxValue = 12
        timerMinutes.minValue = 0
        timerMinutes.maxValue = 3
        timerMinutes.displayedValues = arrayOf("00", "15", "30", "45")

        // Restore last played sound
        val lastPlayedSound = getLastPlayedSound()
        playSound(lastPlayedSound)
    }


    private fun saveLastPlayedSound(resourceId: Int) {
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt("LAST_PLAYED_SOUND", resourceId)
            apply()
        }
    }

    private fun playSound(resourceId: Int) {
        releaseMediaPlayer()
        contextRef.get()?.let {
            mediaPlayer = MediaPlayer.create(it, resourceId).apply {
                isLooping = true // Enable looping
                val volume = volumeControl.progress / 100f
                setVolume(volume, volume)
                start()
            }
        }
        saveLastPlayedSound(resourceId)
    }

    private fun getLastPlayedSound(): Int {
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return R.raw.white_noise // Default sound
        return sharedPref.getInt("LAST_PLAYED_SOUND", R.raw.white_noise)
    }


    private fun pauseOrResumeSound() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                pausePosition = mediaPlayer.currentPosition
                mediaPlayer.pause()
                isPaused = true
                pauseButton.background = ContextCompat.getDrawable(this, R.drawable.ic_play)
            } else if (isPaused) {
                mediaPlayer.seekTo(pausePosition)
                mediaPlayer.start()
                isPaused = false
                pauseButton.background = ContextCompat.getDrawable(this, R.drawable.ic_pause)
            }
        }
    }

    private fun setSleepTimer() {
        val hours = timerHours.value
        val minutes = timerMinutes.value * 15
        val totalMinutes = (hours * 60) + minutes

        if (totalMinutes > 0) {
            countDownTimer?.cancel() // Cancel any existing timer
            countDownTimer = object : CountDownTimer(totalMinutes.toLong() * 60 * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    // Update UI or show remaining time if needed
                }

                override fun onFinish() {
                    releaseMediaPlayer()
                    Toast.makeText(contextRef.get(), "Sleep timer finished", Toast.LENGTH_SHORT).show()
                }
            }.start()
            Toast.makeText(this, "Sleep timer set for $totalMinutes minutes", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please set a valid timer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releaseMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
            isPaused = false
            pauseButton.background = ContextCompat.getDrawable(this, R.drawable.ic_pause)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        countDownTimer?.cancel()
    }

    private fun saveVolumePosition(position: Int) {
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt("VOLUME_POSITION", position)
            apply()
        }
    }

    private fun getVolumePosition(): Int {
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return 50 // Default volume is 50%
        return sharedPref.getInt("VOLUME_POSITION", 50)
    }

}
