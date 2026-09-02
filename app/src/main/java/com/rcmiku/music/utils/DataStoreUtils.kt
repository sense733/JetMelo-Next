package com.rcmiku.music.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CachedPreference<T>(
    private val context: Context,
    private val key: Preferences.Key<T>,
    private val defaultValue: T,
    scope: CoroutineScope,
) : ReadOnlyProperty<Any?, T> {
    @Volatile
    private var cachedValue: T = defaultValue

    init {
        scope.launch(Dispatchers.IO) {
            val targetContext = runCatching { context.applicationContext }.getOrNull() ?: context
            runCatching {
                targetContext.dataStore.data
                    .map { it[key] ?: defaultValue }
                    .first()
            }.getOrNull()?.let { cachedValue = it }

            runCatching {
                targetContext.dataStore.data
                    .map { it[key] ?: defaultValue }
                    .distinctUntilChanged()
                    .collect { cachedValue = it }
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = cachedValue
}

class CachedEnumPreference<T : Enum<T>>(
    private val context: Context,
    private val key: Preferences.Key<String>,
    private val defaultValue: T,
    scope: CoroutineScope,
    private val toEnum: (String?) -> T,
) : ReadOnlyProperty<Any?, T> {
    @Volatile
    private var cachedValue: T = defaultValue

    init {
        scope.launch(Dispatchers.IO) {
            val targetContext = runCatching { context.applicationContext }.getOrNull() ?: context
            runCatching {
                targetContext.dataStore.data
                    .map { it[key] }
                    .first()
            }.getOrNull()?.let { cachedValue = toEnum(it) }

            runCatching {
                targetContext.dataStore.data
                    .map { it[key] }
                    .distinctUntilChanged()
                    .collect { cachedValue = toEnum(it) }
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = cachedValue
}

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
    scope: CoroutineScope,
): ReadOnlyProperty<Any?, T> = CachedPreference(context, key, defaultValue, scope)

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
    scope: CoroutineScope,
): ReadOnlyProperty<Any?, T> = CachedEnumPreference(
    context = context,
    key = key,
    defaultValue = defaultValue,
    scope = scope,
    toEnum = { it.toEnum(defaultValue) },
)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember(key) {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsState(initial = defaultValue)

    return remember(key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        runCatching {
                            context.dataStore.edit {
                                it[key] = value
                            }
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state = remember(key) {
        context.dataStore.data
            .map { it[key].toEnum(defaultValue = defaultValue) }
            .distinctUntilChanged()
    }.collectAsState(initial = defaultValue)

    return remember(key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        runCatching {
                            context.dataStore.edit {
                                it[key] = value.name
                            }
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
fun <T> collectPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): State<T> {
    val context = LocalContext.current
    return remember(key) {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsState(initial = defaultValue)
}

inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    if (this == null) defaultValue
    else try {
        enumValueOf(this)
    } catch (e: IllegalArgumentException) {
        defaultValue
    }