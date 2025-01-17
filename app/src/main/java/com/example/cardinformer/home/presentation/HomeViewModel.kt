package com.example.cardinformer.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardinformer.core.domain.model.CardInf
import com.example.cardinformer.core.utils.NetworkError
import com.example.cardinformer.core.utils.debounce
import com.example.cardinformer.home.domain.usecase.GetCardInfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCardInfUseCase: GetCardInfUseCase
) : ViewModel() {

    private var lastExpression = ""

    private val _uiState = MutableStateFlow<HomeScreenUiState>(HomeScreenUiState.Empty)
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    private val searchDebounceAction: (String) -> Unit = debounce(
        delayMillis = 2_000L,
        coroutineScope = viewModelScope,
        useLastParam = true
    ) { changedText ->
        if (changedText != lastExpression && changedText.isNotBlank()) {
            getCardInformation(changedText)
            _uiState.value = HomeScreenUiState.Loading
        } else {
            _uiState.value = HomeScreenUiState.Empty
        }
    }

    fun searchDebounce(expression: String) {
        if (expression.isBlank()) clearSearch()
        searchDebounceAction(expression)
    }

    private fun clearSearch() {
        lastExpression = ""
        _uiState.value = HomeScreenUiState.Empty
    }

    private fun getCardInformation(bin: String) = viewModelScope.launch(Dispatchers.Main) {
        val result: Result<CardInf> = getCardInfUseCase(bin)
        val newState = when (result.exceptionOrNull()) {
            is NetworkError.ServerError -> HomeScreenUiState.Error
            is NetworkError.NoData -> HomeScreenUiState.Empty
            is NetworkError.NoInternet -> HomeScreenUiState.NoInternet
            else -> result.getOrNull()?.let {
                HomeScreenUiState.Content(it)
            } ?: HomeScreenUiState.Empty
        }
        _uiState.value = newState

    }
}