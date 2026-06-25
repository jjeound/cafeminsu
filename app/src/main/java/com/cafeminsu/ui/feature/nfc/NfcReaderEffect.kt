package com.cafeminsu.ui.feature.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** NFC 가용성 — 화면이 어댑터 부재/비활성/정상 분기로 안내·동작을 결정한다. */
enum class NfcAvailability {
    Unsupported,
    Disabled,
    Ready,
}

/** 어댑터 존재/활성 여부 → 가용성(순수 함수, 테스트 대상). */
fun resolveNfcAvailability(hasAdapter: Boolean, enabled: Boolean): NfcAvailability =
    when {
        !hasAdapter -> NfcAvailability.Unsupported
        !enabled -> NfcAvailability.Disabled
        else -> NfcAvailability.Ready
    }

/**
 * 화면이 보일 때만([enabled]) 호스트 Activity 에 NFC reader mode 를 붙였다 [onDispose] 에서 해제한다.
 * 백그라운드 상시 스캔/자원 누수 금지. 콜백은 바인더 스레드에서 호출되므로 raw 추출 후 메인 스레드로 전달한다.
 * IO/예외는 안전 처리(크래시 금지), raw/태그 값은 로깅하지 않는다.
 */
@Composable
fun NfcReaderEffect(
    enabled: Boolean,
    onRawTagRead: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val adapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }
    val currentOnRead by rememberUpdatedState(onRawTagRead)

    if (activity == null || adapter == null || !enabled) {
        return
    }

    DisposableEffect(activity, adapter, enabled) {
        val callback = NfcAdapter.ReaderCallback { tag ->
            val raw = readNdefRaw(tag)
            if (!raw.isNullOrBlank()) {
                // ViewModel 상태는 메인 스레드에서 갱신한다(따닥 중복은 ViewModel 진행중 가드가 막는다).
                activity.runOnUiThread { currentOnRead(raw) }
            }
        }
        runCatching {
            adapter.enableReaderMode(activity, callback, READER_FLAGS, null)
        }
        onDispose {
            runCatching { adapter.disableReaderMode(activity) }
        }
    }
}

private fun readNdefRaw(tag: Tag?): String? {
    val ndef = tag?.let { runCatching { Ndef.get(it) }.getOrNull() } ?: return null
    return try {
        ndef.connect()
        val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
        NfcNdefParser.extractRawCode(message)
    } catch (_: Throwable) {
        // 연결 실패 시 캐시된 메시지로 폴백(크래시 금지).
        runCatching { ndef.cachedNdefMessage }.getOrNull()?.let(NfcNdefParser::extractRawCode)
    } finally {
        runCatching { ndef.close() }
    }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private const val READER_FLAGS =
    NfcAdapter.FLAG_READER_NFC_A or
        NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V
