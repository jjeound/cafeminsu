package com.ssafy.cafeminsu.feature.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.LocationManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

data class StoreMapMarker(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

fun shouldRenderKakaoMap(kakaoNativeAppKey: String): Boolean = kakaoNativeAppKey.isNotBlank()

@Composable
fun StoreMapView(
    markers: List<StoreMapMarker>,
    selectedStoreId: String?,
    kakaoNativeAppKey: String,
    onMarkerClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (shouldRenderKakaoMap(kakaoNativeAppKey)) {
        KakaoStoreMap(
            markers = markers,
            selectedStoreId = selectedStoreId,
            onMarkerClick = onMarkerClick,
            modifier = modifier,
        )
    } else {
        StoreMapPlaceholder(modifier = modifier)
    }
}

@Composable
private fun KakaoStoreMap(
    markers: List<StoreMapMarker>,
    selectedStoreId: String?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val selectedColorArgb = CafeMinsuTheme.colors.primary.toArgb()
    val selectedDotColorArgb = CafeMinsuTheme.colors.onPrimary.toArgb()
    val defaultColorArgb = CafeMinsuTheme.colors.ink.toArgb()
    val defaultDotColorArgb = CafeMinsuTheme.colors.onPrimary.toArgb()
    val selectedBitmap = remember(selectedColorArgb, selectedDotColorArgb) {
        createMarkerBitmap(selectedColorArgb, selectedDotColorArgb)
    }
    val defaultBitmap = remember(defaultColorArgb, defaultDotColorArgb) {
        createMarkerBitmap(defaultColorArgb, defaultDotColorArgb)
    }
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapStarted by remember { mutableStateOf(false) }
    var renderedMarkers by remember { mutableStateOf<List<StoreMapMarker>?>(null) }
    var renderedSelectedStoreId by remember { mutableStateOf<String?>(null) }

    if (mapFailed) {
        StoreMapPlaceholder(modifier = modifier)
        return
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (mapStarted) mapView.resume()
                Lifecycle.Event.ON_PAUSE -> if (mapStarted) mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight()),
        factory = {
            mapView.doOnAttach {
                mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit

                        override fun onMapError(error: Exception?) {
                            Log.e(MapLogTag, "카카오맵을 불러오지 못했어요", error)
                            mapFailed = true
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            kakaoMap = map
                            map.setOnLabelClickListener { _, _, label ->
                                (label.tag as? String)?.let(currentOnMarkerClick)
                                true
                            }
                            map.renderStoreMarkers(
                                markers,
                                selectedStoreId,
                                selectedBitmap,
                                defaultBitmap
                            )
                            renderedMarkers = markers
                            renderedSelectedStoreId = selectedStoreId
                            map.frameStoresOrFallback(markers, context)
                        }
                    },
                )
                mapStarted = true
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    mapView.resume()
                }
            }
            mapView
        },
        update = {
            val map = kakaoMap ?: return@AndroidView
            if (markers != renderedMarkers || selectedStoreId != renderedSelectedStoreId) {
                map.renderStoreMarkers(markers, selectedStoreId, selectedBitmap, defaultBitmap)
                renderedMarkers = markers
                renderedSelectedStoreId = selectedStoreId
            }
        },
    )
}

private fun KakaoMap.renderStoreMarkers(
    markers: List<StoreMapMarker>,
    selectedStoreId: String?,
    selectedBitmap: Bitmap,
    defaultBitmap: Bitmap,
) {
    val manager = labelManager ?: return
    val layer = manager.getLayer(StoreLayerId)
        ?: manager.addLayer(LabelLayerOptions.from(StoreLayerId))
        ?: return
    val selectedStyles =
        manager.addLabelStyles(LabelStyles.from(LabelStyle.from(selectedBitmap))) ?: return
    val defaultStyles =
        manager.addLabelStyles(LabelStyles.from(LabelStyle.from(defaultBitmap))) ?: return

    layer.removeAll()
    markers.filter { it.hasValidCoordinate() }.forEach { marker ->
        layer.addLabel(
            LabelOptions.from(LatLng.from(marker.latitude, marker.longitude))
                .setStyles(if (marker.id == selectedStoreId) selectedStyles else defaultStyles)
                .setTag(marker.id),
        )
    }
}

private fun KakaoMap.frameStoresOrFallback(
    markers: List<StoreMapMarker>,
    context: Context,
): Boolean {
    val userLatLng = lastKnownUserLatLng(context)
    val firstStoreLatLng = markers.firstOrNull { it.hasValidCoordinate() }
        ?.let { LatLng.from(it.latitude, it.longitude) }
    val target = userLatLng ?: firstStoreLatLng ?: LatLng.from(DefaultLat, DefaultLng)
    moveCamera(CameraUpdateFactory.newCenterPosition(target, CameraZoomLevel))
    return userLatLng != null || firstStoreLatLng != null
}

@SuppressLint("MissingPermission")
private fun lastKnownUserLatLng(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
        ?: return null
    return LatLng.from(location.latitude, location.longitude)
}

private fun StoreMapMarker.hasValidCoordinate(): Boolean =
    latitude in MinLat..MaxLat && longitude in MinLng..MaxLng &&
            (latitude != 0.0 || longitude != 0.0)

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun mapHeight(): Dp =
    CafeMinsuTheme.spacing.space18 * MapHeightMultiplier + CafeMinsuTheme.spacing.space3

@Composable
private fun StoreMapPlaceholder(modifier: Modifier = Modifier) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight()),
        shape = CafeMinsuTheme.shapes.radiusXl,
        color = colors.surfaceCard,
        contentColor = colors.ink,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val verticalGap = size.width / MapGridLineCount
                    val horizontalGap = size.height / MapGridLineCount
                    repeat(MapGridLineCount + 1) { index ->
                        drawLine(
                            color = colors.hairline,
                            start = Offset(x = verticalGap * index, y = 0f),
                            end = Offset(x = verticalGap * index, y = size.height),
                        )
                        drawLine(
                            color = colors.hairline,
                            start = Offset(x = 0f, y = horizontalGap * index),
                            end = Offset(x = size.width, y = horizontalGap * index),
                        )
                    }
                }
                .padding(spacing.space5),
        ) {
            Surface(
                shape = CafeMinsuTheme.shapes.radiusPill,
                color = colors.canvas,
                contentColor = colors.ink,
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = spacing.space4,
                        vertical = spacing.space2
                    ),
                    text = "지도 미리보기",
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.ink,
                )
            }

            Box(
                modifier = Modifier
                    .size(CafeMinsuTheme.spacing.space10)
                    .background(color = colors.primary, shape = CafeMinsuTheme.shapes.radiusPill)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(CafeMinsuTheme.spacing.space2)
                        .background(
                            color = colors.onPrimary,
                            shape = CafeMinsuTheme.shapes.radiusPill
                        ),
                )
            }
        }
    }
}

private fun createMarkerBitmap(
    fillColorArgb: Int,
    dotColorArgb: Int,
): Bitmap {
    val bitmap =
        Bitmap.createBitmap(MarkerBitmapSizePx, MarkerBitmapSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = MarkerBitmapSizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = fillColorArgb
    canvas.drawCircle(center, center, center, paint)

    paint.color = dotColorArgb
    canvas.drawCircle(center, center, center * MarkerDotRatio, paint)

    return bitmap
}

private const val MapHeightMultiplier = 2
private const val MapGridLineCount = 4
private const val CameraZoomLevel = 15
private const val MarkerBitmapSizePx = 56
private const val MarkerDotRatio = 0.32f
private const val DefaultLat = 37.5665
private const val DefaultLng = 126.9780
private const val MinLat = -90.0
private const val MaxLat = 90.0
private const val MinLng = -180.0
private const val MaxLng = 180.0
private const val MapLogTag = "StoreMapView"
private const val StoreLayerId = "store-markers"
