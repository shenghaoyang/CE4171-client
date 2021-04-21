package info.shenghaoyang.ntu.ntu_iot_course

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import info.shenghaoyang.ntu.ntu_iot.dlserver.DLServerGrpc
import info.shenghaoyang.ntu.ntu_iot.dlserver.Dlserver
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlin.math.max

const val PLAYBACK_CHANNEL_SPECIFICATION = AudioFormat.CHANNEL_OUT_MONO


/**
 * Activity launched to preview the recording and allow the user to send the recording
 * to the remote server for inference.
 */
class PreviewActivity : AppCompatActivity() {
    private lateinit var audioSamples: FloatArray
    private lateinit var track: AudioTrack
    private lateinit var channel: ManagedChannel
    private lateinit var stub: DLServerGrpc.DLServerBlockingStub
    private lateinit var scope: CoroutineScope

    /**
     * Called when this activity is created.
     *
     * Reads the audio samples sent to it from the recording activity and creates a
     * player tha is used to play these samples.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.intent.extras!!
        audioSamples = bundle.getFloatArray(AUDIO_SAMPLES_KEY)!!
        setContentView(R.layout.activity_preview)

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, PLAYBACK_CHANNEL_SPECIFICATION, ENCODING)
        track = AudioTrack.Builder()
                .setAudioAttributes(
                        AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                .setAudioFormat(
                        AudioFormat.Builder()
                                .setEncoding(ENCODING)
                                .setChannelMask(PLAYBACK_CHANNEL_SPECIFICATION)
                                .setSampleRate(SAMPLE_RATE)
                                .build()
                )
                .setBufferSizeInBytes(max(audioSamples.size * 4, minBufferSize))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

        /* Small enough blocking writes shouldn't be a problem */
        track.write(audioSamples, 0, audioSamples.size, AudioTrack.WRITE_BLOCKING)
    }

    /**
     * Called when this activity is visible.
     *
     * Creates a gRPC connection to the remote deep learning server.
     */
    override fun onResume() {
        super.onResume()
        scope = CoroutineScope(Job() + Dispatchers.IO)
        channel = ManagedChannelBuilder.forTarget(getString(R.string.dl_server_uri)).usePlaintext().build()
        stub = DLServerGrpc.newBlockingStub(channel)
    }

    override fun onPause() {
        super.onPause()
        track.pause()
        channel.shutdownNow()
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        track.release()
    }

    /**
     * Called when the play button is pressed.
     *
     * Starts playing the recorded sample.
     */
    fun playPressed(view: View) {
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    fun inferPressed(view: View) {
        view.isEnabled = false
        scope.launch {
            val req = Dlserver.InferenceRequest.newBuilder()
            for (v in audioSamples) {
                req.addAudioSamples(v)
            }
            val response = stub.infer(req.build())
            Log.d("infer_response", response.label.number.toString())

            withContext(Dispatchers.Main) {
                view.isEnabled = true
                val intent = Intent(this@PreviewActivity, InferResultActivity::class.java)
                intent.putExtra(LABEL_KEY, response.label.number)
                intent.putExtra(AUDIO_SAMPLES_KEY, audioSamples)
                startActivity(intent)
            }
        }
    }

}