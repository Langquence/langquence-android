package kr.co.langquence.presentation.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 네비게이션 헤더 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavHeader(
	modifier: Modifier = Modifier,
	title: String? = null,
	showBackButton: Boolean = false,  // 뒤로가기 버튼 표시 여부
	showHamburgerMenu: Boolean = false,  // 햄버거 메뉴 버튼 표시 여부
	onBackClick: () -> Unit = {},
	onMenuClick: () -> Unit = {},
	additionalActions: @Composable RowScope.() -> Unit = {}
) {
	TopAppBar(
		title = {
			if (title != null) {
				Text(
					text = title,
					color = Color.White
				)
			}
		},
		navigationIcon = {
			if (showBackButton) {
				IconButton(onClick = onBackClick) {
					Icon(
						imageVector = Icons.Default.ArrowBack,
						contentDescription = "Back",
						tint = Color.White
					)
				}
			}
		},
		actions = {
			additionalActions()

			if (showHamburgerMenu) {
				IconButton(onClick = onMenuClick) {
					Icon(
						imageVector = Icons.Default.Menu,
						contentDescription = "Menu",
						tint = Color.White
					)
				}
			}
		},
		modifier = modifier
	)
}