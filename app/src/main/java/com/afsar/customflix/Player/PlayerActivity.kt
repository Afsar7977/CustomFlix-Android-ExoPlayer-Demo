@file:Suppress(
    "PackageName", "DEPRECATION",
    "PrivatePropertyName", "SameParameterValue"
)

package com.afsar.customflix.Player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProviders
import com.afsar.customflix.Modal.Downloads
import com.afsar.customflix.R
import com.afsar.customflix.Utilities.CFApplication
import com.afsar.customflix.Utilities.DBHelper
import com.afsar.customflix.Utilities.DownloadTracker
import com.afsar.customflix.VModel.VModel
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.Util
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_player.*
import java.util.*
import kotlin.properties.Delegates

class PlayerActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var mediaDataSourceFactory: DataSource.Factory
    private lateinit var playerView: PlayerView
    private lateinit var text2: TextView
    private lateinit var text3: TextView
    private lateinit var imageView: ImageView
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var playpause: LinearLayout
    private lateinit var seekBar: SeekBar
    private lateinit var startTime: TextView
    private lateinit var endTime: TextView
    private lateinit var timeseperator: TextView
    private lateinit var controls: LinearLayout
    private lateinit var controls_seek: LinearLayout
    private lateinit var full_screen: ImageView
    private lateinit var vModel: VModel
    private lateinit var url: String
    private lateinit var downloadTracker: DownloadTracker
    private lateinit var downloadManager: DownloadManager
    private lateinit var dbHelper: DBHelper
    private var dataSourceFactory: DataSource.Factory? = null
    private lateinit var mediaSource: MediaSource

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint(
        "ClickableViewAccessibility", "SetTextI18n",
        "SimpleDateFormat"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        dataSourceFactory = buildDataSourceFactory()
        val application: CFApplication = application as CFApplication
        downloadTracker = application.getDownloadTracker()
        downloadManager = application.getDownloadManager()!!
        dbHelper = DBHelper(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        playerView = findViewById(R.id.player_view)
        vModel = ViewModelProviders.of(this).get(VModel::class.java)
        text2 = findViewById(R.id.videoName)
        text3 = findViewById(R.id.tDescription)
        imageView = findViewById(R.id.previousten)
        imageView1 = findViewById(R.id.nextten)
        imageView2 = findViewById(R.id.btnPlay)
        playpause = findViewById(R.id.playpause)
        seekBar = findViewById(R.id.seekbar)
        timeseperator = findViewById(R.id.time_sperator)
        startTime = findViewById(R.id.time_current)
        endTime = findViewById(R.id.player_end_time)
        controls = findViewById(R.id.controls)
        controls_seek = findViewById(R.id.controls_seek)
        full_screen = findViewById(R.id.full_screen)

        showNow()
        videoName.text = vtitle
        tDescription.text = desc
        playerView.setOnTouchListener { _: View?, _: MotionEvent? ->
            showNow()
            false
        }

        full_screen.setOnClickListener {
            setOrientation("fullscreen")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        exitfullscreen.setOnClickListener {
            seekPosition = simpleExoPlayer.currentPosition
            VideoSeeking = true
            setOrientation("exitfullscreen")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        player_back.setOnClickListener {
            finish()
        }

        try {
            initSeekBar()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageView.setOnClickListener {
            when {
                simpleExoPlayer.currentPosition > 10000 -> simpleExoPlayer.seekTo(
                    simpleExoPlayer.currentPosition - 10000
                )
                else -> simpleExoPlayer.seekTo(0)
            }
        }

        imageView1.setOnClickListener {
            simpleExoPlayer.seekTo(
                simpleExoPlayer.currentPosition + 10000
            )
        }

        imageView2.setOnClickListener {
            if (simpleExoPlayer.isPlaying) {
                loadPlayerLog("iplay", "called")
                simpleExoPlayer.playWhenReady = false
                Glide
                    .with(applicationContext)
                    .load(R.drawable.exo_icon_play)
                    .into(imageView2)
            } else {
                loadPlayerLog("ipause", "called")
                simpleExoPlayer.playWhenReady = true
                Glide
                    .with(applicationContext)
                    .load(R.drawable.ic_pause)
                    .into(imageView2)
            }
        }

        download_linear.setOnClickListener {
            when (download.text) {
                "Download" -> {
                    Glide.with(applicationContext)
                        .load(R.drawable.cancel)
                        .error(R.drawable.ndownload)
                        .into(ndownload)
                    download.text = "Remove Download"
                    addDownload(
                        Uri.parse(intent.getStringExtra("url")!!),
                        intent.getStringExtra("title")!!,
                        simpleExoPlayer.duration.toString(),
                        "video"
                    )
                }
                "Remove Download" -> {
                    Glide.with(applicationContext)
                        .load(R.drawable.ndownload)
                        .error(R.drawable.ndownload)
                        .into(ndownload)
                    download.text = "Download"
                    removeDownload(Uri.parse(intent.getStringExtra("url")!!))
                }
            }
        }
    }

    private fun initializePlayer() {
        try {
            loadPlayerLog("initializePlayer", "called")
            Handler().postDelayed(
                {
                    mediaDataSourceFactory =
                        DefaultDataSourceFactory(
                            this,
                            Util.getUserAgent(this, "mediaPlayerSample")
                        )
                    val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
                    val videoTrackSelectionFactory: TrackSelection.Factory =
                        AdaptiveTrackSelection.Factory(bandwidthMeter)
                    val trackSelector: TrackSelector =
                        DefaultTrackSelector(videoTrackSelectionFactory)

                    simpleExoPlayer =
                        ExoPlayerFactory.newSimpleInstance(this, trackSelector)
                    url = intent.getStringExtra("url")!!
                    isDownloaded(url)
                    mediaSource = buildMediaSource(Uri.parse(url))
                    simpleExoPlayer.playWhenReady = true
                    playerView.setShutterBackgroundColor(Color.TRANSPARENT)
                    playerView.player = simpleExoPlayer
                    playerView.requestFocus()
                    simpleExoPlayer.addListener(PlayerEventListener())
                    playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
                    simpleExoPlayer.prepare(mediaSource, false, false)
                },
                2000
            )
        } catch (e: Exception) {
            loadPlayerLog("playerError", e.toString())
            Toast.makeText(this, "oops", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun isDownloaded(url: String) {
        try {
            when {
                downloadTracker.isDownloaded(Uri.parse(url)) -> {
                    Glide.with(applicationContext)
                        .load(R.drawable.cancel)
                        .error(R.drawable.ndownload)
                        .into(ndownload)
                    download.text = "Remove Download"
                }
                else -> {
                    Glide.with(applicationContext)
                        .load(R.drawable.ndownload)
                        .error(R.drawable.ndownload)
                        .into(ndownload)
                    download.text = "Download"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a new DataSource factory.
     */
    private fun buildDataSourceFactory(): DataSource.Factory? {
        return (application as CFApplication).buildDataSourceFactory()
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return buildMediaSource(uri, null)
    }


    /**
     * Returns a new MediaSource.
     */
    private fun buildMediaSource(uri: Uri, overrideExtension: String?): MediaSource {
        try {
            val downloadRequest: DownloadRequest = downloadTracker.getDownloadRequest(uri)!!
            @Suppress("SENSELESS_COMPARISON")
            when {
                downloadRequest != null -> {
                    loadPlayerLog("downloadMedaiSource", "called")
                    return DownloadHelper.createMediaSource(downloadRequest, dataSourceFactory)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return when (@C.ContentType val type = Util.inferContentType(uri, overrideExtension)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }


    /**
     * PlayerEventListener Class Methods.
     */
    inner class PlayerEventListener : Player.EventListener {

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            loadPlayerLog(TAG, "onPlaybackParametersChanged: ")
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
            loadPlayerLog(TAG, "onTracksChanged: ")
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            loadPlayerLog(TAG, "onPlayerError: ")
        }

        /** 4 playbackState exists */
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                PlaybackState.STATE_BUFFERING -> {
                    showNow()
                    loadPlayerLog("onPlayerStateChanged", "STATE_BUFFERING")
                }
                Player.STATE_READY -> {
                    VideoSeeking = false
                    loadPlayerLog("onPlayerStateChanged", "STATE_READY")
                }
                Player.STATE_IDLE -> {
                    loadPlayerLog("onPlayerStateChanged", "STATE_IDLE")
                }
                Player.STATE_ENDED -> {
                    loadPlayerLog("onPlayerStateChanged", "STATE_ENDED")
                }
            }
            when {
                playbackState == Player.STATE_READY -> {
                    loadPlayerLog("might be idle ready", "TAG")
                    Picasso.get().load(R.drawable.ic_pause).into(imageView2)
                    setSeekProgress()
                }
                playWhenReady -> {
                    loadPlayerLog("might be idle (plays)", "TAG")
                }
                else -> {
                    loadPlayerLog("player paused in any", "TAG")
                }
            }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            loadPlayerLog(TAG, "onLoadingChanged: ")
        }

        override fun onPositionDiscontinuity(reason: Int) {
            loadPlayerLog(TAG, "onPositionDiscontinuity: ")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            loadPlayerLog(TAG, "onRepeatModeChanged: ")
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            loadPlayerLog(TAG, "onTimelineChanged: ")
        }
    }

    private fun releasePlayer() {
        try {
            simpleExoPlayer.release()
        } catch (e: Exception) {
            loadPlayerLog("releasePlayer", e.toString())
        }
    }

    public override fun onStart() {
        super.onStart()
        when {
            Util.SDK_INT > 23 -> {
                initializePlayer()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        when {
            Util.SDK_INT <= 23 -> {
                initializePlayer()
            }
        }
        try {
            playerView.onResume()
            if (VideoSeeking) {
                simpleExoPlayer.seekTo(seekPosition)
                VideoSeeking = false
                simpleExoPlayer.playWhenReady = true
                simpleExoPlayer.playbackState
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            simpleExoPlayer.playWhenReady = true
            simpleExoPlayer.playbackState
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) releasePlayer()
        try {
            simpleExoPlayer.playWhenReady = false
            simpleExoPlayer.playbackState
        } catch (e: Exception) {
            e.printStackTrace()
        }
        playerView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        releasePlayer()
        VideoSeeking = false
        seekPosition = 0
        finish()
    }

    private fun showNow() {
        controls.visibility = View.VISIBLE
        controls_seek.visibility = View.VISIBLE
        if (current_orientation == "fullscreen") {
            exitactivity.visibility = View.GONE
            exitfullscreen.visibility = View.VISIBLE
            full_screen.visibility = View.GONE
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        controls.postDelayed({
            controls.visibility = View.GONE
        }, 3000)

        controls_seek.postDelayed({
            controls_seek.visibility = View.GONE
        }, 3000)
    }

    private fun initSeekBar() {
        seekBar.requestFocus()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!p2) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return
                }
                simpleExoPlayer.seekTo((p1 * 1000).toLong())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
        seekBar.max = 0
        seekBar.max = (simpleExoPlayer.duration / 1000).toInt()
    }

    private fun setSeekProgress() {
        if (simpleExoPlayer.currentPosition == 0L) {
            seekBar.progress = 0
        }
        seekBar.max = (simpleExoPlayer.duration / 1000).toInt()
        startTime.text = stringForTime(simpleExoPlayer.currentPosition.toInt())
        endTime.text = stringForTime(simpleExoPlayer.duration.toInt())
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                seekBar.max = simpleExoPlayer.duration.toInt() / 1000
                val mCurrentPosition = simpleExoPlayer.currentPosition.toInt() / 1000
                seekBar.progress = mCurrentPosition
                startTime.text = stringForTime(simpleExoPlayer.currentPosition.toInt())
                endTime.text = stringForTime(simpleExoPlayer.duration.toInt())
                handler.postDelayed(this, 1000)
            }
        })
    }

    /**
     * Add Download Media.
     */
    private fun addDownload(sample: Uri, name: String, duration: String, type: String) {
        Toast.makeText(this, "Media Added for Downloading!!", Toast.LENGTH_SHORT).show()
        when (getDownloadUnsupportedStringId(sample)) {
            0 -> {
                val renderersFactory: RenderersFactory = (application as CFApplication)
                    .buildRenderersFactory(false)
                downloadTracker.addDownload(
                    supportFragmentManager,
                    name,
                    sample,
                    "",
                    renderersFactory
                )
                when {
                    !dbHelper.checkDownloads(url) -> {
                        dbHelper.insertDownloads(name, type, url, duration, eng_banner)
                    }
                }
                try {
                    val notes: List<Downloads> = dbHelper.allDownloads
                    loadPlayerLog("notes", "${notes.size}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Remove Downloaded Video.
     */
    private fun removeDownload(sample: Uri) {
        Toast.makeText(this, "Media Removed from Downloading!!", Toast.LENGTH_SHORT).show()
        when (getDownloadUnsupportedStringId(sample)) {
            0 -> {
                downloadTracker.removeDownload(sample)
                when {
                    !dbHelper.checkDownloads(url) -> {
                        dbHelper.deleteDownloadsByPath(url)
                    }
                }
                try {
                    val notes: List<Downloads> = dbHelper.allDownloads
                    loadPlayerLog("notes", "${notes.size}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * PlayerErrorMessageProvider Class Methods.
     */
    private class PlayerErrorMessageProvider :
        ErrorMessageProvider<ExoPlaybackException> {
        @SuppressLint("SwitchIntDef")
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString = "Playback failed"
            when (e.type) {
                ExoPlaybackException.TYPE_RENDERER -> {
                    when (val cause = e.rendererException) {
                        is MediaCodecRenderer.DecoderInitializationException -> {
                            // Special case for decoder initialization failures.
                            errorString = if (cause.decoderName == null) {
                                when {
                                    cause.cause is MediaCodecUtil.DecoderQueryException -> {
                                        "Unable to query device decoders"
                                    }
                                    cause.secureDecoderRequired -> {
                                        "This device does not provide a secure decoder"
                                    }
                                    else -> {
                                        "This device does not provide a secure decoder"
                                    }
                                }
                            } else {
                                "Unable to instantiate decoder "
                            }
                        }
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }

    private fun getDownloadUnsupportedStringId(sample: Uri): Int {
        val scheme = sample.scheme
        return if (!("http" == scheme || "https" == scheme)) {
            R.string.download_scheme_unsupported
        } else 0
    }

    private fun stringForTime(timeMs: Int): String? {
        val mFormatter: Formatter
        val mFormatBuilder: StringBuilder = StringBuilder()
        mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun loadPlayerLog(key: String, value: String) {
        Log.d(TAG, "loadPlayerLog:$key::$value")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setOrientation(string: String) {
        current_orientation = string
        when (string) {
            "fullscreen" -> {
                loadPlayerLog(TAG, "setOrientation: fullscreen")
                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val vplayoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                viewplayer.layoutParams = layoutParams
                playerView.layoutParams = vplayoutParams
                video.visibility = View.GONE
                videoName.visibility = View.GONE
                tDescription.visibility = View.GONE
                full_screen.visibility = View.GONE
                exitfullscreen.visibility = View.VISIBLE
                exitactivity.visibility = View.GONE
                playerView.visibility = View.VISIBLE
                controls_options.visibility = View.VISIBLE
                download_linear.visibility = View.GONE
                exitfullscreen.z = 1f
                exitactivity.z = 1f
            }
            "exitfullscreen" -> {
                loadPlayerLog(TAG, "setOrientation: exitfullscreen")
                val vp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    250f,
                    resources.displayMetrics
                ).toInt()
                val pv = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    250f,
                    resources.displayMetrics
                ).toInt()
                val vplayoutParams =
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        pv
                    )
                val layoutParams =
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        vp
                    )
                val svlayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val hlayoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                hlayoutParams.setMargins(20, 10, 0, 0)
                header_relative.layoutParams = hlayoutParams
                svlayoutParams.setMargins(10, 10, 0, 0)
                layoutParams.setMargins(4, 100, 4, 0)
                viewplayer.layoutParams = layoutParams
                playerView.layoutParams = vplayoutParams
                video.visibility = View.VISIBLE
                videoName.visibility = View.VISIBLE
                exitfullscreen.visibility = View.GONE
                tDescription.visibility = View.VISIBLE
                full_screen.visibility = View.VISIBLE
                exitactivity.visibility = View.GONE
                header_relative.visibility = View.VISIBLE
                playerView.visibility = View.VISIBLE
                controls_options.visibility = View.VISIBLE
                download_linear.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private var TAG: String = "PlayerActivity"
        private var seekPosition by Delegates.notNull<Long>()
        private var current_orientation: String = ""
        private var VideoSeeking: Boolean = false
        var eng_banner: String = ""
        var vtitle: String = ""
        var desc: String = ""
        var id = ""
    }
}