package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.data.repository.NodeGreetingRepository
import network.bisq.mobile.android.node.domain.user_profile.NodeUserProfileModel
import network.bisq.mobile.android.node.domain.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.android.node.presentation.MainNodePresenter
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    // this one is for example properties, will be eliminated soon
    single<NodeGreetingRepository> { NodeGreetingRepository() }

    single { AndroidApplicationService.Supplier() }

    // this line showcases both, the possibility to change behaviour of the app by changing one definition
    // and binding the same obj to 2 different abstractions
    single<MainPresenter> { MainNodePresenter(get(), get()) } bind AppPresenter::class

    single<UserProfileModel> { NodeUserProfileModel() }
    single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get(), get()) }

    // Services
//    TODO might not work because of the jars dependencies, needs more work
//    single <AndroidMemoryReportService> {
//        val context = androidContext()
//        AndroidMemoryReportService(context)
//    }
//    single <AndroidApplicationService> {
//        val filesDirsPath = androidContext().filesDir.toPath()
//        val androidMemoryService: AndroidMemoryReportService = get()
//        AndroidApplicationService(androidMemoryService, filesDirsPath)
//    }
//    single <UserIdentityService> {
//        val applicationService: AndroidApplicationService = get()
//        applicationService.userService.userIdentityService
//    }
//    single <SecurityService> {
//        val applicationService: AndroidApplicationService = get()
//        applicationService.securityService
//    }
}