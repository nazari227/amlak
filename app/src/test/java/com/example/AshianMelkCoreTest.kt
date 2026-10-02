package com.example

import com.example.core.util.PersianUtils
import com.example.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AshianMelkCoreTest {

    @Test
    fun testPersianDigitConversion() {
        assertEquals("۱۲۳۴۵۶۷۸۹۰", PersianUtils.toPersianDigits("1234567890"))
        assertEquals("کد ملک: ۱۲۳۴", PersianUtils.toPersianDigits("کد ملک: 1234"))
    }

    @Test
    fun testPersianPriceFormatting() {
        // 38.5 Billion Toman
        val price1 = 38_500_000_000L
        val formatted1 = PersianUtils.formatPrice(price1)
        assertTrue(formatted1.contains("میلیارد تومان"))

        // 85 Million Toman
        val price2 = 85_000_000L
        val formatted2 = PersianUtils.formatPrice(price2)
        assertTrue(formatted2.contains("میلیون تومان"))

        // Negotiable / 0 Toman
        assertEquals("توافقی", PersianUtils.formatPrice(0))
    }

    @Test
    fun testPhoneSanitization() {
        val raw1 = "+989123456789"
        assertEquals("09123456789", PersianUtils.sanitizePhoneNumber(raw1))

        val rawPersian = "۰۹۱۲۳۴۵۶۷۸۹"
        assertEquals("09123456789", PersianUtils.sanitizePhoneNumber(rawPersian))
    }

    @Test
    fun testSha256ChecksumCalculation() {
        val testData = "AshianMelk-IranAmlak-Enterprise".toByteArray(Charsets.UTF_8)
        val hash = SecurityUtils.calculateSha256(testData)
        assertEquals(64, hash.length) // 256 bits = 64 hex characters
    }

    @Test
    fun testMaskSensitiveData() {
        val maskedPhone = SecurityUtils.maskPhoneNumber("09123456789")
        assertEquals("0912***789", maskedPhone)

        val maskedToken = SecurityUtils.maskToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
        assertTrue(maskedToken.startsWith("eyJh..."))
    }
}
