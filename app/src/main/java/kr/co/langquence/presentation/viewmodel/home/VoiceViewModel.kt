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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
	private var speechRecognizer: SpeechRecognizer? = null

	private val _voiceState = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
	val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()

	private val _permissionRequest = MutableStateFlow(false)
	val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

    /**
     * 음성 인식 모드 토글
     */
    fun toggleListeningMode() {
        viewModelScope.launch {
	        if (!hasAudioPermission()) {
		        requestAudioPermission()
		        return@launch
	        }
	        log.info { "current voice state : ${_voiceState.value}" }

            when (_voiceState.value) {
                is VoiceRecognitionState.Idle -> {
					startVoiceRecognition()
                }
                is VoiceRecognitionState.Listening -> {
                    stopVoiceRecognition()
                }
                is VoiceRecognitionState.NoInput,
                is VoiceRecognitionState.Success,
                is VoiceRecognitionState.Error -> {
					log.error { "VoiceViewModel's state is Error" }

                    _voiceState.value = VoiceRecognitionState.Idle
                }
            }
        }
    }

	fun resetPermissionRequest() {
		_permissionRequest.value = false
	}

	/**
	 * 마이크 권한 체크
	 */
	private fun hasAudioPermission(): Boolean {
		return ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.RECORD_AUDIO
		) == PackageManager.PERMISSION_GRANTED
	}

	/**
	 * 마이크 권한 요청 이벤트 발행
	 */
	private fun requestAudioPermission() {
		log.info { "Request audio permission" }
		_permissionRequest.value = true
	}

	/**
	 * 음성 인식 시작
	 */
	private fun startVoiceRecognition() {
		val intent = createSpeechRecognitionIntent()

		val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
		speechRecognizer.setRecognitionListener(recognitionListener)

		_voiceState.value = VoiceRecognitionState.Listening
		this.speechRecognizer = speechRecognizer

		speechRecognizer.startListening(intent)
	}

	private fun createSpeechRecognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
		putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
		putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
		putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
		putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
		putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
	}

	/**
	 * 음성 인식 중단
	 */
	private fun stopVoiceRecognition() {
		log.info { "Stopping voice recognition, current state: ${_voiceState.value}" }

		speechRecognizer?.apply {
			stopListening()
			destroy()
		}
		speechRecognizer = null
	}

    /**
     * 음성 인식 리스너
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
			log.info { "Speech recognition ready for speech" }
		}

        override fun onBeginningOfSpeech() {
			log.info { "Beginning of speech detected" }
		}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
	        log.info { "End of speech detected" }
		}

        override fun onError(error: Int) {
            val errorMessage = getSpeechRecognitionErrorMessage(error)
	        log.error { "Speech recognition error: $errorMessage (code: $error)" }

	        when (error) {
		        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
			        _voiceState.value = VoiceRecognitionState.NoInput
		        }
		        else -> {
			        _voiceState.value = VoiceRecognitionState.Error(errorMessage)
		        }
	        }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
	        log.info { "Speech recognition results received: ${matches?.size ?: 0} matches" }

            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                if (recognizedText.isNotBlank()) {
	                log.info { "Successfully recognized: \"$recognizedText\"" }

                    _voiceState.value = VoiceRecognitionState.Success(recognizedText)
                } else {
	                log.warn { "Recognized text was blank" }

                    _voiceState.value = VoiceRecognitionState.NoInput
                }
            } else {
	            log.warn { "No recognition matches found" }

                _voiceState.value = VoiceRecognitionState.NoInput
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

	private fun getSpeechRecognitionErrorMessage(error: Int): String = when (error) {
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
}