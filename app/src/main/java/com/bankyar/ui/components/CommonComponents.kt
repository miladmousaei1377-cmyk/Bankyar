package com.bankyar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bankyar.ui.theme.GradientEnd
import com.bankyar.ui.theme.GradientStart
import java.text.DecimalFormat

fun formatAmount(amount: Double): String = DecimalFormat("#,###").format(amount)

private fun formatDigitsWithCommas(digits: String): String {
    if (digits.isEmpty()) return ""
    val sb = StringBuilder()
    val len = digits.length
    digits.forEachIndexed { i, c ->
        val remaining = len - i - 1
        sb.append(c)
        if (remaining > 0 && remaining % 3 == 0) sb.append(',')
    }
    return sb.toString()
}

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val formatted = formatDigitsWithCommas(digits)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (formatted.isEmpty()) return 0
                var digitsSeen = 0
                formatted.forEachIndexed { i, c ->
                    if (digitsSeen == offset) return i
                    if (c.isDigit()) digitsSeen++
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                var digitCount = 0
                for (i in 0 until minOf(offset, formatted.length)) {
                    if (formatted[i].isDigit()) digitCount++
                }
                return digitCount
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun GradientCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground, modifier = modifier)
}

@Composable
fun LoadingOverlay() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
