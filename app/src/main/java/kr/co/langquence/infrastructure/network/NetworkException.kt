package kr.co.langquence.infrastructure.network

import java.io.IOException

class NetworkException(val code: Int, override val message: String?): IOException(message) {
	override fun toString(): String {
		return "NetworkException(code=$code, message=$message)"
	}
}