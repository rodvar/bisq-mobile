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
package network.bisq.mobile.domain.data.replicated.common.currency

import kotlinx.serialization.Serializable

@Serializable
data class MarketVO(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val baseCurrencyName: String = baseCurrencyCode,
    val quoteCurrencyName: String = quoteCurrencyCode
)

val marketListDemoObj = listOf(MarketVO("BTC", "USD"),
    MarketVO("BTC", "EUR"),
    MarketVO("BTC", "ARS"),
    MarketVO("BTC", "PYG"),
    MarketVO("BTC", "LBP"),
    MarketVO("BTC", "CZK"),
    MarketVO("BTC", "AUD"),
    MarketVO("BTC", "CAD"),
    MarketVO("BTC", "IDR"))
