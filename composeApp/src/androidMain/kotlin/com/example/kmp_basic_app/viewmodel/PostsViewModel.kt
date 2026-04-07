package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.usecase.CreatePostUseCase
import com.example.kmp_basic_app.domain.usecase.DeletePostUseCase
import com.example.kmp_basic_app.domain.usecase.GetPostsUseCase
import com.example.kmp_basic_app.domain.usecase.UpdatePostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostsUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class PostsViewModel(
    private val getPostsUseCase: GetPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getPostsUseCase()
                .onSuccess { postPage ->
                    _uiState.update { it.copy(posts = postPage.posts, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun createPost(title: String, body: String) {
        viewModelScope.launch {
            createPostUseCase(title, body)
                .onSuccess { post ->
                    _uiState.update {
                        it.copy(
                            posts = listOf(post) + it.posts,
                            snackbarMessage = "Post created"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(snackbarMessage = "Failed to create post: ${e.message}") }
                }
        }
    }

    fun updatePost(id: String, title: String, body: String) {
        viewModelScope.launch {
            updatePostUseCase(id, title, body)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { if (it.id == id) updated else it },
                            snackbarMessage = "Post updated"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(snackbarMessage = "Failed to update post: ${e.message}") }
                }
        }
    }

    fun deletePost(id: String) {
        viewModelScope.launch {
            deletePostUseCase(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filter { it.id != id },
                            snackbarMessage = "Post deleted"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(snackbarMessage = "Failed to delete post: ${e.message}") }
                }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
