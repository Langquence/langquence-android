package kr.co.langquence.common.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRouting
import android.media.MediaRecorder.AudioSource
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

private val log = KotlinLogging.logger {}

/**
 * 오디오 녹음을 담당하는 클래스
 */
class AudioRecorder(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        const val RECORDER_AUDIO_SOURCE = AudioSource.VOICE_RECOGNITION
        const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = AtomicBoolean(false)
    private var recordSampleRate = 0
    private var bufferSizeInBytes = 0
    private lateinit var audioData: ByteArrayOutputStream
    private val audioHandler = Handler(Looper.getMainLooper())

    private val audioRoutingListener = AudioRouting.OnRoutingChangedListener { router ->
        log.info { "onRoutingChanged: $router" }
        val currentRoutes = router.routedDevice
        log.info { "현재 라우팅 장치: ${currentRoutes?.productName}" }
        log.info { "audio 녹음 상태 : ${audioRecord?.recordingState?.equals(AudioRecord.RECORDSTATE_RECORDING)}" }
    }

    /**
     * 오디오 녹음 시작
     * @return 성공 여부
     */
    fun startRecording(): Boolean {
        audioRecord = createCompatibleAudioRecord() ?: run {
            log.error { "AudioRecord is not initialized" }
            return false
        }

        audioRecord!!.addOnRoutingChangedListener(audioRoutingListener, audioHandler)
        startRecordingCoroutine()
        audioRecord!!.startRecording()
        return true
    }

    /**
     * 오디오 녹음 중지
     * @return 녹음된 PCM 데이터와 샘플레이트
     */
    fun stopRecording(): Pair<ByteArray, Int>? {
        log.info { "Stop voice record" }

        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null

        val audioBytes = audioData.toByteArray()
        log.info { "Total recorded audio: ${audioBytes.size} bytes" }

        if (audioBytes.isEmpty()) {
            log.warn { "No audio data recorded!" }
            releaseAudioRecord()
            return null
        }

        val avgVolume = calcAverageVolume(audioBytes, audioBytes.size)
        log.info { "Average audio volume from entire recording: $avgVolume (total bytes: ${audioBytes.size})" }

        releaseAudioRecord()
        return Pair(audioBytes, recordSampleRate)
    }

    /**
     * 녹음 자원 해제
     */
    fun release() {
        releaseAudioRecord()
    }

    /**
     * 사용 가능한 오디오 레코드 생성을 시도합니다.
     * 여러 샘플링 레이트를 시도하여 가장 먼저 초기화에 성공한 AudioRecord를 반환합니다.
     *
     * @return 초기화에 성공한 AudioRecord 인스턴스 또는 null (권한 없음 포함)
     */
    private fun createCompatibleAudioRecord(): AudioRecord? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            log.info { "No audio permission" }
            return null
        }

        var audioRecord: AudioRecord?
        val sampleRateCandidates = intArrayOf(44100, 22050, 11025, 16000)

        for (sampleRate in sampleRateCandidates) {
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
            )

            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                log.error { "Invalid parameter for AudioRecord for sample rate $sampleRate" }
                continue
            }

            audioRecord = AudioRecord.Builder()
                .setAudioSource(RECORDER_AUDIO_SOURCE)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(RECORDER_AUDIO_ENCODING)
                        .setSampleRate(sampleRate)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .build()

            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                log.info { "AudioRecord initialized with sample rate: $sampleRate, buffer size: $minBufferSize" }
                recordSampleRate = sampleRate
                bufferSizeInBytes = minBufferSize
                return audioRecord
            } else {
                audioRecord.release()
            }
        }

        log.error { "Failed to initialize AudioRecord with any sample rate" }
        return null
    }

    /**
     * 오디오 녹음 코루틴을 시작합니다.
     */
    private fun startRecordingCoroutine() {
        audioData = ByteArrayOutputStream()
        isRecording.set(true)

        recordingJob = coroutineScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSizeInBytes)

            while (isRecording.get() && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (bytesRead > 0) {
                    collectAudioData(buffer, bytesRead)
                }
            }

            log.info { "Recording finished, total bytes: ${audioData.size()}" }
        }
    }

    /**
     * 오디오 데이터를 수집하고 처리합니다.
     */
    private fun collectAudioData(buffer: ByteArray, bytesRead: Int) {
        audioData.write(buffer, 0, bytesRead)

        val volume = calcAverageVolume(buffer, bytesRead)
        if (audioData.size() % (buffer.size * 10) == 0) {
            log.debug { "Current volume: $volume" }
        }
    }

    /**
     * 오디오 데이터의 평균 볼륨을 계산합니다.
     */
    private fun calcAverageVolume(buffer: ByteArray, numberOfReadBytes: Int): Double {
        var totalAbsValue = 0.0

        for (i in 0 until numberOfReadBytes step 2) {
            if (i + 1 < buffer.size) {
                val sample = (buffer[i].toInt() and 0xFF) or ((buffer[i + 1].toInt() and 0xFF) shl 8)
                totalAbsValue += abs(sample)
            }
        }

        val sampleCnt = numberOfReadBytes / 2
        return if (numberOfReadBytes > 0) totalAbsValue / sampleCnt else 0.0
    }

    /**
     * AudioRecord 자원을 해제합니다.
     */
    private fun releaseAudioRecord() {
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                removeOnRoutingChangedListener(audioRoutingListener)
                stop()
                release()
            }
        }
        audioRecord = null
    }

    /**
     * 오디오 권한이 있는지 확인합니다.
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}