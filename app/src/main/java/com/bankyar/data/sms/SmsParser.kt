package com.bankyar.data.sms

import com.bankyar.data.database.entities.TransactionType

data class ParsedSmsTransaction(
    val amount: Double,
    val type: TransactionType,
    val cardLastDigits: String?,
    val rawMessage: String,
    val bankName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsParser {

    // Sender ID → bank name. Keys are prefix patterns; add new banks here easily.
    private val BANK_SENDER_PREFIXES: Map<String, String> = mapOf(
        "200"      to "بانک ملی",
        "201"      to "بانک ملی",
        "202"      to "بانک ملی",
        "203"      to "بانک ملی",
        "30005"    to "بانک ملت",
        "5000030"  to "بانک ملت",
        "8009"     to "بانک سامان",
        "10008009" to "بانک سامان",
        "10002020" to "بانک پاسارگاد",
        "2020"     to "بانک پاسارگاد",
        "100026"   to "بانک سپه",
        "1000260"  to "بانک سپه",
        "50002"    to "بانک تجارت",
        "5000240"  to "بانک صادرات",
        "10006"    to "بانک رفاه",
        "2010"     to "بانک کشاورزی",
        "50004"    to "بانک مسکن",
        "50001"    to "بانک صنعت و معدن",
    )

    // Regexes for amount extraction — supports both Persian and Latin digits, with or without commas
    private val AMOUNT_REGEXES = listOf(
        Regex("""مبلغ\s*:?\s*([\d,،]+)"""),
        Regex("""([\d,،]+)\s*ریال"""),
        Regex("""([\d,،]+)\s*تومان"""),
        Regex("""([\d,،]+)\s*Rial""", RegexOption.IGNORE_CASE),
        Regex("""برداشت\s+([\d,،]+)"""),
        Regex("""واریز\s+([\d,،]+)"""),
        Regex("""خرید\s+([\d,،]+)"""),
    )

    // Keywords that indicate expense
    private val EXPENSE_KEYWORDS = listOf(
        "برداشت", "خرید", "پرداخت", "کسر", "کسرشد", "بدهکار"
    )

    // Keywords that indicate income
    private val INCOME_KEYWORDS = listOf(
        "واریز", "افزایش موجودی", "دریافت", "بستانکار", "کارت به کارت", "انتقال به"
    )

    // Last 4 digits of card — common patterns: ****1234, XXXX-1234, card ending 1234
    private val CARD_DIGITS_REGEX = Regex("""(?:\*{4}[-\s]?|کارت\s*)(\d{4})\b""")

    fun parse(sender: String, body: String): ParsedSmsTransaction? {
        val bankName = resolveBankName(sender) ?: return null

        val amount = extractAmount(body) ?: return null
        val type = classifyType(body)
        val cardDigits = CARD_DIGITS_REGEX.find(body)?.groupValues?.getOrNull(1)

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            cardLastDigits = cardDigits,
            rawMessage = body,
            bankName = bankName
        )
    }

    private fun resolveBankName(sender: String): String? {
        val normalized = sender.trim()
        for ((prefix, name) in BANK_SENDER_PREFIXES) {
            if (normalized.startsWith(prefix)) return name
        }
        return null
    }

    private fun extractAmount(body: String): Double? {
        for (regex in AMOUNT_REGEXES) {
            val match = regex.find(body) ?: continue
            val raw = match.groupValues[1]
                .replace(",", "")
                .replace("،", "")
                .trim()
            val value = raw.toDoubleOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }

    private fun classifyType(body: String): TransactionType {
        val lower = body
        for (kw in INCOME_KEYWORDS) {
            if (kw in lower) return TransactionType.INCOME
        }
        for (kw in EXPENSE_KEYWORDS) {
            if (kw in lower) return TransactionType.EXPENSE
        }
        return TransactionType.EXPENSE
    }
}
