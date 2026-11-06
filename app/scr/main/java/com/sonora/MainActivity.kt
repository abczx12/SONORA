package com.sonora

import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private lateinit var status: TextView
    private lateinit var track: TextView
    private lateinit var playButton: Button
    private lateinit var audioManager: AudioManager

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            playUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        buildUi()
        player = ExoPlayer.Builder(this).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playButton.text = if (isPlaying) "PAUSE" else "PLAY"
                    status.text = if (isPlaying) "Đang phát" else "Đã tạm dừng"
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) status.text = "Đã phát xong"
                }
            })
        }
        refreshRoutes()
    }

    private fun buildUi() {
        val bg = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)
            setBackgroundColor(0xFF090D18.toInt())
        }
        val title = TextView(this).apply {
            text = "SONORA"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "AUDIO PLAYER  •  AUDIO ENGINE"
            textSize = 11f
            setTextColor(0xFF9AA8C5.toInt())
        }
        val island = TextView(this).apply {
            text = "●  SONORA  •  READY"
            textSize = 13f
            setTextColor(0xFFE8F0FF.toInt())
            setPadding(18, 14, 18, 14)
            setBackgroundColor(0xFF202B43.toInt())
        }
        track = TextView(this).apply {
            text = "Chưa chọn bài nhạc"
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 24, 0, 12)
        }
        status = TextView(this).apply {
            text = "Sẵn sàng"
            textSize = 13f
            setTextColor(0xFF9AA8C5.toInt())
        }
        val choose = makeButton("CHỌN TỆP NHẠC") { pickAudio.launch(arrayOf("audio/*")) }
        playButton = makeButton("PLAY") {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        val stop = makeButton("DỪNG") { player?.stop(); playButton.text = "PLAY"; status.text = "Đã dừng" }
        val seek = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    if (u) player?.let { if (it.duration > 0) it.seekTo(it.duration * p / 1000) }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val routeTitle = section("ĐẦU RA ÂM THANH")
        val routes = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFD6E0F5.toInt())
            setPadding(0, 6, 0, 12)
        }
        val refresh = makeButton("KIỂM TRA THIẾT BỊ ĐẦU RA") { routes.text = routeText() }
        val engineTitle = section("AUDIO ENGINE")
        val preset = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Flat", "Voice Clear", "Podcast", "Vocal", "Bass Soft"))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    applyPreset(position)
                }
            }
        }
        val gainLabel = TextView(this).apply {
            text = "Loudness boost (0–1500 mB)"
            setTextColor(0xFFD6E0F5.toInt())
        }
        val gain = SeekBar(this).apply {
            max = 1500
            progress = 300
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    try { loudness?.setTargetGain(p) } catch (_: Exception) {}
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        bg.addView(title)
        bg.addView(subtitle)
        bg.addView(space(18))
        bg.addView(island)
        bg.addView(track)
        bg.addView(status)
        bg.addView(choose)
        bg.addView(playButton)
        bg.addView(stop)
        bg.addView(seek)
        bg.addView(space(16))
        bg.addView(routeTitle)
        bg.addView(routes)
        bg.addView(refresh)
        bg.addView(space(12))
        bg.addView(engineTitle)
        bg.addView(preset)
        bg.addView(gainLabel)
        bg.addView(gain)
        val scroll = ScrollView(this).apply { addView(bg) }
        setContentView(scroll)
        routes.text = routeText()
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 8
        }
    }

    private fun section(label: String) = TextView(this).apply {
        text = label
        textSize = 14f
        setTextColor(0xFF8EB8FF.toInt())
        setPadding(0, 12, 0, 4)
    }

    private fun space(h: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, h)
    }

    private fun playUri(uri: Uri) {
        val p = player ?: return
        releaseEffects()
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.playWhenReady = true
        track.text = uri.lastPathSegment ?: "Tệp âm thanh"
        status.text = "Đang tải âm thanh…"
        p.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachEffects(audioSessionId)
            }
        })
        if (p.audioSessionId != 0) attachEffects(p.audioSessionId)
    }

    private fun attachEffects(sessionId: Int) {
        releaseEffects()
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (_: Exception) { equalizer = null }
        try {
            loudness = LoudnessEnhancer(sessionId).apply { setTargetGain(300); enabled = true }
        } catch (_: Exception) { loudness = null }
    }

    private fun applyPreset(index: Int) {
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val profiles = listOf(
                listOf(0,0,0,0,0), listOf(-1,1,3,2,0),
                listOf(1,2,2,1,0), listOf(-1,1,3,2,1),
                listOf(3,2,0,-1,-2)
            )
            for (i in 0 until bands) {
                val source = profiles[index]
                val value = source[(i * source.size / bands).coerceIn(0, source.lastIndex)] * 100
                eq.setBandLevel(i.toShort(), value.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
        } catch (_: Exception) {}
    }

    private fun routeText(): String {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        if (devices.isEmpty()) return "Không phát hiện thiết bị đầu ra."
        return devices.joinToString("\n") { d ->
            val name = when (d.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Loa điện thoại"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Tai nghe có dây"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth"
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB audio"
                else -> "Thiết bị âm thanh khác"
            }
            "• $name"
        } + "\n\nĐây là danh sách đầu ra hệ thống, không phải microphone ảo."
    }

    private fun refreshRoutes() { status.text = "Đã kiểm tra thiết bị đầu ra"; }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null
        loudness = null
    }

    override fun onDestroy() {
        releaseEffects()
        player?.release()
        player = null
        super.onDestroy()
    }
}
