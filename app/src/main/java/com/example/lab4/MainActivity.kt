package com.example.lab4

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var customControls: View
    private lateinit var header: View
    private lateinit var btnPickContainer: View
    private lateinit var btnFullscreen: ImageButton
    private lateinit var playerCard: androidx.cardview.widget.CardView
    private lateinit var mainLayout: ConstraintLayout

    private var orientationListener: OrientationEventListener? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { playMedia(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        playerView = findViewById(R.id.playerView)
        playerCard = findViewById(R.id.playerCard)
        customControls = findViewById(R.id.customControlsLayout)
        header = findViewById(R.id.header)
        btnPickContainer = findViewById(R.id.btnPickContainer)
        btnFullscreen = findViewById(R.id.btnFullscreen)

        val volumeSeekBar = findViewById<SeekBar>(R.id.volumeSeekBar)
        val ivVolumeIcon = findViewById<ImageView>(R.id.ivVolumeIcon)

        initializePlayer()

        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            customControls.visibility = visibility
        })

        findViewById<Button>(R.id.btnPickVideo).setOnClickListener { pickMedia.launch("video/*") }
        findViewById<Button>(R.id.btnPickAudio).setOnClickListener { pickMedia.launch("audio/*") }

        findViewById<ImageButton>(R.id.btnOpenWeb).setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_url, null)
            bottomSheetDialog.setContentView(view)

            val etUrl = view.findViewById<EditText>(R.id.etUrl)
            val btnStream = view.findViewById<Button>(R.id.btnStream)

            btnStream.setOnClickListener {
                val url = etUrl.text.toString()
                if (url.isNotBlank()) {
                    playMedia(Uri.parse(url))
                    bottomSheetDialog.dismiss()
                } else {
                    Toast.makeText(this, "Посилання не може бути порожнім", Toast.LENGTH_SHORT).show()
                }
            }

            bottomSheetDialog.show()
        }

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val isPhysicallyLandscape = (orientation in 60..120) || (orientation in 240..300)
                val isPhysicallyPortrait = (orientation in 0..30) || (orientation in 330..360) || (orientation in 150..210)

                val currentConfig = resources.configuration.orientation

                if (currentConfig == Configuration.ORIENTATION_LANDSCAPE && isPhysicallyLandscape) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else if (currentConfig == Configuration.ORIENTATION_PORTRAIT && isPhysicallyPortrait) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
        orientationListener?.enable()

        btnFullscreen.setOnClickListener {
            val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            if (isPortrait) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                player?.volume = p / 100f
                ivVolumeIcon.setImageResource(if (p == 0) android.R.drawable.ic_lock_silent_mode
                else android.R.drawable.ic_lock_silent_mode_off)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        updateUI(resources.configuration.orientation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateUI(newConfig.orientation)
    }

    private fun updateUI(orientation: Int) {
        val params = playerCard.layoutParams as ConstraintLayout.LayoutParams
        val density = resources.displayMetrics.density

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            header.visibility = View.GONE
            btnPickContainer.visibility = View.GONE
            mainLayout.setPadding(0, 0, 0, 0)

            params.dimensionRatio = null
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.topMargin = 0
            playerCard.radius = 0f

            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)

            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            header.visibility = View.VISIBLE
            btnPickContainer.visibility = View.VISIBLE
            val p = (20 * density).toInt()
            mainLayout.setPadding(p, p, p, p)

            params.dimensionRatio = "16:9"
            params.width = 0
            params.height = 0
            params.topMargin = (20 * density).toInt()
            playerCard.radius = 28 * density

            btnFullscreen.setImageResource(R.drawable.ic_fullscreen)

            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        playerCard.layoutParams = params
    }

    private fun playMedia(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationListener?.disable()
        player?.release()
    }
}