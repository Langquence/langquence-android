package kr.co.langquence.presentation.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kr.co.langquence.presentation.component.NavHeader
import kr.co.langquence.presentation.ui.home.HomeConstants.MIC_BUTTON_SIZE
import kr.co.langquence.presentation.viewmodel.home.VoiceRecognitionState
import kr.co.langquence.presentation.viewmodel.home.VoiceRecordViewModel

private val log = KotlinLogging.logger {}

@Composable
fun HomeScreen(
	viewModel: VoiceRecordViewModel = hiltViewModel(),
	onNavigateToProfile: () -> Unit,
	onNavigateToResult: () -> Unit
) {
	val voiceState by viewModel.voiceState.collectAsState()
	val permissionRequest by viewModel.permissionRequest.collectAsState()
	val timerValue by viewModel.timerValue.collectAsState()
	val correctState by viewModel.correctState.collectAsState()

	val requestPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission(),
		onResult = { isGranted ->
			if (isGranted) {
				viewModel.resetPermissionRequest()
				viewModel.toggleListeningMode()
			}
		}
	)

	// 음성 인식 성공 시 결과 화면으로 이동
	LaunchedEffect(voiceState) {
		if (voiceState is VoiceRecognitionState.Success) {
			onNavigateToResult()
		}
	}

	// 음성 권한 요청 시, 권환 획득 컴포즈 실행
	LaunchedEffect(permissionRequest) {
		if (permissionRequest) {
			requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
		}
	}

	LaunchedEffect(correctState) {
		correctState.data?.let { data ->
			log.info { "Correct answer received data: \n ${data.text}" }
		}

		correctState.error?.let { error ->
			log.error { "Correct answer received error: \n ${error.message}" }
		}
	}

	Scaffold(
		topBar = {
			NavHeader(
				title = "Langquence",
				additionalActions = {
					IconButton(onClick = onNavigateToProfile) {
						Icon(
							imageVector = Icons.Default.Menu,
							contentDescription = "프로필"
						)
					}
				}
			)
		}
	) { paddingValues ->
		HomeContent(
			modifier = Modifier.padding(paddingValues),
			isListening = voiceState is VoiceRecognitionState.Listening,
			timerValue = timerValue,
			onVoiceButtonClick = {
				log.info { "Voice button clicked, toggling listening mode" }

				viewModel.toggleListeningMode()
			}
		)
	}
}


@Composable
private fun HomeContent(
	modifier: Modifier = Modifier,
	isListening: Boolean,
	timerValue: Int,
	onVoiceButtonClick: () -> Unit
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(Color.DarkGray)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Box(
				modifier = Modifier
					.size(HomeConstants.MAIN_CONTAINER_SIZE.dp)
					.background(Color.DarkGray)
			) {
				Column(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					if (isListening) {
						Text(
							text = formatTime(timerValue),
							color = Color.White,
							fontSize = 24.sp,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.padding(bottom = 16.dp)
						)
					}

					VoiceButton(
						isListening = isListening,
						onToggle = onVoiceButtonClick
					)
				}
			}
		}
	}
}

@Composable
fun VoiceButton(
	isListening: Boolean,
	onToggle: () -> Unit
) {
	val buttonColor = if (isListening) HomeConstants.BORDER_COLOR_LISTENING else HomeConstants.BORDER_COLOR_NORMAL
	val iconVector = if (isListening) Icons.Default.KeyboardArrowUp else Icons.Default.Face
	val contentDescription = if (isListening) "Stop Listening" else "Start Listening"

	Box(
		modifier = Modifier
			.size(MIC_BUTTON_SIZE.dp)
			.clip(CircleShape)
			.border(
				width = 2.dp,
				color = buttonColor,
				shape = CircleShape
			)
			.background(Color.DarkGray)
			.clickable(onClick = onToggle)
			.padding(16.dp),
		contentAlignment = Alignment.Center
	) {
		Icon(
			imageVector = iconVector,
			contentDescription = contentDescription,
			tint = Color.White,
			modifier = Modifier.size(40.dp)
		)
	}
}

private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
	HomeScreen(onNavigateToProfile = {}, onNavigateToResult = {})
}