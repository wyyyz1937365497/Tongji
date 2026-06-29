package com.example.tongji.ui.screens.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.tongji.TongjiApp
import com.example.tongji.data.repository.SeatInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.max

private val STATUS_FREE = Color(0xFF4CAF50)
private val STATUS_USE = Color(0xFFFF5722)
private val STATUS_LEAVE = Color(0xFFFFC107)
private val STATUS_CLOSE = Color(0xFF9E9E9E)

fun statusToImageType(status: String): String = when (status) {
    "1" -> "free"
    "2" -> "book"
    "6" -> "use"
    "7" -> "leave"
    "0" -> "close"
    else -> "not"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatMapScreen(areaId: String, areaName: String, labelId: String? = null, onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var seats by remember { mutableStateOf<List<SeatInfo>>(emptyList()) }
    var imageMap by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                val seatsDeferred = async { app.librarySpaceRepository.fetchSeats(areaId, labelId) }
                val mapUrlsDeferred = async { app.librarySpaceRepository.fetchSeatMap(areaId) }

                val seatList = seatsDeferred.await()
                val mapUrls = mapUrlsDeferred.await()
                seats = seatList

                val bitmaps = mutableMapOf<String, ImageBitmap>()
                mapUrls.entries.map { (type, url) ->
                    async {
                        val bytes = app.librarySpaceRepository.downloadImage(url)
                        if (bytes != null) {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                synchronized(bitmaps) { bitmaps[type] = bmp.asImageBitmap() }
                            }
                        }
                    }
                }.awaitAll()
                imageMap = bitmaps
            } catch (e: Exception) {
                errorMsg = e.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val statusCounts = seats.groupBy { it.status }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(areaName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SeatLegend(statusCounts, seats.size)
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMsg != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                ZoomableSeatMap(
                    seats = seats,
                    imageMap = imageMap,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SeatLegend(statusCounts: Map<String, List<SeatInfo>>, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(STATUS_FREE, "空闲", statusCounts["1"]?.size ?: 0)
        LegendItem(STATUS_USE, "使用中", statusCounts["6"]?.size ?: 0)
        LegendItem(STATUS_LEAVE, "暂离", statusCounts["7"]?.size ?: 0)
        LegendItem(STATUS_CLOSE, "不可用", statusCounts["0"]?.size ?: 0)
        Text(
            "共 $total",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color))
        Spacer(Modifier.width(4.dp))
        Text("$label $count", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ZoomableSeatMap(
    seats: List<SeatInfo>,
    imageMap: Map<String, ImageBitmap>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val baseBitmap = imageMap["not"] ?: imageMap["free"] ?: imageMap.values.firstOrNull()
    if (baseBitmap == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("图片加载失败", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.3f, 8f)
                    scale = newScale
                    offset += pan
                }
            }
    ) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        val imgW = baseBitmap.width.toFloat()
        val imgH = baseBitmap.height.toFloat()
        val fitScale = max(viewportW / imgW, viewportH / imgH)
        val mapW = imgW * fitScale * scale
        val mapH = imgH * fitScale * scale

        val mapX = offset.x
        val mapY = offset.y

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = baseBitmap,
                dstOffset = IntOffset(mapX.toInt(), mapY.toInt()),
                dstSize = IntSize(mapW.toInt(), mapH.toInt())
            )

            seats.forEach { seat ->
                val imgType = statusToImageType(seat.status)
                val srcBitmap = imageMap[imgType] ?: baseBitmap

                val srcImgW = srcBitmap.width
                val srcImgH = srcBitmap.height

                val srcX = (seat.x / 100f * srcImgW).toInt().coerceIn(0, srcImgW - 1)
                val srcY = (seat.y / 100f * srcImgH).toInt().coerceIn(0, srcImgH - 1)
                val srcW = (seat.width / 100f * srcImgW).toInt().coerceIn(1, srcImgW - srcX)
                val srcH = (seat.height / 100f * srcImgH).toInt().coerceIn(1, srcImgH - srcY)

                val dstX = mapX + seat.x / 100f * mapW
                val dstY = mapY + seat.y / 100f * mapH
                val dstW = seat.width / 100f * mapW
                val dstH = seat.height / 100f * mapH

                if (dstX + dstW < 0 || dstX > viewportW ||
                    dstY + dstH < 0 || dstY > viewportH
                ) return@forEach

                try {
                    drawImage(
                        image = srcBitmap,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(srcW, srcH),
                        dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
                        dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1))
                    )
                } catch (_: Exception) { }
            }
        }
    }
}
