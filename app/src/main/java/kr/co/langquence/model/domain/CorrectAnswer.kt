package kr.co.langquence.model.domain

import com.google.gson.annotations.SerializedName

data class CorrectAnswer(
	@SerializedName("text")
	val text: String
)