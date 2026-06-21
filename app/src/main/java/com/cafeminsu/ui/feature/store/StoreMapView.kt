package com.cafeminsu.ui.feature.store

import android.Manifest
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
import com.cafeminsu.BuildConfig
import com.cafeminsu.ui.theme.CafeTheme
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

/** 지도에 찍을 매장 마커 한 개(좌표 + 식별/표시용 정보). */
data class StoreMapMarker(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * 카카오맵 네이티브 키가 설정돼 있을 때만 실제 지도를 렌더한다.
 * 키가 없으면(테스트/프리뷰/키 미설정 빌드) 플레이스홀더로 안전 폴백.
 */
fun shouldRenderKakaoMap(kakaoNativeAppKey: String): Boolean =
    kakaoNativeAppKey.isNotBlank()

@Composable
fun StoreMapView(
    markers: List<StoreMapMarker>,
    modifier: Modifier = Modifier,
) {
    if (shouldRenderKakaoMap(BuildConfig.KAKAO_NATIVE_APP_KEY)) {
        KakaoStoreMap(markers = markers, modifier = modifier)
    } else {
        StoreMapPlaceholder(modifier = modifier)
    }
}

@Composable
private fun KakaoStoreMap(
    markers: List<StoreMapMarker>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 마커 색은 디자인 토큰(primary/onPrimary)에서 — 네이티브 MapView 경계라 Compose 테마를 직접 못 써 ARGB int 로 넘긴다.
    val markerColorArgb = CafeTheme.colors.primary.toArgb()
    val markerDotColorArgb = CafeTheme.colors.onPrimary.toArgb()
    // 이 SDK 는 텍스트 전용 라벨/벡터 드로어블을 "ImageAsset is invalid" 로 거부한다 → 코드로 그린 비트맵 핀을 마커 아이콘으로 쓴다.
    val markerBitmap = remember(markerColorArgb, markerDotColorArgb) {
        createMarkerBitmap(fillColorArgb = markerColorArgb, dotColorArgb = markerDotColorArgb)
    }
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapStarted by remember { mutableStateOf(false) }
    // 이미 마커를 그린 목록. 매 recomposition 마다 removeAll+moveCamera 하면 카메라/타일 로딩이 churn 되므로 변경 시에만 다시 그린다.
    var renderedMarkers by remember { mutableStateOf<List<StoreMapMarker>?>(null) }
    // 유효 좌표 매장이 보이도록 카메라를 한 번만 맞춘다(이후 사용자의 이동/확대를 덮어쓰지 않게).
    var storesFramed by remember { mutableStateOf(false) }

    // 인증/렌더 실패 시 빈 화면 대신 플레이스홀더로 폴백한다.
    if (mapFailed) {
        StoreMapPlaceholder(modifier = modifier)
        return
    }

    // resume()/pause() 는 반드시 start()(=onMapReady) 이후에만 호출한다.
    // 엔진 준비 전에 resume() 이 먼저 돌면 RenderView Create Failure 가 난다.
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
        // GLSurfaceView 는 네이티브 surface 라 clip(둥근모서리) 을 주면 렌더뷰 생성이 깨진다. clip 미적용.
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight()),
        factory = {
            // MapView 가 윈도우에 attach 된 뒤 start() → resume() 순서로 호출한다.
            // attach 전 start 하거나 start 전에 resume 되면 RenderView Create Failure 가 난다.
            mapView.doOnAttach {
                mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception?) {
                            // 콘솔에 지도 제품/키 해시 미등록이면 MapAuthException 으로 여기 떨어진다 — logcat 확인.
                            Log.e(MapLogTag, "카카오맵을 불러오지 못했어요", error)
                            mapFailed = true
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            kakaoMap = map
                            map.renderStoreMarkers(markers, markerBitmap)
                            renderedMarkers = markers
                            storesFramed = map.frameStoresOrFallback(markers, context)
                        }
                    },
                )
                // start() 직후 resume()(현재 포그라운드면) — 정상 Activity 흐름과 동일 순서 보장.
                mapStarted = true
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    mapView.resume()
                }
            }
            mapView
        },
        update = {
            val map = kakaoMap
            if (map != null && markers != renderedMarkers) {
                map.renderStoreMarkers(markers, markerBitmap)
                renderedMarkers = markers
                // 좌표는 매장 상세가 도착한 뒤(2차 emit) 채워진다 → 그때 한 번 매장에 맞춰 카메라를 이동.
                if (!storesFramed) {
                    storesFramed = map.frameStoresOrFallback(markers, context)
                }
            }
        },
    )
}

private fun KakaoMap.renderStoreMarkers(
    markers: List<StoreMapMarker>,
    markerBitmap: Bitmap,
) {
    val manager = labelManager ?: return
    val layer = manager.layer ?: return
    // 코드로 그린 비트맵 핀을 아이콘으로 등록한다. (텍스트/벡터 드로어블은 이 SDK 가 거부함)
    val styles = manager.addLabelStyles(
        LabelStyles.from(LabelStyle.from(markerBitmap)),
    ) ?: return

    layer.removeAll()
    // 매장 목록 API 는 좌표를 주지 않아 대부분 (0,0) 이다 → 유효 좌표인 매장만 핀으로 찍는다.
    markers.filter { it.hasValidCoordinate() }.forEach { marker ->
        layer.addLabel(
            LabelOptions.from(LatLng.from(marker.latitude, marker.longitude))
                .setStyles(styles),
        )
    }
}

/** 디자인 토큰 색으로 그린 마커 핀(코랄 원 + 흰 점). 네이티브 라벨 아이콘으로 쓴다. */
private fun createMarkerBitmap(
    fillColorArgb: Int,
    dotColorArgb: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(MarkerBitmapSizePx, MarkerBitmapSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = MarkerBitmapSizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = fillColorArgb
    canvas.drawCircle(center, center, center, paint)

    paint.color = dotColorArgb
    canvas.drawCircle(center, center, center * MarkerDotRatio, paint)

    return bitmap
}

/**
 * 카메라: 내 위치(가까운 매장) → 유효 좌표 첫 매장 → 서울 기본값 순으로 센터링한다.
 * 내 위치나 매장 좌표로 확정됐으면 true 를 반환해 호출부가 재이동을 막는다(서울 기본값이면 false → 다음 emit 재시도).
 * ⚠ (0,0) 으로 옮기면 대서양 한복판(빈 화면)이라 절대 그쪽으로 가지 않는다.
 */
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

/** 위치 권한이 있으면 마지막으로 알려진 내 위치(LatLng), 없으면 null. */
@SuppressLint("MissingPermission")
private fun lastKnownUserLatLng(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
        ?: return null
    return LatLng.from(location.latitude, location.longitude)
}

/** (0,0) 같은 미제공 좌표를 거른다. 지구 범위 안 + (0,0) 아님. */
private fun StoreMapMarker.hasValidCoordinate(): Boolean =
    latitude in MinLat..MaxLat && longitude in MinLng..MaxLng &&
        (latitude != 0.0 || longitude != 0.0)

/** 정밀/대략 위치 권한 중 하나라도 부여됐는지. (UI 레이어가 권한 요청 여부를 결정할 때도 재사용.) */
fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

@Composable
private fun mapHeight(): Dp = CafeTheme.spacing.space18 * MapHeightMultiplier + CafeTheme.spacing.space3

@Composable
private fun StoreMapPlaceholder(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight()),
        shape = CafeTheme.shapes.radiusXl,
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
                shape = CafeTheme.shapes.radiusPill,
                color = colors.canvas,
                contentColor = colors.ink,
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = spacing.space4,
                        vertical = spacing.space2,
                    ),
                    text = "내 주변 지도",
                    style = CafeTheme.typography.caption,
                    color = colors.ink,
                )
            }

            PlaceholderMapMarker(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun PlaceholderMapMarker(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Box(
        modifier = modifier
            .size(CafeTheme.spacing.space10)
            .background(
                color = colors.primary,
                shape = CafeTheme.shapes.radiusPill,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CafeTheme.spacing.space2)
                .background(
                    color = colors.onPrimary,
                    shape = CafeTheme.shapes.radiusPill,
                ),
        )
    }
}

private const val MapHeightMultiplier = 2
private const val MapGridLineCount = 4
private const val CameraZoomLevel = 15
private const val MarkerBitmapSizePx = 56
private const val MarkerDotRatio = 0.32f
private const val DefaultLat = 37.5665 // 서울시청 — 좌표 미상일 때 기본 중심
private const val DefaultLng = 126.9780
private const val MinLat = -90.0
private const val MaxLat = 90.0
private const val MinLng = -180.0
private const val MaxLng = 180.0
private const val MapLogTag = "StoreMapView"
