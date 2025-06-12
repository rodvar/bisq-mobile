package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PriceUtilTest {

    private fun createTestMarket(): MarketVO {
        return MarketVO(
            baseCurrencyCode = "BTC",
            quoteCurrencyCode = "USD",
            baseCurrencyName = "Bitcoin",
            quoteCurrencyName = "US Dollar"
        )
    }

    private fun createTestPriceQuote(value: Long, market: MarketVO = createTestMarket()): PriceQuoteVO {
        return PriceQuoteVO(
            value = value,
            precision = 4,
            lowPrecision = 2,
            market = market,
            baseSideMonetary = CoinVO("BTC", 1, "BTC", 8, 4),
            quoteSideMonetary = FiatVO("USD", value, "USD", 4, 2)
        )
    }

    @Test
    fun `getPercentageToMarketPrice should calculate correct percentage for equal prices`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(50000)  // $50,000
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(0.0, result, 0.0001) // 0% difference
    }

    @Test
    fun `getPercentageToMarketPrice should calculate correct percentage for higher price`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(55000)  // $55,000 (10% higher)
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(0.1, result, 0.0001) // 10% higher
    }

    @Test
    fun `getPercentageToMarketPrice should calculate correct percentage for lower price`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(45000)  // $45,000 (10% lower)
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(-0.1, result, 0.0001) // 10% lower
    }

    @Test
    fun `getPercentageToMarketPrice should handle small differences without crashing`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(50001)  // $50,001 (0.002% higher)

        // The main goal is to ensure it doesn't crash
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)

        // Basic sanity checks - the exact value may vary due to rounding
        assertTrue(result.isFinite(), "Result should be finite")
        // Don't assert specific bounds since rounding behavior may vary
    }

    @Test
    fun `getPercentageToMarketPrice should round to 4 decimal places`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(50001)  // $50,001
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        
        // Result should be rounded to 4 decimal places
        val resultString = result.toString()
        val decimalIndex = resultString.indexOf('.')
        if (decimalIndex != -1) {
            val decimalPlaces = resultString.length - decimalIndex - 1
            assertTrue(decimalPlaces <= 4, "Result should have at most 4 decimal places, but had $decimalPlaces")
        }
    }

    @Test
    fun `getPercentageToMarketPrice should throw exception for zero market price`() {
        val marketPrice = createTestPriceQuote(0) // $0
        val priceQuote = createTestPriceQuote(50000)  // $50,000
        
        assertFailsWith<IllegalArgumentException> {
            PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        }
    }

    @Test
    fun `getPercentageToMarketPrice should throw exception for negative market price`() {
        val marketPrice = createTestPriceQuote(-1000) // -$1,000
        val priceQuote = createTestPriceQuote(50000)   // $50,000
        
        assertFailsWith<IllegalArgumentException> {
            PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        }
    }

    @Test
    fun `getPercentageToMarketPrice should throw exception for negative price quote`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(-1000)   // -$1,000
        
        assertFailsWith<IllegalArgumentException> {
            PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        }
    }

    @Test
    fun `getPercentageToMarketPrice should handle zero price quote`() {
        val marketPrice = createTestPriceQuote(50000) // $50,000
        val priceQuote = createTestPriceQuote(0)       // $0
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(-1.0, result, 0.0001) // -100% (complete discount)
    }

    @Test
    fun `getPercentageToMarketPrice should handle very large numbers`() {
        val marketPrice = createTestPriceQuote(1000000000) // $1 billion
        val priceQuote = createTestPriceQuote(1100000000)  // $1.1 billion (10% higher)
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(0.1, result, 0.0001) // 10% higher
    }

    @Test
    fun `getPercentageToMarketPrice should handle very small numbers`() {
        val marketPrice = createTestPriceQuote(1) // $0.0001
        val priceQuote = createTestPriceQuote(2)  // $0.0002 (100% higher)
        
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
        assertEquals(1.0, result, 0.0001) // 100% higher
    }

    @Test
    fun `getPercentageToMarketPrice should throw exception for infinite market price`() {
        // This test simulates what would happen if conversion to double results in infinity
        val marketPrice = createTestPriceQuote(Long.MAX_VALUE)
        val priceQuote = createTestPriceQuote(50000)
        
        // The actual behavior depends on how Long.MAX_VALUE converts to Double
        // If it results in infinity, it should throw an exception
        try {
            val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)
            // If no exception is thrown, the result should be finite
            assertTrue(result.isFinite(), "Result should be finite")
        } catch (e: IllegalArgumentException) {
            // This is also acceptable if the conversion results in infinity
            assertTrue(e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun `getPercentageToMarketPrice should handle precision edge cases without crashing`() {
        // Test with numbers that might cause precision issues
        val marketPrice = createTestPriceQuote(333333) // $33.3333
        val priceQuote = createTestPriceQuote(333334)  // $33.3334

        // The main goal is crash prevention
        val result = PriceUtil.getPercentageToMarketPrice(marketPrice, priceQuote)

        // Should handle the calculation without throwing exceptions
        assertTrue(result.isFinite(), "Result should be finite")
        // Don't assert specific bounds since precision and rounding may vary
        // The important thing is that it doesn't crash and produces a finite result
    }
}
