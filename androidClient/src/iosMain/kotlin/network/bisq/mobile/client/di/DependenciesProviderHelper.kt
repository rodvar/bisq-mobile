package network.bisq.mobile.client.di

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.getOriginalKotlinClass
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.iosClientModule
import network.bisq.mobile.presentation.di.iosPresentationModule
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier

/**
 * Helper for iOS koin injection
 */
class DependenciesProviderHelper {

    fun initKoin() {
        val instance = startKoin {
            modules(
                listOf(
                    domainModule,
                    serviceModule,
                    presentationModule,
                    clientModule,
                    iosClientModule,
                    iosPresentationModule
                )
            )
        }

        koin = instance.koin
    }

    companion object {
        lateinit var koin: Koin
    }

}

@OptIn(BetaInteropApi::class)
fun Koin.get(objCClass: ObjCClass): Any {
    val kClazz = getOriginalKotlinClass(objCClass)!!
    return get(kClazz, null, null)
}

@OptIn(BetaInteropApi::class)
fun Koin.get(objCClass: ObjCClass, qualifier: Qualifier?, parameter: Any): Any {
    val kClazz = getOriginalKotlinClass(objCClass)!!
    return get(kClazz, qualifier) { parametersOf(parameter) }
}
