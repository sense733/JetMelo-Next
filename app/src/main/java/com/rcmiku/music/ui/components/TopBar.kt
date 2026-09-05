package com.rcmiku.music.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.rcmiku.music.R
import com.rcmiku.music.ui.navigation.Screen

/**
 * 架构决策说明：顶栏采用轻量表达式封装并支持外部 Modifier 适配；
 * 路由判断统一引用 Screen 常量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavHostController,
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier
) = TopAppBar(
    modifier = modifier,
    title = { Text(stringResource(titleRes)) },
    actions = {
        IconButton(onClick = {
            navController.navigate(Screen.Search.route)
        }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(
                    R.string.search
                )
            )
        }
        IconButton(onClick = {
            navController.navigate(Screen.Settings.route)
        }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(
                    R.string.settings
                )
            )
        }
    })