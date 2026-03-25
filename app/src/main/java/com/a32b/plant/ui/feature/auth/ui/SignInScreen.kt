package com.a32b.plant.ui.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement       // 자식 요소 배치 방식 (가운데, 위, 아래 등)
import androidx.compose.foundation.layout.Column            // 세로로 요소를 쌓는 레이아웃 컴포넌트
import androidx.compose.foundation.layout.fillMaxSize       // 부모 크기를 꽉 채우는 Modifier
import androidx.compose.material3.Button                    // 버튼 컴포넌트
import androidx.compose.material3.Text                      // 텍스트 컴포넌트
import androidx.compose.runtime.Composable                  // @Composable 어노테이션을 쓰기 위해 필요
import androidx.compose.ui.Alignment                        // 정렬 방향 설정 (가로 중앙, 우측 등)
import androidx.compose.ui.Modifier                         // UI 요소의 크기, 여백 등을 설정하는 수식어
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController                    // 화면 이동을 담당하는 네비게이션 컨트롤러
import com.a32b.plant.core.navigation.Routes                // 앱 내 화면 경로(라우트) 정의 모음
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.auth.viewmodel.SignInViewModel


// @Composable: 이 함수가 UI를 그리는 Compose 함수임을 선언하는 어노테이션
// Compose에서는 UI를 함수로 표현해 — @Composable이 붙은 함수만 화면을 그릴 수 있음
@Composable
// SignInScreen: 로그인 화면을 그리는 함수
// navController: 다른 화면으로 이동할 때 사용하는 네비게이션 객체를 외부에서 받아옴
//************ 임시 로그인 화면 ************
//***************************************
fun SignInScreen(navController: NavController) {

    val viewModel : SignInViewModel = viewModel(factory = ViewModelFactory.signInViewModelFactory)

    // Column: 자식 요소들을 세로(위→아래)로 쌓는 레이아웃
    Column(

        // Modifier.fillMaxSize(): 이 Column이 화면 전체 크기를 꽉 채우도록 설정
        modifier = Modifier.fillMaxSize(),

        // verticalArrangement: 세로 방향으로 자식 요소들을 어떻게 배치할지 설정
        // Arrangement.Center: 세로 방향 가운데 정렬
        verticalArrangement = Arrangement.Center,

        // horizontalAlignment: 가로 방향으로 자식 요소들을 어떻게 정렬할지 설정
        // Alignment.CenterHorizontally: 가로 방향 가운데 정렬
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Text: "로그인 화면 (임시)" 라는 글자를 화면에 표시
        // 현재는 임시 화면이라 간단하게 텍스트만 표시
        Text("로그인 화면 (임시)")

        // Button: 클릭 가능한 버튼 컴포넌트
        // onClick: 버튼 클릭 시 실행할 동작을 람다(중괄호 블록)로 전달
        Button(onClick = {
            // navController.navigate(): 다른 화면으로 이동하는 함수
            // Routes.SignUp: Routes.kt에 정의된 회원가입 화면 경로
            navController.navigate(Routes.SignUp)
        }) {
            // 버튼 안에 표시될 텍스트
            Text("회원가입 하러가기")
        }
    }
}