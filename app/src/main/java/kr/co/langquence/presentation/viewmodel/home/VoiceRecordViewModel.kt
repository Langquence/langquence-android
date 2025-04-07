package kr.co.langquence.presentation.viewmodel.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

data class VoiceRecordUiState(
    val recordState: VoiceRecognitionState = VoiceRecognitionState.Idle,
    val permissionRequest: Boolean = false,
    val timerValue: Int = 0,
    val correctState: CorrectState = CorrectState()
)

sealed class VoiceRecognitionState {
    data object Idle : VoiceRecognitionState()
    data object Listening : VoiceRecognitionState()
    data object NoInput : VoiceRecognitionState()
    data object Networking : VoiceRecognitionState()
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
        intervalMs = COUNTDOWN_INTERVAL
    )

    private val _uiState = MutableStateFlow(VoiceRecordUiState())
    val uiState: StateFlow<VoiceRecordUiState> = _uiState.asStateFlow()

    init {
        // 타이머 시간 수집
        viewModelScope.launch {
            recordingTimer.timerValue.collect { seconds ->
                _uiState.update { it.copy(timerValue = seconds) }
            }
        }

        // 타이머 완료 감지
        viewModelScope.launch {
            recordingTimer.isFinished
                .filter { it }
                .collect {
                    stopVoiceRecord()
                }
        }
    }

    /**
     * 음성 녹음 트리거.
     * 현재 상태가 [Idle][VoiceRecognitionState]이면 녹음을 시작하고, [Listening][VoiceRecognitionState]이면 녹음을 중지한다.
     *
     * @see VoiceRecognitionState
     */
    fun toggleListeningMode() {
        log.info { "Toggle listening mode. Current state: ${_uiState.value.recordState}" }

        if (_uiState.value.recordState is VoiceRecognitionState.Networking) {
            log.warn { "Can't toggle listening mode, ignoring." }
            return
        }

        when (_uiState.value.recordState) {
            VoiceRecognitionState.Idle -> startVoiceRecord()
            VoiceRecognitionState.Listening -> stopVoiceRecord()
            else -> {
                log.warn { "Can't toggle listening mode, ignoring. current state : ${_uiState.value.recordState}" }
                _uiState.update { it.copy(recordState = VoiceRecognitionState.Idle) }
            }
        }
    }

    /**
     * 마이크 권한 요청 이벤트 초기화
     */
    fun resetPermissionRequest() {
        _uiState.update { currentState ->
            currentState.copy(permissionRequest = false)
        }
    }

    /**
     * 마이크 권한 요청 이벤트 발행
     */
    private fun requestAudioPermission() {
        log.info { "Request audio permission" }
        _uiState.update { currentState ->
            currentState.copy(permissionRequest = true)
        }
    }

    private fun startVoiceRecord() {
        if (!audioRecorder.hasAudioPermission()) {
            requestAudioPermission()
            return
        }

        if (audioRecorder.startRecording()) {
            recordingTimer.start()
            _uiState.update { currentState ->
                currentState.copy(recordState = VoiceRecognitionState.Listening)
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(recordState = VoiceRecognitionState.Error("It is not possible to initialize audio recording"))
            }
        }
    }

    private fun stopVoiceRecord() {
        log.info { "Stop voice record" }
        recordingTimer.cancel()

        return convertPcmToWav()
    }

    private fun convertPcmToWav() {
        val result = audioRecorder.stopRecording()
            ?: run {
                log.warn { "No audio data recorded!" }

                _uiState.update { currentState ->
                    currentState.copy(recordState = VoiceRecognitionState.NoInput)
                }

                return
            }

        val (audioBytes, sampleRate) = result
        val wavBytes = WavUtil.addWavHeader(audioBytes, sampleRate)
        log.info { "Created WAV stream with size: ${wavBytes.size} bytes" }

        _uiState.update { currentState ->
            currentState.copy(recordState = VoiceRecognitionState.Networking)
        }

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
                                    _uiState.update { currentState ->
                                        currentState.copy(
                                            recordState = VoiceRecognitionState.Success("성공"),
                                            correctState = CorrectState(data = result.data)
                                        )
                                    }
                                }

                                is Resource.Loading -> {
                                    log.info { "Network request loading..." }
                                }

                                is Resource.Error -> {
                                    log.error { "Network request failed: ${result.message}" }
                                    _uiState.update { currentState ->
                                        currentState.copy(
                                            recordState = VoiceRecognitionState.Error("에러"),
                                            correctState = CorrectState(reason = result.message)
                                        )
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                log.error(e) { "Error during network request: ${e.message}" }
                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            recordState = VoiceRecognitionState.Error("에러"),
                            correctState = CorrectState(reason = "Network error: ${e.message}")
                        )
                    }
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