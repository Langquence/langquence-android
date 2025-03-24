package kr.co.langquence.data.dto.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kr.co.langquence.model.domain.CorrectAnswer

@Parcelize
data class CorrectResponse(
	@SerializedName("text")
	val text: String
) : Parcelable {
	fun toEntity(): CorrectAnswer = CorrectAnswer(text)

	override fun toString(): String {
		return "CorrectResponse(text='$text')"
	}
}
