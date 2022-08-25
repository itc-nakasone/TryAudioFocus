package jp.ac.it_college.std.nakasone.tryaudiofocus

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import jp.ac.it_college.std.nakasone.tryaudiofocus.databinding.ActivityMainBinding
import kotlinx.coroutines.Runnable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioManager: AudioManager

    private val delayedStopRunnable = Runnable {
        mediaController.transportControls.stop()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("TAF", "FOCUS LOSS")
                mediaController.transportControls.pause()
                handler.postDelayed(delayedStopRunnable, TimeUnit.SECONDS.toMillis(30))
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d("TAF", "FOCUS LOSS TRANSIENT")
                // Pause playback
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("TAF", "FOCUS LOSS TRANSIENT CAN DUCK")
                // Lower the volume, keep playing
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("TAF", "FOCUS GAIN")
            }
        }
    }
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioManager = (getSystemService(AUDIO_SERVICE) as? AudioManager)
            ?: throw IllegalStateException("AudioManager へアクセスできません")

        binding.button.setOnClickListener { _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getAudioFocus()
            } else {
                getAudioFocusOld()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAudioFocus() {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(afChangeListener, handler)
            build()
        }
        mediaPlayer = MediaPlayer()
        val focusLock = Any()

        var playbackDelayed = false
        var playbackNowAuthorized = false

        val res = audioManager.requestAudioFocus(focusRequest)
        synchronized(focusLock) {
            playbackNowAuthorized = when (res) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    Log.d("TAF", "REQUEST GRANTED")
                    playbackNow()
                    true
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    Log.d("TAF", "REQUEST DELAYED")
                    playbackDelayed = true
                    false
                }
                else -> false
            }
        }
    }



    private fun getAudioFocusOld() {

    }

    private fun playbackNow() {
        mediaPlayer?.let { player ->
            Log.d("TAF", "playbackNow")
            val uri = Uri.parse("android.resource://${packageName}/${R.raw.ff1_battle}")
            player.setDataSource(this, uri)
            player.setOnPreparedListener {
                Log.d("TAF", "onPrepared")
                player.start()
            }
            player.setOnErrorListener { mp, what, extra ->
                when (what) {
                    MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                        Log.w("TAF", "よくわからないエラー")
                    }
                    MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                        Log.w("TAF", "サーバ?が死んでるエラー")
                    }
                }
                when (extra) {
                    MediaPlayer.MEDIA_ERROR_IO -> {
                        Log.w("TAF", "IOエラー")
                    }
                    MediaPlayer.MEDIA_ERROR_MALFORMED -> {
                        Log.w("TAF", "正しくないファイル")
                    }
                    MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                        Log.w("TAF", "未サポートのファイル形式")
                    }
                    MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                        Log.w("TAF", "タイムアウト")
                    }
                }
                false
            }
            player.prepareAsync()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let { player ->
            player.pause()
            player.seekTo(0)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}