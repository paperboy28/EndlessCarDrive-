package com.endless.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnLeft: TextView
    private lateinit var btnRight: TextView
    private lateinit var btnAccel: TextView
    private lateinit var btnBrake: TextView
    private lateinit var btnNitro: TextView
    private lateinit var btnPause: TextView
    private lateinit var vibrator: Vibrator

    private var webSocket: WebSocket? = null
    private var isConnected = false

    // Button states
    private var left = false
    private var right = false
    private var accel = false
    private var brake = false
    private var nitro = false

    // QR Scanner launcher
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            tvStatus.text = "Scan Cancelled"
        } else {
            val wsUrl = result.contents.trim()
            tvStatus.text = "Connecting to: $wsUrl"
            connectWebSocket(wsUrl)
        }
    }

    // Camera Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            launchScanner()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR", Toast.LENGTH_LONG).show()
            tvStatus.text = "Camera Permission Denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        tvStatus = findViewById(R.id.tvStatus)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnAccel = findViewById(R.id.btnAccel)
        btnBrake = findViewById(R.id.btnBrake)
        btnNitro = findViewById(R.id.btnNitro)
        btnPause = findViewById(R.id.btnPause)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Setup button touch listeners
        setupButton(btnLeft) { left = it; sendState() }
        setupButton(btnRight) { right = it; sendState() }
        setupButton(btnAccel) { accel = it; sendState() }
        setupButton(btnBrake) { brake = it; sendState() }
        setupButton(btnNitro) { nitro = it; sendState() }

        // Pause button special logic (sends a quick tap, doesn't hold)
        btnPause.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                vibrate()
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                sendPauseTap()
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
            }
            true
        }

        // Auto launch QR scanner on start
        checkPermissionAndScan()
    }

    private fun checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan the QR on your laptop")
        options.setBeepEnabled(false)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun setupButton(view: View, onChange: (Boolean) -> Unit) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onChange(true)
                    vibrate()
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onChange(false)
                    vibrate()
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                }
            }
            true
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun connectWebSocket(url: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    isConnected = true
                    tvStatus.text = "Connected ✓"
                    Toast.makeText(this@MainActivity, "Connected to Game!", Toast.LENGTH_SHORT).show()
                    // Vibrate double pulse for success
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                runOnUiThread {
                    isConnected = false
                    tvStatus.text = "Connection Failed. Tap to Retry."
                    tvStatus.setOnClickListener { checkPermissionAndScan() }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    isConnected = false
                    tvStatus.text = "Disconnected. Tap to Reconnect."
                    tvStatus.setOnClickListener { checkPermissionAndScan() }
                }
            }
        })
    }

    private fun sendState() {
        if (!isConnected) return
        val data = mapOf(
            "left" to left,
            "right" to right,
            "accel" to accel,
            "brake" to brake,
            "nitro" to nitro,
            "pause" to false
        )
        val json = Gson().toJson(data)
        webSocket?.send(json)
    }

    private fun sendPauseTap() {
        if (!isConnected) return
        val data = mapOf(
            "left" to false,
            "right" to false,
            "accel" to false,
            "brake" to false,
            "nitro" to false,
            "pause" to true
        )
        val json = Gson().toJson(data)
        webSocket?.send(json)

        // Send pause false immediately after (it's a tap)
        btnPause.postDelayed({
            sendState()
        }, 100)
    }

    override fun onDestroy() {
        webSocket?.close(1000, "App closed")
        super.onDestroy()
    }
}