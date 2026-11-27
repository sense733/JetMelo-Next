package com.rcmiku.music.viewModel

import com.rcmiku.ncmapi.model.LyricData
import com.rcmiku.ncmapi.model.LyricResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class LyricViewModelTest {

    private suspend fun awaitUntil(timeoutMs: Long = 2000L, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!predicate()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Timed out after ${timeoutMs}ms waiting for condition")
            }
            delay(10L)
        }
    }

    @Test
    fun prefetchPopulatesParsedLyricValueWithoutActiveCollectors() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val fetchCount = AtomicInteger(0)
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    fetchCount.incrementAndGet()
                    Result.success(
                        LyricResponse(
                            lrc = LyricData(lyric = "[00:00.00]Line 1 ($id)\n[00:05.00]Line 2")
                        )
                    )
                },
                scopeOverride = scope
            )

            vm.fetchLyric(42L)
            awaitUntil { vm.parsedLyric.value?.musicId == 42L }

            val parsed = vm.parsedLyric.value
            assertNotNull(parsed)
            assertEquals(42L, parsed?.musicId)
            assertEquals(2, parsed?.lines?.size)
            assertEquals(1, fetchCount.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun duplicateFetchWhileInFlightDoesNotCancelOrRestartRequest() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val fetchCount = AtomicInteger(0)
            val gate = CompletableDeferred<Unit>()
            val vm = LyricViewModel(
                lyricFetcher = {
                    fetchCount.incrementAndGet()
                    gate.await()
                    Result.success(LyricResponse(lrc = LyricData(lyric = "[00:01.00]Hello")))
                },
                scopeOverride = scope
            )

            vm.fetchLyric(100L)
            awaitUntil { fetchCount.get() == 1 }

            vm.fetchLyric(100L)
            delay(40L)
            assertEquals(1, fetchCount.get())

            gate.complete(Unit)
            awaitUntil { vm.parsedLyric.value?.musicId == 100L }
            assertEquals(1, fetchCount.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedFetchCanBeRetriedAndClearResetsState() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val attempt = AtomicInteger(0)
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    if (attempt.incrementAndGet() == 1) {
                        Result.failure(IllegalStateException("network error"))
                    } else {
                        Result.success(LyricResponse(lrc = LyricData(lyric = "[00:02.00]Recovered $id")))
                    }
                },
                scopeOverride = scope
            )

            vm.fetchLyric(77L)
            awaitUntil { vm.lyricError.value != null }
            assertNull(vm.parsedLyric.value)

            vm.fetchLyric(77L)
            awaitUntil { vm.parsedLyric.value?.musicId == 77L }
            assertNull(vm.lyricError.value)
            assertEquals(2, attempt.get())

            vm.clearLyric()
            awaitUntil { vm.parsedLyric.value == null }

            vm.fetchLyric(77L)
            awaitUntil { vm.parsedLyric.value?.musicId == 77L }
            assertEquals(3, attempt.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cachedSongsSwitchImmediatelyWithoutDuplicateNetworkFetch() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val fetchCount = AtomicInteger(0)
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    fetchCount.incrementAndGet()
                    Result.success(
                        LyricResponse(
                            lrc = LyricData(lyric = "[00:00.00]Song $id")
                        )
                    )
                },
                scopeOverride = scope,
                ioDispatcher = Dispatchers.Default,
                defaultDispatcher = Dispatchers.Default
            )

            vm.fetchLyric(101L)
            awaitUntil { vm.parsedLyric.value?.musicId == 101L }
            assertEquals(1, fetchCount.get())

            vm.fetchLyric(202L)
            awaitUntil { vm.parsedLyric.value?.musicId == 202L }
            assertEquals(2, fetchCount.get())

            vm.fetchLyric(101L)
            awaitUntil { vm.parsedLyric.value?.musicId == 101L }
            assertEquals(2, fetchCount.get())
            assertEquals("Song 101", vm.parsedLyric.value?.lines?.firstOrNull()?.text)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun switchingSongsClearsPriorErrorSynchronously() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val gate = CompletableDeferred<Unit>()
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    if (id == 1L) {
                        Result.failure(IllegalStateException("timeout"))
                    } else {
                        gate.await()
                        Result.success(LyricResponse(lrc = LyricData(lyric = "[00:00.00]Ok")))
                    }
                },
                scopeOverride = scope,
                ioDispatcher = Dispatchers.Default,
                defaultDispatcher = Dispatchers.Default
            )

            vm.fetchLyric(1L)
            awaitUntil { vm.lyricError.value != null }
            assertEquals(1L, vm.errorMusicId.value)

            vm.fetchLyric(2L)
            assertNull("Prior song error must be cleared immediately on switch", vm.lyricError.value)
            assertNull("Prior errorMusicId must be cleared immediately on switch", vm.errorMusicId.value)

            gate.complete(Unit)
            awaitUntil { vm.parsedLyric.value?.musicId == 2L }
            assertNull(vm.lyricError.value)
            assertNull(vm.errorMusicId.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun peekCachedParsedLyricReturnsSynchronouslyOnCacheHit() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    Result.success(LyricResponse(lrc = LyricData(lyric = "[00:00.00]Cached $id")))
                },
                scopeOverride = scope,
                ioDispatcher = Dispatchers.Default,
                defaultDispatcher = Dispatchers.Default
            )

            assertNull(vm.peekCachedParsedLyric(501L))
            vm.fetchLyric(501L)
            awaitUntil { vm.parsedLyric.value?.musicId == 501L }

            vm.fetchLyric(502L)
            awaitUntil { vm.parsedLyric.value?.musicId == 502L }

            val peeked = vm.peekCachedParsedLyric(501L)
            assertNotNull(peeked)
            assertEquals(501L, peeked?.musicId)
            assertEquals("Cached 501", peeked?.lines?.firstOrNull()?.text)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancelledInFlightFetchDoesNotEmitErrorState() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val started = CompletableDeferred<Unit>()
            val vm = LyricViewModel(
                lyricFetcher = { id ->
                    if (id == 10L) {
                        started.complete(Unit)
                        delay(5000L)
                        Result.failure(CancellationException("cancelled"))
                    } else {
                        Result.success(LyricResponse(lrc = LyricData(lyric = "[00:00.00]Fast $id")))
                    }
                },
                scopeOverride = scope,
                ioDispatcher = Dispatchers.Default,
                defaultDispatcher = Dispatchers.Default
            )

            vm.fetchLyric(10L)
            started.await()
            vm.fetchLyric(20L)

            awaitUntil { vm.parsedLyric.value?.musicId == 20L }
            assertNull(vm.lyricError.value)
            assertNull(vm.errorMusicId.value)
        } finally {
            scope.cancel()
        }
    }
}
