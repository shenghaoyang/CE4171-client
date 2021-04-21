package info.shenghaoyang.ntu.ntu_iot_course

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import info.shenghaoyang.ntu.ntu_iot.dlserver.DLServerGrpc
import info.shenghaoyang.ntu.ntu_iot.dlserver.Dlserver
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*

const val LABEL_KEY = "info.shenghaoyang.ntu.ntu_iot_course.label"
val LABEL_TO_TEXT = arrayOf("DOWN", "GO", "LEFT", "NO", "RIGHT", "STOP", "UP", "YES")


/**
 * Activity that is used for displaying inference results.
 *
 * Also allows the user to suggest a different label for the recorded audio.
 */
class InferResultActivity : AppCompatActivity() {
    private lateinit var labels: Spinner
    private lateinit var label: TextView
    private lateinit var suggestButton: Button
    private lateinit var stub: DLServerGrpc.DLServerBlockingStub
    private lateinit var scope: CoroutineScope
    private lateinit var channel: ManagedChannel
    private lateinit var audioSamples: FloatArray

    /**
     * Called when the activity is created.
     *
     * Extracts the audio samples from the intent and populates the suggestion spinner's
     * items.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_infer_result)

        labels = findViewById(R.id.suggested_label)
        label = findViewById(R.id.label_string)
        suggestButton = findViewById(R.id.submit_suggested_label)
        audioSamples = this.intent.getFloatArrayExtra(AUDIO_SAMPLES_KEY)!!
        labels.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, LABEL_TO_TEXT)

        val dataLabel = this.intent.getIntExtra(LABEL_KEY, 0)

        /* Set label text */
        label.text = LABEL_TO_TEXT[dataLabel]
    }

    /**
     * Called when the activity is finally visible as the top activity.
     *
     * Starts a gRPC connection to the deep learning server.
     */
    override fun onResume() {
        super.onResume()
        scope = CoroutineScope(Job() + Dispatchers.IO)
        channel = ManagedChannelBuilder.forTarget(getString(R.string.dl_server_uri)).usePlaintext().build()
        stub = DLServerGrpc.newBlockingStub(channel)
    }

    override fun onPause() {
        super.onPause()
        channel.shutdownNow()
        scope.cancel()
    }

    /**
     * Called when the submit suggestion button has been pressed.
     *
     * Sends the suggestion to the deep learning server and returns to the main activity,
     * in order to make sure that the user can only send one suggestion per recording.
     */
    fun submitPressed(view: View) {
        view.isEnabled = false
        scope.launch {
            val req = Dlserver.TrainingRequest.newBuilder()
            for (v in audioSamples) {
                req.addAudioSamples(v)
            }
            req.label = Dlserver.Label.forNumber(labels.selectedItemPosition)
            stub.train(req.build())

            withContext(Dispatchers.Main) {
                view.isEnabled = true
                Toast.makeText(this@InferResultActivity, "Suggestion successfully submitted", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@InferResultActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
