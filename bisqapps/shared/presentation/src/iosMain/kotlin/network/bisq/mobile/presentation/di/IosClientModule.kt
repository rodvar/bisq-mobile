package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.client.user_profile.ClientUserProfileModel
import network.bisq.mobile.client.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.service.UserProfileServiceFacade
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientModule = module {
    single { ApiRequestService(get(), "localhost") }
    single { UserProfileApiGateway(get()) }
    single<UserProfileRepository<ClientUserProfileModel>> { UserProfileRepository() }
    single<ClientUserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) } bind UserProfileServiceFacade::class
}
