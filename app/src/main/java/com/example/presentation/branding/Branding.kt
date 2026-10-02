package com.example.presentation.branding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val BrandNavy = Color(0xFF0B2A4A)
private val BrandBlue = Color(0xFF1565C0)
private val BrandGold = Color(0xFFF4B942)
private val BrandIce = Color(0xFFF4F8FC)

@Composable
fun AshianBrandMark(
    modifier: Modifier = Modifier,
    animate: Boolean = false
) {
    val scale = remember { Animatable(if (animate) 0.74f else 1f) }
    val alpha = remember { Animatable(if (animate) 0f else 1f) }
    val ring = remember { Animatable(if (animate) 0.25f else 1f) }

    LaunchedEffect(animate) {
        if (animate) {
            alpha.animateTo(1f, tween(320))
            scale.animateTo(1.06f, tween(520, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(180))
            ring.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
    }

    Canvas(
        modifier = modifier
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        val s = size.minDimension
        val left = (size.width - s) / 2f
        val top = (size.height - s) / 2f

        drawRoundRect(
            color = BrandNavy,
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = CornerRadius(s * 0.25f, s * 0.25f)
        )

        if (animate) {
            drawCircle(
                color = BrandBlue.copy(alpha = 0.26f * ring.value),
                radius = s * (0.39f + 0.08f * ring.value),
                center = center,
                style = Stroke(width = s * 0.025f)
            )
        }

        val roof = Path().apply {
            moveTo(left + s * 0.20f, top + s * 0.52f)
            lineTo(left + s * 0.50f, top + s * 0.24f)
            lineTo(left + s * 0.80f, top + s * 0.52f)
        }
        drawPath(
            path = roof,
            color = Color.White,
            style = Stroke(width = s * 0.085f, cap = StrokeCap.Round)
        )

        val body = Path().apply {
            moveTo(left + s * 0.31f, top + s * 0.49f)
            lineTo(left + s * 0.31f, top + s * 0.77f)
            lineTo(left + s * 0.69f, top + s * 0.77f)
            lineTo(left + s * 0.69f, top + s * 0.49f)
        }
        drawPath(
            path = body,
            color = Color.White,
            style = Stroke(width = s * 0.075f, cap = StrokeCap.Round)
        )

        drawRoundRect(
            color = BrandGold,
            topLeft = Offset(left + s * 0.455f, top + s * 0.585f),
            size = Size(s * 0.09f, s * 0.19f),
            cornerRadius = CornerRadius(s * 0.035f, s * 0.035f)
        )

        drawCircle(
            color = BrandGold,
            radius = s * 0.037f,
            center = Offset(left + s * 0.50f, top + s * 0.56f)
        )
    }
}

@Composable
fun AshianSplashScreen(
    onFinished: () -> Unit
) {
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(450))
        delay(850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandIce),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(contentAlpha.value)
        ) {
            AshianBrandMark(
                modifier = Modifier.size(118.dp),
                animate = true
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "آشیان ملک",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "سامانه هوشمند مدیریت املاک",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandBlue
            )
        }
    }
}
