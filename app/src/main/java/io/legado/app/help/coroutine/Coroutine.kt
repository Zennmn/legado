package io.legado.app.help.coroutine

import io.legado.app.utils.printOnDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Coroutine<T>(
    private val scope: CoroutineScope,
    context: CoroutineContext = Dispatchers.IO,
    private val startOption: CoroutineStart = CoroutineStart.DEFAULT,
    private val executeContext: CoroutineContext = Dispatchers.Main,
    private val semaphore: Semaphore? = null,
    block: suspend CoroutineScope.() -> T
) {

    companion object {
        private val DEFAULT = MainScope()

        fun <T> async(
            scope: CoroutineScope = DEFAULT,
            context: CoroutineContext = Dispatchers.IO,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            executeContext: CoroutineContext = Dispatchers.Main,
            semaphore: Semaphore? = null,
            block: suspend CoroutineScope.() -> T
        ): Coroutine<T> = Coroutine(scope, context, start, executeContext, semaphore, block)
    }

    private val job: Job = executeInternal(context, block)
    private var start: VoidCallback? = null
    private var success: Callback<T>? = null
    private var error: Callback<Throwable>? = null
    private var finally: VoidCallback? = null
    private var cancel: VoidCallback? = null
    private var timeMillis: Long? = null
    private var errorReturn: Result<T>? = null

    val isCancelled: Boolean get() = job.isCancelled
    val isActive: Boolean get() = job.isActive
    val isCompleted: Boolean get() = job.isCompleted

    fun timeout(timeMillis: () -> Long): Coroutine<T> = timeout(timeMillis())

    fun timeout(timeMillis: Long): Coroutine<T> {
        this.timeMillis = timeMillis
        return this
    }

    fun onErrorReturn(value: () -> T?): Coroutine<T> = onErrorReturn(value())

    fun onErrorReturn(value: T?): Coroutine<T> {
        errorReturn = Result(value)
        return this
    }

    fun onStart(context: CoroutineContext? = null, block: suspend CoroutineScope.() -> Unit): Coroutine<T> {
        start = VoidCallback(context, block)
        return this
    }

    fun onSuccess(context: CoroutineContext? = null, block: suspend CoroutineScope.(T) -> Unit): Coroutine<T> {
        success = Callback(context, block)
        return this
    }

    fun onError(context: CoroutineContext? = null, block: suspend CoroutineScope.(Throwable) -> Unit): Coroutine<T> {
        error = Callback(context, block)
        return this
    }

    fun onFinally(context: CoroutineContext? = null, block: suspend CoroutineScope.() -> Unit): Coroutine<T> {
        finally = VoidCallback(context, block)
        return this
    }

    fun onCancel(context: CoroutineContext? = null, block: suspend CoroutineScope.() -> Unit): Coroutine<T> {
        cancel = VoidCallback(context, block)
        job.invokeOnCompletion {
            if (it is CancellationException && it !is ActivelyCancelException) {
                cancel()
            }
        }
        return this
    }

    fun cancel(cause: ActivelyCancelException = ActivelyCancelException()) {
        if (!job.isCancelled) job.cancel(cause)
        cancel?.let { callback ->
            DEFAULT.launch(executeContext) { dispatchVoidCallback(this, callback) }
        }
    }

    fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle = job.invokeOnCompletion(handler)
    fun start() = job.start()

    private fun executeInternal(context: CoroutineContext, block: suspend CoroutineScope.() -> T): Job {
        return (scope.plus(executeContext)).launch(start = startOption) {
            semaphore?.acquire()
            try {
                start?.let { dispatchVoidCallback(this, it) }
                ensureActive()
                val value = executeBlock(context, timeMillis ?: 0L, block)
                ensureActive()
                success?.let { dispatchCallback(this, value, it) }
            } catch (e: Throwable) {
                e.printOnDebug()
                val consumed = errorReturn?.value?.let { value ->
                    success?.let { dispatchCallback(this, value, it) }
                    true
                } ?: false
                if (!consumed) error?.let { dispatchCallback(this, e, it) }
            } finally {
                try {
                    finally?.let { dispatchVoidCallback(this, it) }
                } finally {
                    semaphore?.release()
                }
            }
        }
    }

    private suspend fun dispatchVoidCallback(scope: CoroutineScope, callback: VoidCallback) {
        if (callback.context == null) {
            callback.block.invoke(scope)
        } else {
            withContext(callback.context) { callback.block.invoke(this) }
        }
    }

    private suspend fun <R> dispatchCallback(scope: CoroutineScope, value: R, callback: Callback<R>) {
        if (!scope.isActive) return
        if (callback.context == null) {
            callback.block.invoke(scope, value)
        } else {
            withContext(callback.context) { callback.block.invoke(this, value) }
        }
    }

    private suspend fun executeBlock(
        context: CoroutineContext,
        timeMillis: Long,
        block: suspend CoroutineScope.() -> T
    ): T = withContext(context) {
        if (timeMillis > 0L) withTimeout(timeMillis) { block() } else block()
    }

    private data class Result<out T>(val value: T?)
    private class VoidCallback(val context: CoroutineContext?, val block: suspend CoroutineScope.() -> Unit)
    private class Callback<VALUE>(val context: CoroutineContext?, val block: suspend CoroutineScope.(VALUE) -> Unit)
}
