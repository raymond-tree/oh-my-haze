package com.ohmyhaze.monitoring

import com.ohmyhaze.data.ApiCheckResult
import com.ohmyhaze.data.ApplyResponseOutcome
import com.ohmyhaze.data.RequestSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import com.ohmyhaze.data.CheckStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coalesces overlapping workers in this process; durable revision validation remains in the store. */
internal object AirQualityRequestCoordinator {
    private val mutex = Mutex()
    private val requests = mutableMapOf<Long, CompletableDeferred<ApplyResponseOutcome>>()

    suspend fun execute(
        snapshot: RequestSnapshot,
        fetch: suspend () -> ApiCheckResult,
        commit: suspend (ApiCheckResult) -> ApplyResponseOutcome,
    ): ApplyResponseOutcome {
        val (shared, ownsRequest) = mutex.withLock {
            val existing = requests[snapshot.requestRevision]
            if (existing != null) existing to false
            else CompletableDeferred<ApplyResponseOutcome>().also {
                requests[snapshot.requestRevision] = it
            } to true
        }
        if (!ownsRequest) return shared.await()

        try {
            val response = try {
                fetch()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ApiCheckResult.Failure(
                    status = CheckStatus.NETWORK_ERROR,
                    safeMessage = "Could not reach Open-Meteo. Try again later.",
                )
            }
            val outcome = commit(response)
            shared.complete(outcome)
            return outcome
        } catch (cancelled: CancellationException) {
            shared.cancel(cancelled)
            throw cancelled
        } catch (error: Throwable) {
            shared.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (requests[snapshot.requestRevision] === shared) requests.remove(snapshot.requestRevision)
            }
        }
    }
}
