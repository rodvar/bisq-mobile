package network.bisq.mobile.client.di

import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.client.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.user_profile.ClientUserProfileModel
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.domain.service.UserProfileServiceFacade
import org.koin.dsl.bind
import org.koin.dsl.module

val androidClientModule = module {
    single<ApiRequestService> { ApiRequestService(get(), "10.0.2.2") }
    single<UserProfileApiGateway> { UserProfileApiGateway(get()) }
    single<UserProfileRepository<ClientUserProfileModel>> { UserProfileRepository() }
    single<ClientUserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }  bind  UserProfileServiceFacade::class

}
