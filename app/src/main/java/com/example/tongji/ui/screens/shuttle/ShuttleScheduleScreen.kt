package com.example.tongji.ui.screens.shuttle

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.tongji.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShuttleScheduleScreen(onBack: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班车时刻表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 发布时间信息
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "发布时间：2025-09-15",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 班车时刻表图片
            item {
                if (isPreview) {
                    // Preview 占位符
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "班车时刻表图片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 缩放容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 8.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                                    scale = newScale
                                    offset += pan
                                }
                            }
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.shuttle_schedule),
                            contentDescription = "班车时刻表",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // 底部留白
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
