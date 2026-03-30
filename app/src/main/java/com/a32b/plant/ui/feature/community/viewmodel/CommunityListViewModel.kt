package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityListViewModel(private val repository: PostRepository) : ViewModel() {

    private val _navigateToDetail = MutableSharedFlow<String>()
    val navigateToDetail = _navigateToDetail.asSharedFlow()

    fun onPostClick(postId: String) {
        viewModelScope.launch {
            _navigateToDetail.emit(postId)
        }
    }


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()

    val uiState: StateFlow<List<Post>> = combine(
        repository.getPosts(),
        _searchQuery,
        _selectedTags
    ) { posts, query, tags ->
        posts.filter { post ->
            val matchesQuery = if (query.isBlank()) true 
                               else post.content.contains(query, ignoreCase = true) || post.title.contains(query, ignoreCase = true)
            
            val matchesTags = if (tags.isEmpty()) true 
                              else tags.contains(post.tag)
            
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

    fun onTagsChanged(tags: Set<String>) {
        _selectedTags.value = tags
    }
}