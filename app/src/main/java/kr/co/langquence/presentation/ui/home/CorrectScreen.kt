package kr.co.langquence.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.langquence.presentation.component.NavHeader

@Composable
fun CorrectScreen(
	text: String,
	onBackClick: () -> Unit
) {
	Scaffold(
		topBar = {
			NavHeader(
				title = "Langquence",
				showBackButton = true,
				onBackClick = onBackClick
			)
		}
	) { paddingValues ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.DarkGray)
				.padding(paddingValues),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = "인식된 텍스트",
					color = Color.White,
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold
				)

				Spacer(modifier = Modifier.height(16.dp))

				Surface(
					modifier = Modifier.fillMaxWidth(),
					color = Color(0xFF333333),
					shape = MaterialTheme.shapes.medium
				) {
					Text(
						text = text,
						color = Color.White,
						fontSize = 18.sp,
						modifier = Modifier.padding(16.dp),
						textAlign = TextAlign.Center
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun CorrectScreenPreview() {
	CorrectScreen(
		text = "Hello, World!",
		onBackClick = {}
	)
}