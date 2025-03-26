package kr.co.langquence.presentation.viewmodel.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRouting
import android.media.MediaRecorder.AudioSource
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.langquence.common.utils.AudioUtils.addWavHeader
import kr.co.langquence.model.domain.Resource
import kr.co.langquence.model.usecase.CorrectUseCase
import kr.co.langquence.presentation.viewmodel.state.CorrectState
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.abs

private val log = KotlinLogging.logger {}

/**
 * 음성 인식 상태를 나타내는 Sealed 클래스
 */
sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    object NoInput : VoiceRecognitionState()
    data class Success(val text: String) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

/**
 * 음성 녹음 및 인식을 관리하는 ViewModel
 */
@HiltViewModel
class VoiceRecordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val correctUseCase: CorrectUseCase
) : ViewModel() {
    private companion object {
        const val MAX_RECORDING_TIME_MS = 60000L
        const val COUNTDOWN_INTERVAL = 1000L
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
    private var countDownTimer: CountDownTimer? = null
    private val audioHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()

    private val _permissionRequest = MutableStateFlow(false)
    val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

    private val _timerValue = MutableStateFlow(60)
    val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

    private val _correctState: MutableStateFlow<CorrectState> = MutableStateFlow(CorrectState())
    val correctState: StateFlow<CorrectState> = _correctState.asStateFlow()

    /**
     * 음성 녹음 트리거.
     * 현재 상태가 [Idle][VoiceRecognitionState]이면 녹음을 시작하고, [Listening][VoiceRecognitionState]이면 녹음을 중지한다.
     *
     * @see VoiceRecognitionState
     */
    fun toggleListeningMode() {
        log.info { "Toggle listening mode. Current state: ${_voiceState.value}" }

        when (_voiceState.value) {
            VoiceRecognitionState.Idle -> startVoiceRecord()
            VoiceRecognitionState.Listening -> stopVoiceRecord()
            else -> {
                log.warn { "Can't toggle listening mode, ignoring. current state : ${_voiceState.value}" }

                _voiceState.value = VoiceRecognitionState.Idle
            }
        }
    }

    /**
     * 마이크 권한 요청 이벤트 초기화
     */
    fun resetPermissionRequest() {
        _permissionRequest.value = false
    }

    /**
     * 마이크 권한 요청 이벤트 발행
     */
    private fun requestAudioPermission() {
        log.info { "Request audio permission" }
        _permissionRequest.value = true
    }

    /**
     * 음성 녹음을 시작합니다.
     */
    private fun startVoiceRecord() {
        audioRecord = createCompatibleAudioRecord()
            ?: run {
                log.error { "AudioRecord is not initialized" }
                _voiceState.value = VoiceRecognitionState.Error("AudioRecord is not initialized")
                return
            }

        audioRecord!!.addOnRoutingChangedListener(audioRoutingListener, audioHandler)
        startRecordingCoroutine()
        audioRecord!!.startRecording()
        startTimer()
        _voiceState.value = VoiceRecognitionState.Listening
    }

    /**
     * 사용 가능한 오디오 레코드 생성을 시도합니다.
     * 여러 샘플링 레이트를 시도하여 가장 먼저 초기화에 성공한 AudioRecord를 반환합니다.
     *
     * @return 초기화에 성공한 AudioRecord 인스턴스 또는 null
     */
    private fun createCompatibleAudioRecord(): AudioRecord? {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            .takeIf { it == PackageManager.PERMISSION_GRANTED }
            ?.let {
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
                        .setBufferSizeInBytes(minBufferSize * 2)
                        .build()

                    if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                        log.info { "AudioRecord initialized with sample rate: $sampleRate, buffer size: ${minBufferSize * 2}" }
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
            ?: run {
                requestAudioPermission()
                return null
            }
    }

    /**
     * 오디오 녹음 코루틴을 시작합니다.
     */
    private fun startRecordingCoroutine() {
        audioData = ByteArrayOutputStream()
        isRecording.set(true)

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
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
            log.info { "Current volume: $volume" }
        }
    }

    private fun startTimer() {
        _timerValue.value = 60

        countDownTimer = object : CountDownTimer(MAX_RECORDING_TIME_MS, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.value = (millisUntilFinished / 1000).toInt()

                _timerValue.value.takeIf { it % 10 == 0 }
                    ?.let { log.info { "Timer: ${_timerValue.value} seconds remaining" } }
            }

            override fun onFinish() {
                log.info { "Timer finished" }
                stopVoiceRecord()
            }
        }.start()
    }

    private fun stopVoiceRecord() {
        log.info { "Stop voice record" }

        isRecording.set(false)
        cancelTimer()

        recordingJob?.cancel()
        recordingJob = null

        val audioBytes = audioData.toByteArray()
        log.info { "Total recorded audio: ${audioBytes.size} bytes" }

        if (audioBytes.isEmpty()) {
            log.warn { "No audio data recorded!" }
            _voiceState.value = VoiceRecognitionState.NoInput
        } else {
            val avgVolume = calcAverageVolume(audioBytes, audioBytes.size)
            log.info { "Average audio volume from entire recording: $avgVolume (total bytes: ${audioBytes.size})" }

            val waveBytes = addWavHeader(audioBytes, recordSampleRate)
            log.info { "Created WAV stream with size: ${waveBytes.size} bytes" }

            _voiceState.value = VoiceRecognitionState.Success("성공")
            requestCorrectAnswer(waveBytes)
        }

        audioRecord!!.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                removeOnRoutingChangedListener(audioRoutingListener)
                stop()
                release()
            }
        }
        audioRecord = null
    }

    private fun calcAverageVolume(buffer: ByteArray, numberOfReadBytes: Int): Double {
        var totalAbsValue = 0.0

        for (i in 0 until numberOfReadBytes step 2) {
            if (i + 1 < buffer.size) {
                val sample = (buffer[i].toInt() and 0xFF) or ((buffer[i + 1].toInt() and 0xFF) shl 8)
                totalAbsValue += abs(sample)
            }
        }

        val sampleCnt = numberOfReadBytes / 2

        log.debug { "totalAbsValue = $totalAbsValue, sampleCnt = $sampleCnt, so average volume is ${if (numberOfReadBytes > 0) totalAbsValue / sampleCnt else 0.0}%" }

        return if (numberOfReadBytes > 0) totalAbsValue / sampleCnt else 0.0
    }

    /**
     * 타이머 취소
     */
    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun requestCorrectAnswer(bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log.info { "Starting network request with ${bytes.size} bytes" }

                correctUseCase.invoke(bytes)
                    .collect { result ->
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is Resource.Success -> {
                                    log.info { "Network request succeeded" }
                                    _correctState.value = CorrectState(data = result.data)
                                }

                                is Resource.Loading -> {
                                    log.info { "Network request loading..." }
                                }

                                is Resource.Error -> {
                                    log.error { "Network request failed: ${result.message}" }
                                    _correctState.value = CorrectState(reason = result.message)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                log.error(e) { "Error during network request: ${e.message}" }
                withContext(Dispatchers.Main) {
                    _correctState.value = CorrectState(reason = "네트워크 오류: ${e.message}")
                }
            }
        }
    }

    private val audioRoutingListener = AudioRouting.OnRoutingChangedListener { router ->
        log.info { "onRoutingChanged: $router" }

        val currentRoutes = router.routedDevice
        log.info { "현재 라우팅 장치: ${currentRoutes?.productName}" }

        log.info { "audio 녹음 상태 : ${audioRecord?.recordingState?.equals(AudioRecord.RECORDSTATE_RECORDING)}" }
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                removeOnRoutingChangedListener(audioRoutingListener)
                stop()
                release()
            }
        }
        audioRecord = null
    }
}