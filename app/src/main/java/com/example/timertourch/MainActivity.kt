package com.example.timertourch

import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import androidx.core.net.toUri


class MainActivity : AppCompatActivity() {

    private lateinit var flashlightButton: Button
    private lateinit var button5Min: Button
    private lateinit var button10Min: Button
    private lateinit var button15Min: Button
    private lateinit var customTimeInput: EditText
    private lateinit var setCustomTimeButton: Button
    private lateinit var blinkToggle: ToggleButton
    private lateinit var btnEmail: Button
    private var isFlashlightOn = false
    private var blinkMode = false
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flashlightButton = findViewById(R.id.flashlight_button)
        button5Min = findViewById(R.id.button_5min)
        button10Min = findViewById(R.id.button_10min)
        button15Min = findViewById(R.id.button_15min)
        customTimeInput = findViewById(R.id.custom_time_input)
        setCustomTimeButton = findViewById(R.id.set_custom_time_button)
        blinkToggle = findViewById(R.id.blink_toggle)
        btnEmail = findViewById(R.id.btnEmail)

        val hasCameraFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        btnEmail.setOnClickListener {
            val email = "ask.psoni@gmail.com"
            val subject = "Feedback! For improvement.. From Timer Torch App."
            val uriText = "mailto:$email?subject=${Uri.encode(subject)}"
            val emailIntent = Intent(Intent.ACTION_SENDTO, uriText.toUri())
            startActivity(Intent.createChooser(emailIntent, "Feedback! For improvement.."))
        }

        flashlightButton.setOnClickListener {
            if (hasCameraFlash) {
                if (isFlashlightOn) {
                    stopBlinking() // Stop blinking if active
                    cancelTimer() // Stop any active timer
                    turnOffFlashlight()
                } else {
                    turnOnFlashlight()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_flashlight), Toast.LENGTH_SHORT).show()
            }
        }

        button5Min.setOnClickListener { setTimer(5) }
        button10Min.setOnClickListener { setTimer(10) }
        button15Min.setOnClickListener { setTimer(15) }

        setCustomTimeButton.setOnClickListener {
            if (timer == null) {
                val customTime = customTimeInput.text.toString().toIntOrNull()
                if (customTime != null) {
                    setTimer(customTime)
                } else {
                    Toast.makeText(this, getString(R.string.please_enter_valid_time), Toast.LENGTH_SHORT).show()
                }
            } else {
                cancelTimer()
            }
        }

        blinkToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startBlinking()
                blinkToggle.text = getString(R.string.blink_on) // Update UI
            } else {
                stopBlinking()
                blinkToggle.text = getString(R.string.blink_off) // Update UI
            }
        }
    }

    private fun setTimer(minutes: Int) {
        timer?.cancel()
        timer = object : CountDownTimer((minutes * 60000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!blinkMode) {
                    if (!isFlashlightOn) turnOnFlashlight() // Prevents overriding manual off
                }
            }
            override fun onFinish() {
                turnOffFlashlight()
                Toast.makeText(this@MainActivity, getString(R.string.timer_finished), Toast.LENGTH_SHORT).show()
                resetSetCustomTimeButton()
            }
        }.start()
        setCustomTimeButton.text = getString(R.string.cancel_timer)
        setCustomTimeButton.setOnClickListener { cancelTimer() }
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
        stopBlinking() // Ensure blinking stops
        turnOffFlashlight() // Make sure flashlight turns off
        Toast.makeText(this, getString(R.string.timer_canceled), Toast.LENGTH_SHORT).show()
        resetSetCustomTimeButton()
    }

    private fun resetSetCustomTimeButton() {
        setCustomTimeButton.text = getString(R.string.set_custom_time)
        setCustomTimeButton.setOnClickListener {
            val customTime = customTimeInput.text.toString().toIntOrNull()
            if (customTime != null) {
                setTimer(customTime)
            } else {
                Toast.makeText(this, getString(R.string.please_enter_valid_time), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun turnOnFlashlight() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, true)
            isFlashlightOn = true
            flashlightButton.text = getString(R.string.turn_off)

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                setSize(200, 200)
            }
            flashlightButton.background = shape
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun turnOffFlashlight() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, false)
            isFlashlightOn = false
            flashlightButton.text = getString(R.string.turn_on)

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                setSize(200, 200)
            }
            flashlightButton.background = shape
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // Blink Mode function
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var blinkRunnable: Runnable? = null

    private fun startBlinking() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(this, getString(R.string.no_flashlight), Toast.LENGTH_SHORT).show()
            return
        }

        blinkRunnable = object : Runnable {
            override fun run() {
                if (isFlashlightOn) {
                    turnOffFlashlight()
                } else {
                    turnOnFlashlight()
                }
                handler.postDelayed(this, 1000) // Blinks every 1 second
            }
        }
        handler.post(blinkRunnable!!)
    }

    private fun stopBlinking() {
        blinkRunnable?.let { handler.removeCallbacks(it) }
        blinkMode = false // Ensure the mode is off
        turnOffFlashlight() // Force turn off
    }
}
