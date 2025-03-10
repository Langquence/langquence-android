package kr.co.langquence.presentation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.co.langquence.presentation.component.NavHeader
import kr.co.langquence.presentation.ui.profile.ProfileConstants.PROFILE_IMAGE_SIZE

@Composable
fun ProfileScreen(
	onBackClick: () -> Unit
) {
	Scaffold(
		topBar = {
			NavHeader(
				title = "프로필",
				showBackButton = true,
				showHamburgerMenu = false,
				onBackClick = onBackClick
			)
		}
	) { paddingValues ->
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			ProfileImage()
		}
	}
}

/**
 * 프로필 이미지 컴포넌트
 */
@Composable
fun ProfileImage() {
	Box(
		modifier = Modifier
			.size(PROFILE_IMAGE_SIZE.dp)
			.clip(CircleShape)
			.background(Color.DarkGray)
			.border(2.dp, Color.White, CircleShape),
		contentAlignment = Alignment.Center
	) {
		Icon(
			imageVector = Icons.Default.Person,
			contentDescription = "Profile",
			tint = Color.White,
			modifier = Modifier.size(60.dp)
		)
	}
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
	ProfileScreen(onBackClick = {})
}