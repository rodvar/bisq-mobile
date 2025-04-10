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
package network.bisq.mobile.domain.data.replicated.common.monetary

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.common.roundDouble

@Serializable
@SerialName("Coin")
data class CoinVO(
    override val id: String,
    override val value: Long,
    override val code: String,
    override val precision: Int,
    override val lowPrecision: Int,
) : MonetaryVO {
    override fun round(roundPrecision: Int): MonetaryVO {
        val rounded: Double = roundDouble(toDouble(), roundPrecision);
        val shifted: Long = BigDecimal.fromDouble(rounded).moveDecimalPoint(precision).longValue();
        return CoinVO(code, shifted, code, precision, precision);
    }
}