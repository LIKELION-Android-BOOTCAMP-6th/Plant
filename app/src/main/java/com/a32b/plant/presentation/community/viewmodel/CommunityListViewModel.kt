package com.a32b.plant.presentation.community.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.GetCommunityTagsUseCase
import com.a32b.plant.domain.usecase.community.ObservePostListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityListUiState(
    val tags: List<Tag> = emptyList(),
    val selected: List<Tag> = emptyList(),
    val isTagSheetShown: Boolean = false,
    val isSharedShown: Boolean = false
)
@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val observePostListUseCase: ObservePostListUseCase,
    private val getCommunityTagsUseCase: GetCommunityTagsUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CommunityListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchTags()
    }
    fun onIsTagSheetShownChange() = _uiState.update { it.copy(isTagSheetShown = !_uiState.value.isTagSheetShown) }

    fun onSharedShownChange() = _uiState.update { it.copy(isSharedShown = !_uiState.value.isSharedShown) }
    private fun fetchTags(){
        viewModelScope.launch(Dispatchers.IO) {
            getCommunityTagsUseCase()
                .onSuccess { tags -> getTags(tags) }
                .onFailure { e -> Log.e("CommunityListVM", "태그 목록 조회 실패: ${e.message}") }
            // 빈화면 -> 홈화면
            loaded()
        }
    }
    fun getTags(list: List<Tag>) = _uiState.update { it.copy(tags = list) }


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags = _selectedTags.asStateFlow()

    val searchUiState: StateFlow<List<Post>> = combine(
        observePostListUseCase(),
        _searchQuery,
        _uiState
    ) { posts, query, uiState ->
        posts.filter { post ->
            val matchesQuery = if (query.isBlank()) true
            else (post.content?.contains(query, ignoreCase = true) ?: false) || post.title.contains(query, ignoreCase = true)
            //필터 검색 - 하나라도 들어있을 시
            val matchesTags = if (uiState.selected.isEmpty()) true
                              else uiState.selected.any{it.name == post.tag.name}

            val matchesShared = if(!uiState.isSharedShown) true
                                else post.isShared == true

            matchesQuery && matchesTags && matchesShared
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSelectedChanged(tags: List<Tag>) {
        _uiState.update { it.copy(selected = tags) }
    }
}
