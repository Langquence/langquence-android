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
import kotlinx.coroutines.flow.*
import kr.co.langquence.model.domain.Resource
import kr.co.langquence.model.usecase.CorrectUseCase
import kr.co.langquence.presentation.viewmodel.state.CorrectState
import javax.inject.Inject
import kotlin.math.abs

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
): ViewModel() {
	private companion object {
		const val SAMPLE_RATE = 44100
		const val MIN_BUFF_SIZE = 1024
		const val MAX_RECORDING_TIME_MS = 60000L
		const val COUNTDOWN_INTERVAL = 1000L
	}

	private var audioRecord: AudioRecord? = null

	private val _voiceState = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
	val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()

	private val _permissionRequest = MutableStateFlow(false)
	val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

	private val _timerValue = MutableStateFlow(60)
	val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

	private val _correctState: MutableStateFlow<CorrectState> = MutableStateFlow(CorrectState())
	val correctState: StateFlow<CorrectState> = _correctState.asStateFlow()

	private var countDownTimer: CountDownTimer? = null
	private val audioHandler = Handler(Looper.getMainLooper())

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
		ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
			.takeIf { it == PackageManager.PERMISSION_GRANTED }
			?.let {
				audioRecord = AudioRecord.Builder()
					.setAudioSource(AudioSource.MIC)
					.setAudioFormat(
						AudioFormat.Builder()
							.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
							.setSampleRate(SAMPLE_RATE)
							.setChannelMask(AudioFormat.CHANNEL_IN_MONO)
							.build()
					)
					.setBufferSizeInBytes(2 * MIN_BUFF_SIZE)
					.build()
			}
			?: run {
				requestAudioPermission()
			}

		if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
			log.info { "AudioRecord initialized, starting recording" }
			audioRecord!!.apply {
				addOnRoutingChangedListener(audioRoutingListener, audioHandler)
				startRecording()
			}
			startTimer()
			_voiceState.value = VoiceRecognitionState.Listening
		} else {
			log.error { "AudioRecord initialization failed" }
			_voiceState.value = VoiceRecognitionState.Error("AudioRecord is not initialized")
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

		cancelTimer()

		val buffer = ByteArray(2 * MIN_BUFF_SIZE)
		val bytesRead = audioRecord?.read(buffer, 0, 2 * MIN_BUFF_SIZE)
		val avgVolume = bytesRead?.let { calcAverageVolume(buffer, it) }

		log.info { "Average audio volume: $avgVolume" }

		_voiceState.value = if (bytesRead != null && bytesRead > 0) {
			VoiceRecognitionState.Success("성공")
		} else {
			VoiceRecognitionState.NoInput
		}

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

	private fun calcAverageVolume(buffer: ByteArray, bytesRead: Int): Double {
		var sum = 0.0
		for (i in 0 until bytesRead step 2) {
			if (i + 1 < buffer.size) {
				val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
				sum += abs(sample)
			}
		}
		return if (bytesRead > 0) sum / (bytesRead / 2) else 0.0
	}

	/**
	 * 타이머 취소
	 */
	private fun cancelTimer() {
		countDownTimer?.cancel()
		countDownTimer = null
	}

	private fun requestCorrectAnswer(bytes: ByteArray) {
		correctUseCase.invoke(bytes)
			.onEach { result ->
				when(result) {
					is Resource.Success -> {
						_correctState.value = CorrectState(data = result.data)
					}
					is Resource.Loading -> {}
					is Resource.Error -> {
						log.error { "failed ViewModel ${result.message}" }
						_correctState.value = CorrectState(reason = result.message)
					}
				}
			}.launchIn(viewModelScope)
	}

	private val audioRoutingListener = AudioRouting.OnRoutingChangedListener { router ->
		log.info { "onRoutingChanged: $router" }

		val currentRoutes = router.routedDevice
		log.info { "현재 라우팅 장치: ${currentRoutes?.productName}" }
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