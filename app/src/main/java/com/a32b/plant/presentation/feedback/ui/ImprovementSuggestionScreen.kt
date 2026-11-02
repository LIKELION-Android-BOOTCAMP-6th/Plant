package com.a32b.plant.presentation.feedback.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.presentation.community.ui.PostInputField
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.TagGroup
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.feedback.viewmodel.ImprovementSuggestionEvent
import com.a32b.plant.presentation.feedback.viewmodel.ImprovementSuggestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovementSuggestionScreen(
    navController: NavController,
    viewModel: ImprovementSuggestionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

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
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            LoadingBox(modifier = Modifier.size(80.dp))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    Text(
                        text = "개선사항 제안",
                        style = MaterialTheme.typography.displayLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
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

            Text("설명", style = MaterialTheme.typography.titleSmall)
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
                Text("제출", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private const val DESCRIPTION_MAX_LENGTH = 1000
