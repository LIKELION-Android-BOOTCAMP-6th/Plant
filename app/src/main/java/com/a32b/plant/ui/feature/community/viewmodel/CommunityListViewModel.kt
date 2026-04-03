package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.data.di.AppContainer.potRepository
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.Tag
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityListUiState(
    val tags: List<Tag> = emptyList(),
    val selected: List<Tag> = emptyList(),
    val isTagSheetShown: Boolean = false
)
class CommunityListViewModel(private val repository: PostRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CommunityListUiState())
    val uiState = _uiState.asStateFlow()
    private val _navigateToDetail = MutableSharedFlow<String>()
    val navigateToDetail = _navigateToDetail.asSharedFlow()

    fun onPostClick(postId: String) {
        viewModelScope.launch {
            _navigateToDetail.emit(postId)
        }
    }

    init {
        fetchTags()
    }
    fun onIsTagSheetShownChange() = _uiState.update { it.copy(isTagSheetShown = !_uiState.value.isTagSheetShown) }


//    private fun fetchTags(){
//        viewModelScope.launch {
//            potRepository.getAvailableTags().collectLatest { tags ->
//                getTags(tags)
//            }
//        }
//    }

    private fun fetchTags(){
        viewModelScope.launch(Dispatchers.IO) {
            getTags(repository.getTag())
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
        repository.getPostList(),
        _searchQuery,
        _uiState
    ) { posts, query, uiState ->
        posts.filter { post ->
            val matchesQuery = if (query.isBlank()) true
            else (post.content?.contains(query, ignoreCase = true) ?: false) || post.title.contains(query, ignoreCase = true)
            //필터 검색 - 하나라도 들어있을 시
            val matchesTags = if (uiState.selected.isEmpty()) true
                              else uiState.selected.any{it.name == post.tag.name}
            
            matchesQuery && matchesTags
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