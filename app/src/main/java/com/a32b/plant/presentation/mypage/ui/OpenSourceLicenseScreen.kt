package com.a32b.plant.presentation.mypage.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.a32b.plant.R
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults

/**
 * 오픈소스 라이선스 고지 화면 (#637)
 *
 * - 목록: 빌드할 때 AboutLibraries 플러그인이 의존성에서 만든 res/raw/aboutlibraries.json을 읽는다 (앱에 포함된 리소스라 읽을 때 네트워크를 쓰지 않음)
 * - 의존성이 아닌 폰트(경기천년제목체)와 POM에 전문이 없는 라이선스는 app/aboutlibraries/ 폴더의 JSON으로 직접 추가한다
 * - 항목을 누르면 라이선스 전문이 다이얼로그로 열린다. 전문이 없는 항목(Google SDK 약관 등)은 라이선스 주소를 브라우저로 연다
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OpenSourceLicenseScreen(navController: NavController) {
    val libraries by produceLibraries(R.raw.aboutlibraries)

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text("오픈소스 라이선스", style = MaterialTheme.typography.displayLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // 라이브러리의 스타일 미지정 글자(전문 다이얼로그 본문·확인 버튼)는 LocalTextStyle을 따르는데,
        // 기본값인 bodyLarge는 앱에서 "스타일 미지정 걸러내기용" 기울임체라 기본 글씨(bodyMedium)로 바꾼다
        // ProvideTextStyle은 기존 스타일에 merge해서 기울임이 남으므로 LocalTextStyle 자체를 교체한다
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                colors = LibraryDefaults.libraryColors(
                    // 전문 다이얼로그 배경은 앱의 다른 다이얼로그(ConfirmDialog)와 같은 primaryContainer
                    dialogBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    // 전문 글자는 메인 폰트 색 (#643부터 기본값 onPrimaryContainer도 같은 값이지만 의도를 드러내려고 명시)
                    dialogContentColor = MaterialTheme.colorScheme.onSurface
                ),
                textStyles = LibraryDefaults.libraryTextStyles(
                    nameTextStyle = MaterialTheme.typography.titleMedium,
                    versionTextStyle = MaterialTheme.typography.bodySmall,
                    authorTextStyle = MaterialTheme.typography.bodySmall,
                    licensesTextStyle = MaterialTheme.typography.bodySmall
                ),
                licenseDialogConfirmText = "확인"
            )
        }
    }
}
