package com.example.coccoc.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun HeroImageCard(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    height: Dp = 220.dp,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    gradientStartAlpha: Float = 0f,
    gradientEndAlpha: Float = 0.8f,
    fallbackIcon: String = "🎙️",
    showElevation: Boolean = true
) {
    val surfaceModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = surfaceModifier,
        shape = RoundedCornerShape(cornerRadius),
        shadowElevation = if (showElevation) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            // Image or fallback gradient
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent.copy(alpha = gradientStartAlpha),
                                    Color.Black.copy(alpha = gradientEndAlpha)
                                ),
                                startY = height.value * 0.4f
                            )
                        )
                )
            } else {
                // Fallback gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fallbackIcon,
                        fontSize = 72.sp
                    )
                }
            }

            // Text overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                badge?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Variant for podcast detail screen with larger size
 */
@Composable
fun PodcastHeroImage(
    imageUrl: String?,
    title: String,
    pubDate: String,
    duration: String?,
    modifier: Modifier = Modifier
) {
    val subtitle = buildString {
        if (pubDate.isNotEmpty()) {
            append(pubDate)
        }
        if (!duration.isNullOrEmpty()) {
            if (isNotEmpty()) append(" • ")
            append(duration)
        }
    }

    HeroImageCard(
        imageUrl = imageUrl,
        title = title,
        modifier = modifier,
        subtitle = subtitle.takeIf { it.isNotEmpty() },
        height = 350.dp,
        cornerRadius = 0.dp,
        showElevation = false,
        fallbackIcon = "🎙️"
    )
}
