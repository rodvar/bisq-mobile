package network.bisq.mobile.domain.data.repository

import network.bisq.mobile.domain.data.model.BisqStats
import network.bisq.mobile.domain.data.model.BtcPrice
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade

// this way of definingsupports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class GreetingRepository<T: Greeting>: SingleObjectRepository<T>()
open class BisqStatsRepository: SingleObjectRepository<BisqStats>()
open class BtcPriceRepository: SingleObjectRepository<BtcPrice>()
open class SettingsRepository: SingleObjectRepository<Settings>()
class UserProfileRepository(val model: UserProfileModel, val service: UserProfileServiceFacade)