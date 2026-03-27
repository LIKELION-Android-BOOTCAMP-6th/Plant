package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityListViewModel(private val repository: PostRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<List<Post>>(emptyList())
    val uiState: StateFlow<List<Post>> = _searchQuery
        .combine(repository.getPosts()) { query, posts ->
            if (query.isBlank()) {
                posts
            } else {
                posts.filter { it.content.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
