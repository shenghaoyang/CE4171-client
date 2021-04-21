package info.shenghaoyang.ntu.ntu_iot_course

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Main activity for the deep learning offload application.
 *
 * Seeks permissions to record audio and provides a button to start audio recording.
 */
class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Recording", "audio recording permissions granted")
        } else {
            Log.d("Recording", "audio recording permissions not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO
            ) -> {

            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    /**
     * Called when the record button is pressed.
     *
     * Starts the recording activity.
     */
    fun recordAudio(view: View) {
        startActivity(Intent(this, RecordProgress::class.java))
    }
}
