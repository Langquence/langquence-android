package kr.co.langquence.common.helper

import android.os.CountDownTimer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val log = KotlinLogging.logger {}

/**
 * 녹음 타이머를 관리하는 클래스
 *
 * @property maxTimeMs 타이머 최대 시간 (기본값: 60초)
 * @property intervalMs 타이머 간격 (기본값: 1초)
 * @property onFinish 타이머 종료 시 실행할 람다 함수
 */
class RecordingTimer(
    private val maxTimeMs: Long = 60000L,
    private val intervalMs: Long = 1000L,
    private val onFinish: () -> Unit
) {
    private val _timerValue = MutableStateFlow((maxTimeMs / 1000L).toInt())
    val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    /**
     * 타이머 시작
     */
    fun start() {
        _timerValue.value = (maxTimeMs / 1000L).toInt()

        countDownTimer = object : CountDownTimer(maxTimeMs, intervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.value = (millisUntilFinished / 1000).toInt()

                _timerValue.value.takeIf { it % 10 == 0 }
                    ?.let { log.info { "Timer: ${_timerValue.value} seconds remaining" } }
            }

            override fun onFinish() {
                log.info { "Timer finished" }
                onFinish()
            }
        }.start()
    }

    /**
     * 타이머 취소
     */
    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
}