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

    // Numeric prefix → bank name (match start of sender)
    private val NUMERIC_PREFIXES: Map<String, String> = mapOf(
        "200" to "بانک ملی", "201" to "بانک ملی", "202" to "بانک ملی", "203" to "بانک ملی",
        "30005" to "بانک ملت", "5000030" to "بانک ملت",
        "8009" to "بانک سامان", "10008009" to "بانک سامان",
        "10002020" to "بانک پاسارگاد", "2020" to "بانک پاسارگاد",
        "100026" to "بانک سپه", "1000260" to "بانک سپه",
        "50002" to "بانک تجارت", "5000240" to "بانک صادرات",
        "10006" to "بانک رفاه", "2010" to "بانک کشاورزی",
        "50004" to "بانک مسکن", "50001" to "بانک صنعت و معدن",
        "3000460" to "بانک ایران زمین", "10001" to "بانک توسعه صادرات",
    )

    // Alphanumeric / short-name sender → bank name (case-insensitive substring match)
    private val ALPHA_KEYWORDS: Map<String, String> = mapOf(
        "mellat" to "بانک ملت", "melat" to "بانک ملت", "بانک ملت" to "بانک ملت",
        "melli" to "بانک ملی", "بانک ملی" to "بانک ملی", "bankmelli" to "بانک ملی",
        "saman" to "بانک سامان", "بانک سامان" to "بانک سامان",
        "pasargad" to "بانک پاسارگاد", "بانک پاسارگاد" to "بانک پاسارگاد",
        "sepah" to "بانک سپه", "بانک سپه" to "بانک سپه",
        "tejarat" to "بانک تجارت", "بانک تجارت" to "بانک تجارت",
        "saderat" to "بانک صادرات", "بانک صادرات" to "بانک صادرات",
        "refah" to "بانک رفاه", "بانک رفاه" to "بانک رفاه",
        "keshavarzi" to "بانک کشاورزی", "agri" to "بانک کشاورزی", "بانک کشاورزی" to "بانک کشاورزی",
        "maskan" to "بانک مسکن", "بانک مسکن" to "بانک مسکن",
        "eghtesad" to "بانک اقتصاد نوین", "novin" to "بانک اقتصاد نوین",
        "parsian" to "بانک پارسیان", "بانک پارسیان" to "بانک پارسیان",
        "ansar" to "بانک انصار", "بانک انصار" to "بانک انصار",
        "iranzamin" to "بانک ایران زمین",
        "ayandeh" to "بانک آینده", "بانک آینده" to "بانک آینده",
        "dey" to "بانک دی", "بانک دی" to "بانک دی",
        "sina" to "بانک سینا", "بانک سینا" to "بانک سینا",
        "karafarin" to "بانک کارآفرین",
        "shahr" to "بانک شهر", "بانک شهر" to "بانک شهر",
        "resalat" to "بانک رسالت", "بانک رسالت" to "بانک رسالت",
        "mehriran" to "بانک مهر ایران",
    )

    // Banking body keywords that must appear for fallback parsing
    private val BODY_BANK_KEYWORDS = listOf(
        "واریز", "برداشت", "خرید", "پرداخت", "موجودی", "ریال", "تومان",
        "کارت", "حساب", "بانک", "شبا", "کسر", "بدهکار", "بستانکار"
    )

    private val AMOUNT_REGEXES = listOf(
        Regex("""مبلغ\s*[:\-]?\s*([\d,،٬]+)"""),
        Regex("""([\d,،٬]+)\s*ریال"""),
        Regex("""([\d,،٬]+)\s*تومان"""),
        Regex("""([\d,،٬]+)\s*Rial""", RegexOption.IGNORE_CASE),
        Regex("""برداشت\s+([\d,،٬]+)"""),
        Regex("""واریز\s+([\d,،٬]+)"""),
        Regex("""خرید\s+([\d,،٬]+)"""),
        Regex("""پرداخت\s+([\d,،٬]+)"""),
        Regex("""مبلغ\s*([\d,،٬]+)"""),
    )

    private val EXPENSE_KEYWORDS = listOf(
        "برداشت", "خرید", "پرداخت", "کسر شد", "کسرشد", "بدهکار",
        "انتقال از", "purchase", "withdrawal"
    )
    private val INCOME_KEYWORDS = listOf(
        "واریز", "افزایش موجودی", "دریافت", "بستانکار",
        "کارت به کارت", "انتقال به", "deposit", "credit"
    )

    private val CARD_DIGITS_REGEXES = listOf(
        Regex("""[\*X]{4}[-\s]?(\d{4})\b"""),
        Regex("""کارت\s*(?:شماره\s*)?(?:[\*\-\s]*)?(\d{4})\b"""),
        Regex("""(\d{4})[\*X]{8}(\d{4})"""),   // partial: keep last 4
    )

    fun parse(sender: String, body: String): ParsedSmsTransaction? {
        val bankName = resolveBankName(sender)
            ?: if (looksLikeBankSms(body)) "بانک" else return null

        val amount = extractAmount(body) ?: return null
        val type = classifyType(body)
        val cardDigits = extractCardDigits(body)

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            cardLastDigits = cardDigits,
            rawMessage = body,
            bankName = bankName
        )
    }

    private fun resolveBankName(sender: String): String? {
        val s = sender.trim()
        // 1. Numeric prefix match
        for ((prefix, name) in NUMERIC_PREFIXES) {
            if (s.startsWith(prefix)) return name
        }
        // 2. Alphanumeric keyword match (case-insensitive)
        val lower = s.lowercase()
        for ((kw, name) in ALPHA_KEYWORDS) {
            if (lower.contains(kw.lowercase())) return name
        }
        return null
    }

    private fun looksLikeBankSms(body: String): Boolean {
        val count = BODY_BANK_KEYWORDS.count { it in body }
        return count >= 2
    }

    private fun extractAmount(body: String): Double? {
        for (regex in AMOUNT_REGEXES) {
            val match = regex.find(body) ?: continue
            val raw = match.groupValues[1]
                .replace(",", "").replace("،", "").replace("٬", "").trim()
            val value = raw.toDoubleOrNull() ?: continue
            if (value >= 1000) return value   // amounts below 1000 are likely noise
        }
        return null
    }

    private fun extractCardDigits(body: String): String? {
        for (regex in CARD_DIGITS_REGEXES) {
            val match = regex.find(body) ?: continue
            // For the 3rd regex (full card), use last group
            val groups = match.groupValues
            val digits = groups.lastOrNull { it.length == 4 } ?: continue
            return digits
        }
        return null
    }

    private fun classifyType(body: String): TransactionType {
        for (kw in INCOME_KEYWORDS) { if (kw in body) return TransactionType.INCOME }
        for (kw in EXPENSE_KEYWORDS) { if (kw in body) return TransactionType.EXPENSE }
        return TransactionType.EXPENSE
    }
}
