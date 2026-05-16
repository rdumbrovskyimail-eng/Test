package com.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.data.repository.User
import com.test.data.repository.UserRepository
import com.test.data.repository.UserRole
import com.test.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedRole: UserRole? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val totalCount: Int = 0,
    val selectedUser: User? = null,
    val isRefreshing: Boolean = false
)

sealed class MainUiEvent {
    data class Search(val query: String) : MainUiEvent()
    data class SelectRole(val role: UserRole?) : MainUiEvent()
    data class LoadMore(val page: Int) : MainUiEvent()
    data class SelectUser(val user: User) : MainUiEvent()
    object Refresh : MainUiEvent()
    object ClearError : MainUiEvent()
    object ClearSelection : MainUiEvent()
    data class DeleteUser(val id: String) : MainUiEvent()
}

sealed class MainSideEffect {
    data class ShowToast(val message: String) : MainSideEffect()
    data class Navigate(val route: String) : MainSideEffect()
    object ScrollToTop : MainSideEffect()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<MainSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<MainSideEffect> = _sideEffects.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadUsers()
        observeSearch()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        searchQueryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    if (query.isBlank()) {
                        emit(NetworkResult.Loading)
                        emit(repository.getUsers(1, 20))
                    } else {
                        emit(NetworkResult.Loading)
                        emit(repository.searchUsers(query))
                    }
                }
            }
            .onEach { result -> handleUserListResult(result) }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.Search -> onSearch(event.query)
            is MainUiEvent.SelectRole -> onRoleFilter(event.role)
            is MainUiEvent.LoadMore -> loadMore(event.page)
            is MainUiEvent.SelectUser -> onSelectUser(event.user)
            MainUiEvent.Refresh -> refresh()
            MainUiEvent.ClearError -> clearError()
            MainUiEvent.ClearSelection -> clearSelection()
            is MainUiEvent.DeleteUser -> deleteUser(event.id)
        }
    }

    private fun loadUsers(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getUsers(page, 20)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        users = if (page == 1) result.value else it.users + result.value,
                        isLoading = false,
                        currentPage = page,
                        hasMore = result.value.size == 20
                    )
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    private fun onRoleFilter(role: UserRole?) {
        _uiState.update { it.copy(selectedRole = role, isLoading = true) }
        viewModelScope.launch {
            val result = if (role == null) repository.getUsers(1, 20)
            else repository.getUsersByRole(role)
            handleUserListResult(result)
        }
    }

    private fun loadMore(page: Int) {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        loadUsers(page)
    }

    private fun onSelectUser(user: User) {
        _uiState.update { it.copy(selectedUser = user) }
        viewModelScope.launch {
            _sideEffects.send(MainSideEffect.Navigate("user/${user.id}"))
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val result = repository.getUsers(1, 20)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(users = result.value, isRefreshing = false, currentPage = 1)
                    }
                    _sideEffects.send(MainSideEffect.ScrollToTop)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                    _sideEffects.send(MainSideEffect.ShowToast("Refresh failed: ${result.message}"))
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun deleteUser(id: String) {
        viewModelScope.launch {
            when (val result = repository.deleteUser(id)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(users = it.users.filter { u -> u.id != id }) }
                    _sideEffects.send(MainSideEffect.ShowToast("User deleted"))
                }
                is NetworkResult.Error -> {
                    _sideEffects.send(MainSideEffect.ShowToast("Delete failed: ${result.message}"))
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun clearError() = _uiState.update { it.copy(error = null) }
    private fun clearSelection() = _uiState.update { it.copy(selectedUser = null) }

    private fun handleUserListResult(result: NetworkResult<List<User>>) {
        when (result) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(users = result.value, isLoading = false, error = null)
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(isLoading = false, error = result.message)
            }
            NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
        }
    }

    val filteredUsers: StateFlow<List<User>> = _uiState
        .map { state ->
            state.users.let { list ->
                if (state.selectedRole != null) list.filter { it.role == state.selectedRole } else list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userCount: StateFlow<Int> = filteredUsers
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}