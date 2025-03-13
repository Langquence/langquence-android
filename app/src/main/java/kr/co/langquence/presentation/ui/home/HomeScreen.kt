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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.github.oshai.kotlinlogging.KotlinLogging
import kr.co.langquence.presentation.component.NavHeader
import kr.co.langquence.presentation.navigation.Routes
import kr.co.langquence.presentation.ui.home.HomeConstants.BORDER_COLOR_LISTENING
import kr.co.langquence.presentation.ui.home.HomeConstants.BORDER_COLOR_NORMAL
import kr.co.langquence.presentation.ui.home.HomeConstants.MAIN_CONTAINER_SIZE
import kr.co.langquence.presentation.ui.home.HomeConstants.MIC_BUTTON_SIZE
import kr.co.langquence.presentation.viewmodel.home.VoiceRecognitionState
import kr.co.langquence.presentation.viewmodel.home.VoiceViewModel

val log = KotlinLogging.logger {}

@Composable
fun HomeScreen(
	viewModel: VoiceViewModel = hiltViewModel(),
	onNavigateToProfile: () -> Unit,
	navController: NavController = rememberNavController()
) {
	val voiceState by viewModel.voiceState.collectAsState()
	val hasPermission by viewModel.hasAudioPermission.collectAsState()

	// 권한 요청 런처
	val requestPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission(),
		onResult = { isGranted ->
			viewModel.updatePermissionStatus(isGranted)
			if (isGranted) {
				viewModel.toggleListeningMode()
			}
		}
	)

	// 권한 확인 및 요청
	fun checkAndRequestPermission() {
		if (!hasPermission) {
			log.info { "음성 인식을 위한 권한 인증 요청" }

			requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
			requestPermissionLauncher.launch(Manifest.permission.INTERNET)
		} else {
			viewModel.toggleListeningMode()
		}
	}

	// 음성 인식 성공 시 결과 화면으로 이동
	LaunchedEffect(voiceState) {
		if (voiceState is VoiceRecognitionState.Success) {
			val text = (voiceState as VoiceRecognitionState.Success).text
			 navController.navigate(Routes.voiceResultRoute(text))
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
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.DarkGray)
				.padding(paddingValues)
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
						.size(MAIN_CONTAINER_SIZE.dp)
						.background(Color.DarkGray)
				) {
					Column(
						modifier = Modifier.fillMaxSize(),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center
					) {
						VoiceButton(
							isListening = voiceState is VoiceRecognitionState.Listening,
							onToggle = { checkAndRequestPermission() }
						)
					}
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
	Box(
		modifier = Modifier
			.size(MIC_BUTTON_SIZE.dp)
			.clip(CircleShape)
			.border(
				width = 2.dp,
				color = if (isListening) BORDER_COLOR_LISTENING else BORDER_COLOR_NORMAL,
				shape = CircleShape
			)
			.background(Color.DarkGray)
			.clickable(onClick = onToggle)
			.padding(16.dp),
		contentAlignment = Alignment.Center
	) {
		Icon(
			imageVector = if (isListening) Icons.Default.KeyboardArrowUp else Icons.Default.Face,
			contentDescription = if (isListening) "Stop Listening" else "Start Listening",
			tint = Color.White,
			modifier = Modifier.size(40.dp)
		)
	}
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
	HomeScreen(onNavigateToProfile = {})
}