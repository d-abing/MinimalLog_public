package com.aube.minimallog.presentation.ui.component

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdaptiveBanner(
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val density = LocalDensity.current

    // adSize와 그에 따른 dp 높이를 상태로 유지
    var adSize by remember { mutableStateOf(activity.adSizeForWidth()) }
    var adHeightDp by remember { mutableStateOf(0.dp) }

    // 구성 변경(회전/폭) 시 다시 계산
    val config = LocalConfiguration.current
    LaunchedEffect(config) {
        adSize = activity.adSizeForWidth()
        val px = adSize.getHeightInPixels(context)
        adHeightDp = with(density) { px.toDp() }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp),              // ✅ 높이 명시
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            if (adView.adSize != adSize) {
                adView.setAdSize(adSize)
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}

private fun Activity.adSizeForWidth(): AdSize {
    val dm = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(dm)
    val adWidthPixels = dm.widthPixels.toFloat()
    val density = dm.density
    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
}

private tailrec fun Context.findActivity(): Activity {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> throw IllegalStateException("Activity not found from context.")
    }
}