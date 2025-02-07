package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IGeneralSettingsPresenter : ViewPresenter {
    val i18nPairs: StateFlow<List<Pair<String, String>>>
    val allLanguagePairs: StateFlow<List<Pair<String, String>>>

    val languageCode: StateFlow<String>
    fun setLanguageCode(langCode: String)

    val supportedLanguageCodes: StateFlow<Set<String>>
    fun setSupportedLanguageCodes(langCodes: Set<String>)

    val chatNotification: StateFlow<String>
    fun setChatNotification(value: String)

    val closeOfferWhenTradeTaken: StateFlow<Boolean>
    fun setCloseOfferWhenTradeTaken(value: Boolean)

    val tradePriceTolerance: StateFlow<String>
    fun setTradePriceTolerance(value: String, isValid: Boolean)

    val useAnimations: StateFlow<Boolean>
    fun setUseAnimations(value: Boolean)

    val numDaysAfterRedactingTradeData: StateFlow<Int>

    val powFactor: StateFlow<String>
    fun setPowFactor(value: String, isValid: Boolean)

    val ignorePow: StateFlow<Boolean>
    fun setIgnorePow(value: Boolean)

    val shouldShowPoWAdjustmentFactor: StateFlow<Boolean>
}

@Composable
fun GeneralSettingsScreen(showBackNavigation: Boolean = false) {
    val presenter: IGeneralSettingsPresenter = koinInject()

    val i18nPairs = presenter.i18nPairs.collectAsState().value
    val allLanguagePairs = presenter.allLanguagePairs.collectAsState().value
    val selectedLauguage = presenter.languageCode.collectAsState().value
    val supportedLanguageCodes = presenter.supportedLanguageCodes.collectAsState().value
    val closeOfferWhenTradeTaken = presenter.closeOfferWhenTradeTaken.collectAsState().value
    val tradePriceTolerance = presenter.tradePriceTolerance.collectAsState().value
    val useAnimations = presenter.useAnimations.collectAsState().value
    val powFactor = presenter.powFactor.collectAsState().value
    val ignorePow = presenter.ignorePow.collectAsState().value
    val shouldShowPoWAdjustmentFactor = presenter.shouldShowPoWAdjustmentFactor.collectAsState().value

    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        BisqScrollLayout(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            onModifier = { modifier -> modifier.weight(1f) }
        ) {

            BisqText.h4Regular("settings.language".i18n())

            BisqDropDown(
                label = "settings.language.headline".i18n(),
                items = i18nPairs,
                value = selectedLauguage,
                onValueChanged = { presenter.setLanguageCode(it.first) },
            )

            BisqDropDown(
                label = "${"settings.language.supported.headline".i18n()} (${"settings.language.supported.subHeadLine".i18n()})",
                items = allLanguagePairs,
                value = if (supportedLanguageCodes.isNotEmpty()) supportedLanguageCodes.last() else selectedLauguage,
                values = supportedLanguageCodes,
                onSetChanged = { set ->
                    val codes = set.map { it.first }.toSet()
                    presenter.setSupportedLanguageCodes(codes)
                },
                searchable = true,
                chipMultiSelect = true,
                // chipShowOnlyKey = true
            )

            BisqHDivider()

            BisqText.h4Regular("settings.notification.options".i18n())

            BisqSegmentButton(
                label = "Chat Notification (TODO)", // TODO:i18n
                items = listOf(
                    "chat.notificationsSettingsMenu.all".i18n(),
                    "chat.notificationsSettingsMenu.mention".i18n(),
                    "chat.notificationsSettingsMenu.off".i18n(),
                ),
                onValueChange = { presenter.setChatNotification(it) }
            )

            BisqHDivider()

            BisqText.h4Regular("settings.trade.headline".i18n())

            BisqSwitch(
                label = "settings.trade.closeMyOfferWhenTaken".i18n(),
                checked = closeOfferWhenTradeTaken,
                onSwitch = { presenter.setCloseOfferWhenTradeTaken(it) }
            )

            BisqTextField(
                label = "settings.trade.maxTradePriceDeviation".i18n(),
                value = tradePriceTolerance,
                keyboardType = KeyboardType.Decimal,
                onValueChange = { it, isValid -> presenter.setTradePriceTolerance(it, isValid) },
                helperText = "settings.trade.maxTradePriceDeviation.help".i18n(),
                numberWithTwoDecimals = true,
                valueSuffix = "%",
                validation = {
                    val parsedValue = it.toDoubleOrNull()
                    if (parsedValue == null) {
                        return@BisqTextField "Value cannot be empty"
                    }
                    if (parsedValue < 1 || parsedValue > 10) {
                        return@BisqTextField "settings.trade.maxTradePriceDeviation.invalid".i18n(10)
                    }
                    return@BisqTextField null
                }
            )

            BisqHDivider()

            BisqText.h4Regular("settings.display.headline".i18n())

            BisqSwitch(
                label = "settings.display.useAnimations".i18n(),
                checked = useAnimations,
                onSwitch = { presenter.setUseAnimations(it) }
            )

            if (shouldShowPoWAdjustmentFactor) {
                BisqHDivider()

                BisqText.h4Regular("settings.network.headline".i18n())

                BisqTextField(
                    label = "settings.network.difficultyAdjustmentFactor.description.self".i18n(),
                    value = powFactor.toString(),
                    keyboardType = KeyboardType.Decimal,
                    disabled = !ignorePow,
                    numberWithTwoDecimals = true,
                    onValueChange = { it, isValid -> presenter.setPowFactor(it, isValid) },
                    validation = {
                        val parsedValue = it.toDoubleOrNull()
                        if (parsedValue == null) {
                            return@BisqTextField "Value cannot be empty"
                        }
                        if (parsedValue < 0 || parsedValue > 160_000) {
                            return@BisqTextField "authorizedRole.securityManager.difficultyAdjustment.invalid".i18n(
                                160000
                            )
                        }
                        return@BisqTextField null
                    }
                )
                BisqSwitch(
                    label = "settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n(),
                    checked = ignorePow,
                    onSwitch = { presenter.setIgnorePow(it) }
                )
            }

            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()
            BisqGap.V5()

        }

    }
}