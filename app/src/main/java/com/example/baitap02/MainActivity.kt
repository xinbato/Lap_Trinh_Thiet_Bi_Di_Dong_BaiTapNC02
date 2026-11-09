package com.example.baitap02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.baitap02.ui.CoinGeckoScreen
import com.example.baitap02.ui.CryptoViewModel
import com.example.baitap02.ui.theme.BaiTap02Theme

class MainActivity : ComponentActivity() {
    private val viewModel: CryptoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaiTap02Theme {
                CoinGeckoScreen(viewModel = viewModel)
            }
        }
    }
}