package com.rcmiku.music.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.ui.theme.LocalJetMeloExtendedColors

@Composable
fun QualityBadge(
    qualityText: String,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalJetMeloExtendedColors.current
    Text(
        text = qualityText,
        style = MaterialTheme.typography.labelSmall,
        color = extendedColors.onEmberAccent,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(JetMeloShapes.extraSmall)
            .background(extendedColors.emberAccent)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun VipBadge(
    modifier: Modifier = Modifier,
    vipText: String = "VIP"
) {
    Text(
        text = vipText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(JetMeloShapes.extraSmall)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
