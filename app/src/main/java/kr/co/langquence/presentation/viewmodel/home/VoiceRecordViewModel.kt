package kr.co.langquence.presentation.viewmodel.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.langquence.common.helper.AudioRecorder
import kr.co.langquence.common.helper.RecordingTimer
import kr.co.langquence.common.utils.WavUtil
import kr.co.langquence.model.domain.Resource
import kr.co.langquence.model.usecase.CorrectUseCase
import kr.co.langquence.presentation.viewmodel.state.CorrectState
import javax.inject.Inject

private val log = KotlinLogging.logger {}

sealed class VoiceRecognitionState {
    object Idle : VoiceRecognitionState()
    object Listening : VoiceRecognitionState()
    object NoInput : VoiceRecognitionState()
    data class Success(val text: String) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

@HiltViewModel
class VoiceRecordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val correctUseCase: CorrectUseCase
) : ViewModel() {
    private companion object {
        const val MAX_RECORDING_TIME_MS = 60000L
        const val COUNTDOWN_INTERVAL = 1000L
    }

    private val audioRecorder = AudioRecorder(context, viewModelScope)
    private val recordingTimer = RecordingTimer(
        maxTimeMs = MAX_RECORDING_TIME_MS,
        intervalMs = COUNTDOWN_INTERVAL,
        onFinish = { stopVoiceRecord() }
    )

    private val _voiceState = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()

    private val _permissionRequest = MutableStateFlow(false)
    val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

    val timerValue: StateFlow<Int> = recordingTimer.timerValue

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

    private fun startVoiceRecord() {
        if (!audioRecorder.hasAudioPermission()) {
            requestAudioPermission()
            return
        }

        if (audioRecorder.startRecording()) {
            recordingTimer.start()
            _voiceState.value = VoiceRecognitionState.Listening
        } else {
            _voiceState.value = VoiceRecognitionState.Error("오디오 녹음을 초기화할 수 없습니다")
        }
    }

    private fun stopVoiceRecord() {
        log.info { "Stop voice record" }
        recordingTimer.cancel()

        val result = audioRecorder.stopRecording()
            ?: run {
                log.warn { "No audio data recorded!" }
                _voiceState.value = VoiceRecognitionState.NoInput
                return
            }

        val (audioBytes, sampleRate) = result
        val wavBytes = WavUtil.addWavHeader(audioBytes, sampleRate)
        log.info { "Created WAV stream with size: ${wavBytes.size} bytes" }

        _voiceState.value = VoiceRecognitionState.Success("성공")
        requestCorrectAnswer(wavBytes)
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

    override fun onCleared() {
        super.onCleared()
        recordingTimer.cancel()
        audioRecorder.release()
    }
}