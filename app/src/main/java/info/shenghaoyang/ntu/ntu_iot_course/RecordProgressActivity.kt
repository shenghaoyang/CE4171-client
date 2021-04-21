package info.shenghaoyang.ntu.ntu_iot_course

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.*
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlin.math.round


const val SAMPLE_RATE = 16000
const val BLOCK_SIZE = 5000
const val RECORD_CHANNEL_SPECIFICATION = AudioFormat.CHANNEL_IN_MONO
const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
const val AUDIO_SAMPLES_KEY = "info.shenghaoyang.ntu.ntu_iot_course.audio_samples"

/**
 * Activity presented when the user decides to start recording audio.
 */
class RecordProgress : AppCompatActivity() {
    private val tag = "RecordProgress"
    private lateinit var recorder: AudioRecord
    private lateinit var progress: ProgressBar
    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_progress)
    }

    /**
     * Function called to start recording.
     *
     * Records a second of raw audio, then invokes the preview activity with the
     * recorded data.
     */
    private suspend fun record() {
        withContext(Dispatchers.IO) {
            val recdata = FloatArray(SAMPLE_RATE)
            recorder.startRecording()
            for (i in 0..31) {
                val offset = BLOCK_SIZE * i
                recorder.read(recdata, offset, BLOCK_SIZE, READ_BLOCKING)
                progress.progress = round(100 * (i / 31.0)).toInt()
            }
            recorder.stop()
            Log.d(tag, "Finished recording 1s audio sample")

            val intent = Intent(this@RecordProgress, PreviewActivity::class.java)
            intent.putExtra(AUDIO_SAMPLES_KEY, recdata)
            startActivity(intent)
        }
    }

    /**
     * Called immediately when the user has visibility to the recording activity.
     *
     * Starts recording from the audio recognition source, at 16000Hz with 1 channel.
     */
    override fun onResume() {
        super.onResume()
        progress = findViewById(R.id.recordingProgress)

        val minBufSize = getMinBufferSize(SAMPLE_RATE, RECORD_CHANNEL_SPECIFICATION, ENCODING)
        when (minBufSize) {
            ERROR_BAD_VALUE -> Log.e(tag, "Bad audio recording configuration")
            ERROR -> Log.e(tag, "Unable to query recording hardware")
        }

        recorder = Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                        AudioFormat.Builder()
                                .setEncoding(ENCODING)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(RECORD_CHANNEL_SPECIFICATION)
                                .build()
                )
                .setBufferSizeInBytes(2 * minBufSize)
                .build()

        if (!NoiseSuppressor.isAvailable()) {
            Log.d(tag, "NoiseSuppressor is unavailable")
        } else {
            val s = NoiseSuppressor.create(recorder.audioSessionId)
            if (!s.enabled) {
                Log.d(tag, "NoiseSuppressor created but not enabled")
            }
        }

        scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            record()
        }
    }

    override fun onPause() {
        super.onPause()
        scope.cancel()
        recorder.release()
    }
}