package com.example.baitap02.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baitap02.data.CoinItem
import com.example.baitap02.data.GlobalData
import com.example.baitap02.network.CryptoApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface MarketUiState {
    data object Loading : MarketUiState
    data class Success(
        val coins: List<CoinItem>,
        val global: GlobalData? = null,
        val isRefreshing: Boolean = false
    ) : MarketUiState
    data class Error(val message: String) : MarketUiState
}

class CryptoViewModel(
    private val api: CryptoApi = CryptoApi.instance
) : ViewModel() {

    companion object {

        const val AUTO_REFRESH_INTERVAL_MS = 10_000L
        const val MANUAL_REFRESH_COOLDOWN_MS = 15_000L
    }

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val _manualCooldownSeconds = MutableStateFlow(0)
    val manualCooldownSeconds: StateFlow<Int> = _manualCooldownSeconds.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            // Lần đầu khởi chạy: hiện loading toàn màn hình nếu chưa có dữ liệu
            fetchData(showFullLoading = _uiState.value !is MarketUiState.Success)
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                fetchData(showFullLoading = false)
            }
        }
    }

    fun manualRefresh() {
        if (_manualCooldownSeconds.value > 0) return

        viewModelScope.launch {
            fetchData(showFullLoading = false)
            // Đếm ngược thời gian chờ (cooldown) để tránh spam và lag máy
            for (sec in (MANUAL_REFRESH_COOLDOWN_MS / 1000).toInt() downTo 1) {
                _manualCooldownSeconds.value = sec
                delay(1000L)
            }
            _manualCooldownSeconds.value = 0
        }
    }

    private suspend fun fetchData(showFullLoading: Boolean) {
        if (showFullLoading) {
            _uiState.value = MarketUiState.Loading
        } else {
            val currentState = _uiState.value
            if (currentState is MarketUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }
        }

        try {
            val (coins, global) = coroutineScope {
                val coinsTask = async { api.getMarkets() }
                val globalTask = async {
                    try { api.getGlobal().data } catch (_: Exception) { null }
                }
                coinsTask.await() to globalTask.await()
            }
            val existingGlobal = (_uiState.value as? MarketUiState.Success)?.global
            _uiState.value = MarketUiState.Success(
                coins = coins,
                global = global ?: existingGlobal,
                isRefreshing = false
            )
        } catch (e: IOException) {
            // Nếu đã có dữ liệu trước đó, giữ nguyên UI và tắt refreshing
            val current = _uiState.value
            if (current is MarketUiState.Success) {
                _uiState.value = current.copy(isRefreshing = false)
            } else {
                _uiState.value = MarketUiState.Error("Network error: ${e.localizedMessage ?: "No connection"}")
            }
        } catch (e: HttpException) {
            val current = _uiState.value
            if (current is MarketUiState.Success) {
                _uiState.value = current.copy(isRefreshing = false)
            } else {
                _uiState.value = MarketUiState.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            val current = _uiState.value
            if (current is MarketUiState.Success) {
                _uiState.value = current.copy(isRefreshing = false)
            } else {
                _uiState.value = MarketUiState.Error("Unexpected error: ${e.localizedMessage ?: "Unknown"}")
            }
        }
    }
}
