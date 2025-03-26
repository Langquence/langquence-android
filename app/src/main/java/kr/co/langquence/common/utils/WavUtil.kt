package kr.co.langquence.common.utils

import android.content.Context
import android.os.Environment
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private val log = KotlinLogging.logger {}

object AudioUtils {
    /**
     * PCM 데이터에 WAV 헤더를 추가하는 함수
     *
     * @param pcmData PCM 오디오 데이터
     * @param recordSampleRate 녹음 샘플 레이트
     * @return WAV 헤더가 포함된 바이트 배열
     */
    fun addWavHeader(pcmData: ByteArray, recordSampleRate: Int): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36  // 헤더 크기 더하기
        val sampleRate = recordSampleRate.toLong()
        val channels = 1  // 모노
        val bitsPerSample = 16  // 16비트
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toInt()

        val header = ByteArray(44)

        // "RIFF" 청크 디스크립터
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // 파일 크기
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = (totalDataLen shr 8 and 0xffL).toByte()
        header[6] = (totalDataLen shr 16 and 0xffL).toByte()
        header[7] = (totalDataLen shr 24 and 0xffL).toByte()

        // "WAVE" 포맷
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // "fmt " 서브청크
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // 서브청크 크기 (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // 오디오 포맷 (1 = PCM)
        header[20] = 1
        header[21] = 0

        // 채널 수
        header[22] = channels.toByte()
        header[23] = 0

        // 샘플 레이트
        header[24] = (sampleRate and 0xffL).toByte()
        header[25] = (sampleRate shr 8 and 0xffL).toByte()
        header[26] = (sampleRate shr 16 and 0xffL).toByte()
        header[27] = (sampleRate shr 24 and 0xffL).toByte()

        // 바이트 레이트
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = (byteRate shr 8 and 0xffL).toByte()
        header[30] = (byteRate shr 16 and 0xffL).toByte()
        header[31] = (byteRate shr 24 and 0xffL).toByte()

        // 블록 얼라인
        header[32] = blockAlign.toByte()
        header[33] = 0

        // 비트 뎁스
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        // "data" 서브청크
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        // 데이터 크기
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = (totalAudioLen shr 8 and 0xffL).toByte()
        header[42] = (totalAudioLen shr 16 and 0xffL).toByte()
        header[43] = (totalAudioLen shr 24 and 0xffL).toByte()

        // 결과 WAV 파일 생성
        val wavData = ByteArray(header.size + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, header.size)
        System.arraycopy(pcmData, 0, wavData, header.size, pcmData.size)

        return wavData
    }

    /**
     * WAV 파일을 로컬 저장소에 저장하는 함수
     *
     * @param context 앱 컨텍스트
     * @param bytes WAV 헤더가 포함된 바이트 배열
     * @return 성공 시 파일 경로, 실패 시 null
     */
    suspend fun saveWavFile(
        context: Context,
        bytes: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 파일 이름 생성 (타임스탬프 사용)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "audio_$timestamp.wav"

            // 앱 외부 저장소 디렉토리 확인
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            if (directory == null) {
                log.error { "외부 저장소 디렉토리를 가져올 수 없습니다." }
                return@withContext null
            }

            // 파일 생성
            val file = File(directory, fileName)

            // 파일에 WAV 데이터 쓰기
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            log.info { "WAV 파일이 성공적으로 저장되었습니다: ${file.absolutePath}" }
            return@withContext file.absolutePath
        } catch (e: Exception) {
            log.error(e) { "WAV 파일 저장 중 오류 발생: ${e.message}" }
            return@withContext null
        }
    }
}