package com.example.baitap02.ui

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.baitap02.R
import com.example.baitap02.data.CoinItem
import com.example.baitap02.data.FearAndGreed
import com.example.baitap02.data.GlobalData
import com.example.baitap02.data.Sparkline
import com.example.baitap02.ui.theme.BaiTap02Theme
import java.util.Locale

private val PositiveGreen = Color(0xFF16C784)
private val NegativeRed = Color(0xFFEA3943)

// Định dạng giá chuẩn xác như website CoinGecko:
// Nếu giá < $1.05 (như Tether, USDC...) -> hiển thị 4 chữ số thập phân ($0.9992).
// Nếu giá lớn (Bitcoin, Ethereum...) -> hiển thị 2 chữ số ($75,304.35).
fun formatCryptoPrice(price: Double): String {
    return when {
        price >= 1.05 -> String.format(Locale.US, "$%,.2f", price)
        price >= 0.0001 -> String.format(Locale.US, "$%,.4f", price)
        else -> String.format(Locale.US, "$%,.6f", price)
    }
}

// Định dạng số tiền lớn thành Trillion (T), Billion (B), Million (M)
fun formatTrillionBillion(value: Double?): String {
    if (value == null) return "-"
    return when {
        value >= 1_000_000_000_000.0 -> String.format(Locale.US, "$%,.2fT", value / 1_000_000_000_000.0)
        value >= 1_000_000_000.0 -> String.format(Locale.US, "$%,.2fB", value / 1_000_000_000.0)
        value >= 1_000_000.0 -> String.format(Locale.US, "$%,.2fM", value / 1_000_000.0)
        else -> String.format(Locale.US, "$%,.0f", value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinGeckoScreen(
    viewModel: CryptoViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cooldown by viewModel.manualCooldownSeconds.collectAsStateWithLifecycle()

    CoinGeckoScreenContent(
        state = state,
        cooldownSeconds = cooldown,
        onRefresh = viewModel::manualRefresh,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinGeckoScreenContent(
    state: MarketUiState,
    cooldownSeconds: Int = 0,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRefreshing = (state as? MarketUiState.Success)?.isRefreshing == true
    val isCooldownActive = cooldownSeconds > 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.crypto_market_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    val rotation = if (isRefreshing) {
                        val transition = rememberInfiniteTransition(label = "refresh_rotation")
                        val angle by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                            label = "angle"
                        )
                        angle
                    } else 0f

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isCooldownActive && !isRefreshing
                    ) {
                        if (isCooldownActive) {
                            Text(
                                text = "${cooldownSeconds}s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text(
                                text = "↻",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.rotate(rotation),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is MarketUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MarketUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRefresh) {
                            Text(text = stringResource(R.string.btn_retry))
                        }
                    }
                }
                is MarketUiState.Success -> {
                    val horizontalScroll = rememberScrollState()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // 1. Áp dụng LazyRow: Thẻ thông số thị trường toàn cầu từ API GET /global
                        item {
                            state.global?.let { globalData ->
                                GlobalMarketRow(global = globalData)
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // 2. Header bảng cuộn ngang
                        item {
                            TableHeader(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScroll)
                            )
                            HorizontalDivider()
                        }

                        // 3. Toàn bộ danh sách coin (LazyColumn tái sử dụng từng dòng mượt mà)
                        items(state.coins, key = { it.id }) { coin ->
                            TableRow(
                                coin = coin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScroll)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Danh sách các thẻ dữ liệu toàn cầu (GET /global) cuộn ngang bằng LazyRow
@Composable
fun GlobalMarketRow(
    global: GlobalData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "🌍 Global Market Overview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        val marketCapUsd = global.totalMarketCap?.get("usd")
        val volumeUsd = global.totalVolume?.get("usd")
        val btcDom = global.dominancePercentage?.get("btc")
        val ethDom = global.dominancePercentage?.get("eth")
        val capChange = global.marketCapChange24h
        val sentiment = global.fearAndGreed

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "card_mcap") {
                GlobalMarketCard(
                    title = "Total Market Cap",
                    value = formatTrillionBillion(marketCapUsd),
                    badge = capChange?.let { String.format(Locale.US, "%s%.2f%%", if (it >= 0) "▲ " else "▼ ", it) },
                    isPositive = (capChange ?: 0.0) >= 0.0
                )
            }
            item(key = "card_vol") {
                GlobalMarketCard(
                    title = "24h Volume",
                    value = formatTrillionBillion(volumeUsd),
                    subtitle = "Global trading volume"
                )
            }
            item(key = "card_btc_dom") {
                GlobalMarketCard(
                    title = "BTC Dominance",
                    value = btcDom?.let { String.format(Locale.US, "%.1f%%", it) } ?: "-",
                    subtitle = "Bitcoin market share"
                )
            }
            item(key = "card_eth_dom") {
                GlobalMarketCard(
                    title = "ETH Dominance",
                    value = ethDom?.let { String.format(Locale.US, "%.1f%%", it) } ?: "-",
                    subtitle = "Ethereum market share"
                )
            }
            if (sentiment != null && sentiment.sentiment.isNotBlank()) {
                item(key = "card_sentiment") {
                    val isFear = sentiment.value < 50
                    GlobalMarketCard(
                        title = "Fear & Greed Index",
                        value = "${sentiment.value} - ${sentiment.sentiment.replaceFirstChar { it.uppercase() }}",
                        badge = if (isFear) "Fear" else "Greed",
                        isPositive = !isFear
                    )
                }
            }
            // Card 6: Số lượng đồng coin đang lưu hành
            item(key = "card_active_coins") {
                GlobalMarketCard(
                    title = "Active Cryptos",
                    value = String.format(Locale.US, "%,d", global.activeCryptos),
                    subtitle = "In ${String.format(Locale.US, "%,d", global.marketsCount)} markets"
                )
            }
        }
    }
}

@Composable
fun GlobalMarketCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    isPositive: Boolean = true,
    subtitle: String? = null
) {
    Card(
        modifier = modifier
            .width(165.dp)
            .height(95.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (badge != null) {
                Text(
                    text = badge,
                    color = if (isPositive) PositiveGreen else NegativeRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.column_rank), modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_coin), modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_price), modifier = Modifier.width(105.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_1h), modifier = Modifier.width(75.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_24h), modifier = Modifier.width(75.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_7d), modifier = Modifier.width(75.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_volume), modifier = Modifier.width(140.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_market_cap), modifier = Modifier.width(150.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.column_last_7d), modifier = Modifier.width(130.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TableRow(
    coin: CoinItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rank
        Text(
            text = "${coin.rank}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 3. Coin info (Logo, Name, Symbol)
        Row(
            modifier = Modifier.width(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coin.image,
                contentDescription = stringResource(R.string.cd_logo, coin.name),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = coin.symbol.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 4. Price (Định dạng chính xác: Tether/USDC có 4 số thập phân)
        Text(
            text = formatCryptoPrice(coin.currentPrice),
            modifier = Modifier.width(105.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 5. 1h change
        PercentageText(value = coin.change1h, modifier = Modifier.width(75.dp))

        // 6. 24h change
        PercentageText(value = coin.change24h, modifier = Modifier.width(75.dp))

        // 7. 7d change
        PercentageText(value = coin.change7d, modifier = Modifier.width(75.dp))

        // 8. 24h Volume
        Text(
            text = String.format(Locale.US, "$%,.0f", coin.totalVolume),
            modifier = Modifier.width(140.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 9. Market Cap
        Text(
            text = String.format(Locale.US, "$%,.0f", coin.marketCap),
            modifier = Modifier.width(150.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 10. Sparkline 7 Days Canvas
        val sparklinePrices = coin.sparkline?.price ?: emptyList()
        val is7dUp = (coin.change7d ?: 0.0) >= 0.0
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(38.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            SparklineChart(
                prices = sparklinePrices,
                isUp = is7dUp,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PercentageText(
    value: Double?,
    modifier: Modifier = Modifier
) {
    val isPositive = (value ?: 0.0) >= 0.0
    val color = if (isPositive) PositiveGreen else NegativeRed
    val formattedText = value?.let {
        String.format(Locale.US, "%s%.1f%%", if (isPositive) "▲ " else "▼ ", kotlin.math.abs(it))
    } ?: "-"

    Text(
        text = formattedText,
        modifier = modifier,
        textAlign = TextAlign.End,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    )
}

@Composable
fun SparklineChart(
    prices: List<Double>,
    isUp: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isUp) PositiveGreen else NegativeRed

    Canvas(modifier = modifier) {
        if (prices.size < 2) return@Canvas
        val min = prices.minOrNull() ?: 0.0
        val max = prices.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0

        val path = Path()
        val stepX = size.width / (prices.size - 1)

        prices.forEachIndexed { i, p ->
            val x = i * stepX
            val y = size.height - (((p - min) / range) * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 1.75.dp.toPx())
        )
    }
}

// Preview Light Mode
@Preview(showBackground = true, name = "Light Mode")
@Composable
fun CoinGeckoScreenPreviewLight() {
    BaiTap02Theme(darkTheme = false) {
        CoinGeckoScreenContent(
            state = sampleSuccessState(),
            cooldownSeconds = 0,
            onRefresh = {}
        )
    }
}

// Preview Dark Mode
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun CoinGeckoScreenPreviewDark() {
    BaiTap02Theme(darkTheme = true) {
        CoinGeckoScreenContent(
            state = sampleSuccessState(),
            cooldownSeconds = 0,
            onRefresh = {}
        )
    }
}

private fun sampleSuccessState() = MarketUiState.Success(
    coins = listOf(
        CoinItem(
            id = "bitcoin",
            symbol = "btc",
            name = "Bitcoin",
            image = "",
            rank = 1,
            currentPrice = 75304.35,
            totalVolume = 31430721105.0,
            marketCap = 1503375615064.0,
            change1h = 1.3,
            change24h = -1.4,
            change7d = -3.9,
            sparkline = Sparkline(listOf(75000.0, 76000.0, 75500.0, 75304.35))
        )
    ),
    global = GlobalData(
        activeCryptos = 22874,
        marketsCount = 1522,
        totalMarketCap = mapOf("usd" to 2527568735040.0),
        totalVolume = mapOf("usd" to 86894357155.0),
        dominancePercentage = mapOf("btc" to 59.58, "eth" to 10.30),
        marketCapChange24h = -2.99,
        fearAndGreed = FearAndGreed(37, "fear")
    )
)
