package com.rcmiku.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.PlayArrowFill
import com.rcmiku.music.ui.theme.JetMeloShapes

/**
 * 歌单与专辑页面的粘性「播放全部」操作条 (Sticky Play All Bar)
 * 严格遵循《橙色框折叠效果完整实现文档》规范：
 * - stickyContentHeight = 64.dp
 * - 未吸顶状态：顶部圆角 16dp (StickyBackground 灰白交界过渡)
 * - 吸顶状态：平角且背景切换为实底 surfaceContainer，阻止下方歌曲穿透
 * - 播放按钮：44dp 圆形按钮，内嵌比例缩放的 PlayArrowFill
 */
@Composable
fun StickyPlayAllBar(
    trackCount: Int,
    onPlayAllClick: () -> Unit,
    isSticky: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    isLoading: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isSticky) 0.dp else 16.dp,
        animationSpec = tween(180),
        label = "stickyCornerRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSticky)
            MaterialTheme.colorScheme.surfaceContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "stickyContainerColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSticky) 3.dp else 0.dp,
        animationSpec = tween(180),
        label = "stickyElevation"
    )

    val shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = elevation, shape = shape),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Play all button + title + count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(JetMeloShapes.small)
                    .clickable(enabled = !isLoading && trackCount > 0) {
                        onPlayAllClick()
                    }
                    .padding(vertical = 6.dp)
            ) {
                // Circular Play Button (44dp, icon scaled 38/44)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PlayArrowFill,
                        contentDescription = stringResource(R.string.play_all),
                        tint = onAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.play_all),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.song_size, trackCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Optional trailing actions (e.g. Subscribe/Collect, Multi-select, Sort)
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
