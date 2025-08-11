package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.utils.Logging


/**
 * Repository implementation for a single object. Allows for persistance if the persistance source if provided, otherwise is mem-only.
 *
 * @param T a domain model
 * @param persistenceSource <optional> persistance mechanism to use to save/load data for this repository. Otherwise its mem-only.
 * @param prototype <optional> an instance of T to use as prototype, can be null if no persistance source will be used
 */
abstract class SingleObjectRepository<out T : BaseModel>(
    private val persistenceSource: PersistenceSource<T>? = null,
    private val prototype: T? = null,
) : Repository<T>, Logging {

    private val _data = MutableStateFlow<T?>(null)
    override val data: StateFlow<T?> get() = _data.asStateFlow()

    private val job = Job()
    private val scope = CoroutineScope(job + IODispatcher)

    override suspend fun create(data: @UnsafeVariance T) {
        _data.value = data
        persistenceSource?.save(data)
    }

    override suspend fun update(data: @UnsafeVariance T) {
        _data.value = data
        persistenceSource?.save(data)
    }

    override suspend fun delete(data: @UnsafeVariance T) {
        _data.value = null
        persistenceSource?.delete(data)
    }

    override suspend fun fetch(): T? {
        return _data.value ?: persistenceSource?.get(prototype!!).also { _data.value = it }
    }

    override suspend fun clear() {
        try {
            persistenceSource?.clear()
            scope.cancel()
        } catch (e: Exception) {
            log.e("Failed to cancel repository coroutine scope", e)
        } finally {
            _data.value = null
        }
    }
}