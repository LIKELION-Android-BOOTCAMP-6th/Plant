package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.ui.feature.community.ui.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityListViewModel(private val repository: PostRepository) : ViewModel() {

    // 1️⃣ 서버에서 가져온 전체 게시글 원본 주머니
    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    // 2️⃣ 사용자가 입력한 검색어 주머니
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 3️⃣ ⭐ 핵심: 검색어에 따라 필터링된 "보여줄 리스트"
    // combine은 [전체글]과 [검색어]가 바뀔 때마다 요리를 새로 해서 uiState에 담아줍니다.
    val uiState: StateFlow<List<Post>> = combine(_allPosts, _searchQuery) { posts, query ->
        if (query.isBlank()) {
            posts // 검색어가 비었으면 전체 다 보여줌
        } else {
            // 검색어가 포함된 게시글만 골라내는 필터
            posts.filter { it.content.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            repository.getPosts().collectLatest { newList ->
                _allPosts.value = newList
            }
        }
    }

    // 4️⃣ 검색창에 글자가 바뀔 때 화면(UI)에서 호출할 함수
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }
}