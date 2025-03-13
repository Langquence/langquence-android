package kr.co.langquence.presentation.viewmodel.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VoiceRecognitionState {
	object Idle : VoiceRecognitionState()
	object Listening : VoiceRecognitionState()
	object NoInput : VoiceRecognitionState()
	data class Success(val text: String) : VoiceRecognitionState()
	data class Error(val message: String) : VoiceRecognitionState()
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
	private var speechRecognizer: SpeechRecognizer? = null

	private val _voiceState = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
	val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()

	val isListeningMode: StateFlow<Boolean>
		get() =
			MutableStateFlow(_voiceState.value == VoiceRecognitionState.Listening)

	private val _hasAudioPermission = MutableStateFlow(false)
	val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

	init {
		checkAudioPermission()
		initSpeechRecognizer()
	}

	override fun onCleared() {
		super.onCleared()
		speechRecognizer?.destroy()
		speechRecognizer = null
	}

    /**
     * 음성 인식 모드 토글
     */
    fun toggleListeningMode() {
        viewModelScope.launch {
            when (_voiceState.value) {
                is VoiceRecognitionState.Idle -> {
                    if (_hasAudioPermission.value) {
                        startVoiceRecognition()
                    } else {
                        _voiceState.value = VoiceRecognitionState.Error("마이크 권한이 필요합니다")
                    }
                }
                is VoiceRecognitionState.Listening -> {
                    stopVoiceRecognition()
                }
                is VoiceRecognitionState.NoInput,
                is VoiceRecognitionState.Success,
                is VoiceRecognitionState.Error -> {
                    _voiceState.value = VoiceRecognitionState.Idle
                }
            }
        }
    }

	/**
	 * 권한 체크 결과 업데이트
	 */
	fun updatePermissionStatus(granted: Boolean) {
		_hasAudioPermission.value = granted
	}

	/**
	 * 마이크 권한 체크
	 */
	private fun checkAudioPermission() {
		_hasAudioPermission.value = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.RECORD_AUDIO
		) == PackageManager.PERMISSION_GRANTED
	}

	/**
	 * 음성 인식 초기화
	 */
	private fun initSpeechRecognizer() {
		if (SpeechRecognizer.isRecognitionAvailable(context)) {
			speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
			speechRecognizer?.setRecognitionListener(recognitionListener)
		} else {
			_voiceState.value = VoiceRecognitionState.Error("음성 인식을 사용할 수 없습니다")
		}
	}

	/**
	 * 음성 인식 시작
	 */
	private fun startVoiceRecognition() {
		speechRecognizer?.let {
			val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
				putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
				putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
				putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
				putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
			}

			_voiceState.value = VoiceRecognitionState.Listening
			it.startListening(intent)
		}
	}

	/**
	 * 음성 인식 중단
	 */
	private fun stopVoiceRecognition() {
		speechRecognizer?.stopListening()
	}

    /**
     * 음성 인식 리스너
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 에러"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식 서비스 사용 중"
                SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 없음"
                else -> "알 수 없는 에러"
            }

            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _voiceState.value = VoiceRecognitionState.NoInput
            } else {
                _voiceState.value = VoiceRecognitionState.Error(message)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                if (recognizedText.isNotBlank()) {
                    _voiceState.value = VoiceRecognitionState.Success(recognizedText)
                } else {
                    _voiceState.value = VoiceRecognitionState.NoInput
                }
            } else {
                _voiceState.value = VoiceRecognitionState.NoInput
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}