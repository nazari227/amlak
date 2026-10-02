package com.example.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PersianUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String?): String {
        if (input == null) return ""
        val builder = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun toPersianDigits(number: Long): String {
        return toPersianDigits(number.toString())
    }

    fun toPersianDigits(number: Int): String {
        return toPersianDigits(number.toString())
    }

    fun formatPrice(amountToman: Long): String {
        if (amountToman <= 0) return "توافقی"
        return when {
            amountToman >= 1_000_000_000 -> {
                val billion = amountToman.toDouble() / 1_000_000_000.0
                val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
                "${toPersianDigits(df.format(billion))} میلیارد تومان"
            }
            amountToman >= 1_000_000 -> {
                val million = amountToman.toDouble() / 1_000_000.0
                val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
                "${toPersianDigits(df.format(million))} میلیون تومان"
            }
            else -> {
                val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
                "${toPersianDigits(df.format(amountToman))} تومان"
            }
        }
    }

    fun formatNumberWithCommas(number: Long): String {
        val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        return toPersianDigits(df.format(number))
    }

    fun formatArea(areaSquareMeters: Double): String {
        val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
        return "${toPersianDigits(df.format(areaSquareMeters))} متر مربع"
    }

    fun sanitizePhoneNumber(raw: String): String {
        val english = raw.map { ch ->
            when (ch) {
                in '۰'..'۹' -> ('0' + (ch - '۰'))
                in '٠'..'٩' -> ('0' + (ch - '٠'))
                else -> ch
            }
        }.joinToString("").filter { it.isDigit() || it == '+' }

        return when {
            english.startsWith("+98") -> "0" + english.removePrefix("+98")
            english.startsWith("0098") -> "0" + english.removePrefix("0098")
            else -> english
        }
    }
}
