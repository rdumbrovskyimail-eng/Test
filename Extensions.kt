package com.test.utils

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── String Extensions ─────────────────────────────────────────────────────────

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean =
    length >= 8 && any { it.isDigit() } && any { it.isLetter() }

fun String.isValidPhone(): Boolean =
    android.util.Patterns.PHONE.matcher(this).matches()

fun String.isValidUrl(): Boolean =
    android.util.Patterns.WEB_URL.matcher(this).matches()

fun String.capitalize(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.truncate(maxLength: Int, ellipsis: String = "..."): String =
    if (length <= maxLength) this else take(maxLength - ellipsis.length) + ellipsis

fun String.toSlug(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

fun String.removeExtraSpaces(): String =
    replace(Regex("\\s+"), " ").trim()

fun String.countWords(): Int =
    if (isBlank()) 0 else trim().split(Regex("\\s+")).size

fun String.isPalindrome(): Boolean {
    val cleaned = filter { it.isLetterOrDigit() }.lowercase()
    return cleaned == cleaned.reversed()
}

fun String.toCamelCase(): String {
    return split("_", "-", " ").joinToString("") { it.capitalize() }
        .replaceFirstChar { it.lowercase() }
}

fun String.toSnakeCase(): String =
    replace(Regex("([A-Z])"), "_$1").lowercase().trimStart('_')

fun String?.orEmpty(default: String = ""): String = this ?: default

// ── Int / Long Extensions ─────────────────────────────────────────────────────

fun Int.dp(context: Context): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), context.resources.displayMetrics)

fun Long.toReadableDate(pattern: String = "dd MMM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> toReadableDate()
    }
}

fun Long.toFormattedSize(): String = when {
    this < 1_024 -> "${this} B"
    this < 1_048_576 -> "${ "%.1f".format(this / 1_024.0) } KB"
    this < 1_073_741_824 -> "${ "%.1f".format(this / 1_048_576.0) } MB"
    else -> "${ "%.2f".format(this / 1_073_741_824.0) } GB"
}

fun Int.clamp(min: Int, max: Int) = maxOf(min, minOf(max, this))
fun Float.clamp(min: Float, max: Float) = maxOf(min, minOf(max, this))

fun Int.isEven(): Boolean = this % 2 == 0
fun Int.isOdd(): Boolean = this % 2 != 0

// ── List Extensions ───────────────────────────────────────────────────────────

fun <T> List<T>.second(): T = this[1]
fun <T> List<T>.secondOrNull(): T? = getOrNull(1)

fun <T> List<T>.chunkedBy(predicate: (T) -> Boolean): List<List<T>> {
    val result = mutableListOf<MutableList<T>>()
    var current = mutableListOf<T>()
    for (item in this) {
        if (predicate(item) && current.isNotEmpty()) {
            result.add(current)
            current = mutableListOf()
        }
        current.add(item)
    }
    if (current.isNotEmpty()) result.add(current)
    return result
}

fun <T> List<T>.uniqueBy(selector: (T) -> Any): List<T> {
    val seen = mutableSetOf<Any>()
    return filter { seen.add(selector(it)) }
}

fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    require(page >= 1) { "Page must be >= 1" }
    require(pageSize >= 1) { "Page size must be >= 1" }
    val fromIndex = (page - 1) * pageSize
    if (fromIndex >= size) return emptyList()
    return subList(fromIndex, minOf(fromIndex + pageSize, size))
}

// ── Flow Extensions ───────────────────────────────────────────────────────────

fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}

fun <T> kotlinx.coroutines.flow.Flow<T>.throttleFirst(windowDuration: Long): kotlinx.coroutines.flow.Flow<T> = kotlinx.coroutines.flow.flow {
    var lastEmitTime = 0L
    collect { value ->
        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= windowDuration) {
            lastEmitTime = now
            emit(value)
        }
    }
}

fun CoroutineScope.launchDebounced(
    delayMs: Long = 300L,
    block: suspend () -> Unit
): () -> Unit {
    var job: Job? = null
    return {
        job?.cancel()
        job = launch {
            delay(delayMs)
            block()
        }
    }
}

// ── Context Extensions ────────────────────────────────────────────────────────

fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        cm.activeNetwork?.let { network ->
            cm.getNetworkCapabilities(network)?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } ?: false
    } else {
        @Suppress("DEPRECATION")
        cm.activeNetworkInfo?.isConnected == true
    }
}

fun CoroutineScope.launchSafe(
    onError: (Throwable) -> Unit = {},
    block: suspend () -> Unit
): Job = launch {
    try {
        block()
    } catch (e: Exception) {
        onError(e)
    }
}

fun Context.isDarkMode(): Boolean {
    val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mode == Configuration.UI_MODE_NIGHT_YES
}

fun Context.hideKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.getAppVersion(): String = try {
    val info = packageManager.getPackageInfo(packageName, 0)
    info.versionName ?: "unknown"
} catch (e: android.content.pm.PackageManager.NameNotFoundException) {
    "unknown"
}

// ── Compose Extensions ────────────────────────────────────────────────────────

@Composable
fun Dp.toPx(): Float {
    val density = LocalDensity.current
    return with(density) { toPx() }
}

@Composable
fun Float.toDp(): Dp {
    val density = LocalDensity.current
    return with(density) { toDp() }
}

// ── Misc ──────────────────────────────────────────────────────────────────────

inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().find { it.name.equals(this, ignoreCase = true) }

fun <T> T?.ifNull(default: () -> T): T = this ?: default()

fun Boolean.toInt() = if (this) 1 else 0

fun <K, V> Map<K, V>.getOrThrow(key: K): V =
    get(key) ?: throw NoSuchElementException("Key $key not found")

fun <K, V> Map<K, V>.reverseMap(): Map<V, K> = entries.associate { (k, v) -> v to k }

object TimeUtils {
    fun nowMillis(): Long = System.currentTimeMillis()
    fun nowSeconds(): Long = nowMillis() / 1000
    fun isExpired(expiresAt: Long): Boolean = nowMillis() > expiresAt
    fun expiresIn(durationMs: Long): Long = nowMillis() + durationMs
    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}