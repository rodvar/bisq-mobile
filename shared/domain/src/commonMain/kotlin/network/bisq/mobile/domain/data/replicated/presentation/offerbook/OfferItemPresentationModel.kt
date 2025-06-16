/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.domain.data.replicated.presentation.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

/**
 * This model is used in the UI and will get the mutual fields updated from domain services.
 */
class OfferItemPresentationModel(offerItemPresentationDto: OfferItemPresentationDto) {
    // Delegates
    val bisqEasyOffer = offerItemPresentationDto.bisqEasyOffer
    val isMyOffer = offerItemPresentationDto.isMyOffer
    val makersUserProfile = offerItemPresentationDto.userProfile
    val formattedDate = offerItemPresentationDto.formattedDate
    var formattedQuoteAmount = offerItemPresentationDto.formattedQuoteAmount
    var formattedPriceSpec = offerItemPresentationDto.formattedPriceSpec
    val quoteSidePaymentMethods = offerItemPresentationDto.quoteSidePaymentMethods
    val baseSidePaymentMethods = offerItemPresentationDto.baseSidePaymentMethods

    // convenience fields
    val offerId = bisqEasyOffer.id

    // In case of market price or float is used, the price and the base amount need to be updated at market price updates.
    private val _formattedPrice = MutableStateFlow(offerItemPresentationDto.formattedPrice)
    val formattedPrice: StateFlow<String> get() = _formattedPrice

    private val _formattedBaseAmount = MutableStateFlow(offerItemPresentationDto.formattedQuoteAmount)
    val formattedBaseAmount: StateFlow<String> get() = _formattedBaseAmount

    // The user name is the nickname and the nym in case there are multiple nicknames in the network.
    // We get that set by the backend in the makersUserProfile but we need to update it after initial retrieval by websocket events.
    // At Bisq 2 the UserNameLookup class handles that.
    private val _userName = MutableStateFlow(offerItemPresentationDto.userProfile.userName)
    val userName: StateFlow<String> get() = _userName

    private val _makersReputationScore = MutableStateFlow(offerItemPresentationDto.reputationScore)
    val makersReputationScore: StateFlow<ReputationScoreVO> get() = _makersReputationScore

    var isInvalidDueToReputation: Boolean = false

    fun copyWith(
        formattedQuoteAmount: String = this.formattedQuoteAmount,
        formattedPriceSpec: String = this.formattedPriceSpec,
        isInvalidDueToReputation: Boolean = this.isInvalidDueToReputation,
        formattedBaseAmount: String = this.formattedBaseAmount.value,
        formattedPrice: String = this.formattedPrice.value,
        userName: String = this.userName.value,
        makersReputationScore: ReputationScoreVO = this.makersReputationScore.value
    ): OfferItemPresentationModel {
        val dto = OfferItemPresentationDto(
            bisqEasyOffer = this.bisqEasyOffer,
            isMyOffer = this.isMyOffer,
            userProfile = this.makersUserProfile.copy(userName = userName),
            formattedDate = this.formattedDate,
            formattedQuoteAmount = formattedQuoteAmount,
            formattedPriceSpec = formattedPriceSpec,
            quoteSidePaymentMethods = this.quoteSidePaymentMethods,
            baseSidePaymentMethods = this.baseSidePaymentMethods,
            formattedPrice = formattedPrice,
            reputationScore = makersReputationScore,
            formattedBaseAmount = formattedBaseAmount,
        )
        return OfferItemPresentationModel(dto).also {
            it.formattedQuoteAmount = formattedQuoteAmount
            it.formattedPriceSpec = formattedPriceSpec
            it.isInvalidDueToReputation = isInvalidDueToReputation
        }
    }


}
