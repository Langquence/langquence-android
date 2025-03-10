package kr.co.langquence.presentation.ui.home

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kr.co.langquence.presentation.component.NavHeader
import kr.co.langquence.presentation.ui.home.HomeConstants.BORDER_COLOR_LISTENING
import kr.co.langquence.presentation.ui.home.HomeConstants.BORDER_COLOR_NORMAL
import kr.co.langquence.presentation.ui.home.HomeConstants.MAIN_CONTAINER_SIZE
import kr.co.langquence.presentation.ui.home.HomeConstants.MIC_BUTTON_SIZE
import kr.co.langquence.presentation.viewmodel.home.HomeViewModel


@Composable
fun HomeScreen(
	viewModel: HomeViewModel = hiltViewModel(),
	onNavigateToProfile: () -> Unit
) {
	val isListeningMode by viewModel.isListeningMode.collectAsState()

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
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						VoiceButton(
							isListening = isListeningMode,
							onToggle = { viewModel.toggleListeningMode() }
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