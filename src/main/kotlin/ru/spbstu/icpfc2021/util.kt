package ru.spbstu.icpfc2021

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlin.coroutines.CoroutineContext

suspend fun <T, R> Collection<T>.mapAsync(scope: CoroutineScope, body: suspend (T) -> R): List<R> =
    with(scope) {
        map { async { body(it) } }.awaitAll()
    }

suspend fun <T> Collection<T>.filterAsync(scope: CoroutineScope, body: suspend (T) -> Boolean): List<T> =
    with(scope) {
        map { async { it to body(it) } }.awaitAll().mapNotNull { r -> r.first.takeIf { r.second } }
    }

