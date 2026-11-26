package com.a32b.plant.presentation.feedback.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.rememberPopBackStackSafely
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.presentation.community.ui.PostInputField
import com.a32b.plant.presentation.core.component.BackTopBar
import com.a32b.plant.presentation.core.component.LoadingDialog
import com.a32b.plant.presentation.core.component.TagGroup
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.feedback.viewmodel.ImprovementSuggestionEvent
import com.a32b.plant.presentation.feedback.viewmodel.ImprovementSuggestionViewModel

@Composable
fun ImprovementSuggestionScreen(
    navController: NavController,
    viewModel: ImprovementSuggestionViewModel = hiltViewModel()
) {
    val popBackStackSafely = rememberPopBackStackSafely(navController)
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAppVersion()
        viewModel.events.collect { event ->
            when (event) {
                is ImprovementSuggestionEvent.ShowToast -> context.showToast(event.message)
                ImprovementSuggestionEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    if (uiState.isLoading) {
        LoadingDialog()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopBar(
                title = "개선사항 제안",
                onBack = { popBackStackSafely() },
                backEnabled = !uiState.isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("카테고리", style = MaterialTheme.typography.titleSmall)
            TagGroup(
                tags = ImprovementCategory.entries.map { it.label },
                init = listOfNotNull(uiState.category?.label),
                isMultiSelected = false,
                onSelectedChanged = { selected ->
                    viewModel.onCategoryChanged(
                        ImprovementCategory.entries.firstOrNull { it.label == selected.firstOrNull() }
                    )
                }
            )

            Text("제안 내용", style = MaterialTheme.typography.titleSmall)
            PostInputField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                placeholder = "개선되었으면 하는 점이나 필요한 기능을 알려주세요",
                modifier = Modifier.heightIn(min = 160.dp),
                maxLength = DESCRIPTION_MAX_LENGTH
            )

            Button(
                onClick = viewModel::submit,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("제출", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 보안 규칙(firestore.rules)의 maxFeedbackTextLength()와 같은 값. 바꾸면 규칙도 함께 바꾼다 (#634)
private const val DESCRIPTION_MAX_LENGTH = 1000
