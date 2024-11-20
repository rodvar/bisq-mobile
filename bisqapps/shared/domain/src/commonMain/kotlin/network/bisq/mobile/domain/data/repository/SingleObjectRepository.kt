package network.bisq.mobile.domain.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.persistance.PersistenceSource

/**
 * Repository implementation for a single object. Allows for persistance if the persistance source if provided, otherwise is mem-only.
 * This provide the object/level coroutines observer/observable pattern on the WHOLE data obj.
 * Please inherit and add field level observer patter if you need.
 *
 * TODO: create a map-based multi object repository when needed (might need to leverage some kind of id generation on the base model)
 */
abstract class SingleObjectRepository<out T : BaseModel>(
    private val persistenceSource: PersistenceSource<T>? = null
) : BaseRepository<T>() {

    protected val logger = Logger.withTag(this::class.simpleName ?: "SingleObjectRepository")

    private val _data = MutableStateFlow<T?>(null)
    final override val data: StateFlow<T?> = _data

    private val job = Job()
    private val scope = CoroutineScope(job + BackgroundDispatcher)

    init {
        // Load from persistence on initialization if available
        persistenceSource?.let {
            scope.launch {
                _data.value = it.get()
            }
        }
    }

    /**
     * non-IO blocking get current cached data
     */
    protected fun currentData(): T? {
        return _data.value
    }
    /**
     * non-IO blocking update cache
     */
    protected fun updateData(value: @UnsafeVariance T?) {
        _data.value = value
    }

    override suspend fun fetch(): T? {
        return _data.value ?: persistenceSource?.get().also { _data.value = it }
    }

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

    override suspend fun clear() {
        try {
            persistenceSource?.clear()
            scope.cancel()
        } catch (e: Exception) {
            logger.e("Failed to cancel repository coroutine scope", e)
        } finally {
            _data.value = null
        }
    }
}