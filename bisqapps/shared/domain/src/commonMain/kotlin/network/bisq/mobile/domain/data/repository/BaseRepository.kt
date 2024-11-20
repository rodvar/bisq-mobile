package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.BaseModel

/**
 *
 */
abstract class BaseRepository<out T: BaseModel>: Repository<T> {

}