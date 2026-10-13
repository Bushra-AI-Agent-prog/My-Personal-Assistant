package com.example.action.executor

import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

object CalculatorEngine {

    fun evaluate(query: String): Result<String> {
        val clean = query.trim().lowercase(Locale.US)

        // 1. Check unit conversions
        val conversionResult = tryConvertUnits(clean)
        if (conversionResult != null) {
            return Result.success(conversionResult)
        }

        // 2. Percentage calculation like "15% of 80" or "80 * 15%"
        val percentOfMatch = Regex("""([\d.]+)\s*%\s*of\s*([\d.]+)""").find(clean)
        if (percentOfMatch != null) {
            val p = percentOfMatch.groupValues[1].toDoubleOrNull()
            val total = percentOfMatch.groupValues[2].toDoubleOrNull()
            if (p != null && total != null) {
                val res = (p / 100.0) * total
                return Result.success(formatNumber(res))
            }
        }

        // 3. Mathematical expression evaluation
        return try {
            val expr = clean
                .replace("x", "*")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("^", " ** ")
                .replace("plus", "+")
                .replace("minus", "-")
                .replace("times", "*")
                .replace("multiplied by", "*")
                .replace("divided by", "/")

            val tokens = tokenize(expr)
            if (tokens.isEmpty()) {
                return Result.failure(IllegalArgumentException("Empty mathematical expression"))
            }
            val result = parseExpression(tokens)
            Result.success(formatNumber(result))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not evaluate expression: ${e.message}"))
        }
    }

    private fun tryConvertUnits(input: String): String? {
        // Temperature: "25 c in f" or "77 f in c" or "25 celsius to fahrenheit"
        val tempPattern = Regex("""(-?\d+(?:\.\d+)?)\s*(c|f|celsius|fahrenheit)\s*(?:to|in)\s*(c|f|celsius|fahrenheit)""")
        val tempMatch = tempPattern.find(input)
        if (tempMatch != null) {
            val value = tempMatch.groupValues[1].toDoubleOrNull() ?: return null
            val from = tempMatch.groupValues[2]
            val to = tempMatch.groupValues[3]
            return if (from.startsWith("c") && to.startsWith("f")) {
                val f = (value * 9.0 / 5.0) + 32.0
                "${formatNumber(value)}°C = ${formatNumber(f)}°F"
            } else if (from.startsWith("f") && to.startsWith("c")) {
                val c = (value - 32.0) * 5.0 / 9.0
                "${formatNumber(value)}°F = ${formatNumber(c)}°C"
            } else null
        }

        // Distance & Length: km to miles, miles to km, meters to feet, cm to inches
        val distPattern = Regex("""(\d+(?:\.\d+)?)\s*(km|kilometer|kilometers|miles|mile|mi|meters|meter|m|feet|foot|ft|cm|inches|inch|in)\s*(?:to|in)\s*(km|kilometer|kilometers|miles|mile|mi|meters|meter|m|feet|foot|ft|cm|inches|inch|in)""")
        val distMatch = distPattern.find(input)
        if (distMatch != null) {
            val v = distMatch.groupValues[1].toDoubleOrNull() ?: return null
            val from = distMatch.groupValues[2]
            val to = distMatch.groupValues[3]

            // Convert 'from' to meters
            val meters = when {
                from.startsWith("km") || from.startsWith("kilo") -> v * 1000.0
                from == "mi" || from.startsWith("mile") -> v * 1609.344
                from == "m" || from.startsWith("meter") -> v
                from == "ft" || from.startsWith("foot") || from.startsWith("feet") -> v * 0.3048
                from == "cm" -> v * 0.01
                from == "in" || from.startsWith("inch") -> v * 0.0254
                else -> return null
            }

            // Convert meters to 'to'
            val converted = when {
                to.startsWith("km") || to.startsWith("kilo") -> meters / 1000.0
                to == "mi" || to.startsWith("mile") -> meters / 1609.344
                to == "m" || to.startsWith("meter") -> meters
                to == "ft" || to.startsWith("foot") || to.startsWith("feet") -> meters / 0.3048
                to == "cm" -> meters / 0.01
                to == "in" || to.startsWith("inch") -> meters / 0.0254
                else -> return null
            }
            return "${formatNumber(v)} $from = ${formatNumber(converted)} $to"
        }

        // Weight: kg to lbs, lbs to kg, grams to ounces
        val weightPattern = Regex("""(\d+(?:\.\d+)?)\s*(kg|kilogram|kilograms|lbs|lb|pounds|pound|grams|gram|g|oz|ounces|ounce)\s*(?:to|in)\s*(kg|kilogram|kilograms|lbs|lb|pounds|pound|grams|gram|g|oz|ounces|ounce)""")
        val weightMatch = weightPattern.find(input)
        if (weightMatch != null) {
            val v = weightMatch.groupValues[1].toDoubleOrNull() ?: return null
            val from = weightMatch.groupValues[2]
            val to = weightMatch.groupValues[3]

            val grams = when {
                from.startsWith("kg") || from.startsWith("kilo") -> v * 1000.0
                from.startsWith("lb") || from.startsWith("pound") -> v * 453.59237
                from == "g" || from.startsWith("gram") -> v
                from.startsWith("oz") || from.startsWith("ounce") -> v * 28.3495
                else -> return null
            }

            val converted = when {
                to.startsWith("kg") || to.startsWith("kilo") -> grams / 1000.0
                to.startsWith("lb") || to.startsWith("pound") -> grams / 453.59237
                to == "g" || to.startsWith("gram") -> grams
                to.startsWith("oz") || to.startsWith("ounce") -> grams / 28.3495
                else -> return null
            }
            return "${formatNumber(v)} $from = ${formatNumber(converted)} $to"
        }

        return null
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val regex = Regex("""\d+(?:\.\d+)?|[+\-*/()]|\*\*|sqrt""")
        for (m in regex.findAll(expr)) {
            tokens.add(m.value)
        }
        return tokens
    }

    private class ExpressionParser(private val tokens: List<String>) {
        private var pos = 0
        private fun peek(): String? = if (pos < tokens.size) tokens[pos] else null
        private fun next(): String = tokens[pos++]

        fun parse(): Double = parseAddSub()

        private fun parseAddSub(): Double {
            var left = parseMulDiv()
            while (peek() == "+" || peek() == "-") {
                val op = next()
                val right = parseMulDiv()
                left = if (op == "+") left + right else left - right
            }
            return left
        }

        private fun parseMulDiv(): Double {
            var left = parsePower()
            while (peek() == "*" || peek() == "/") {
                val op = next()
                val right = parsePower()
                left = if (op == "*") left * right else {
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    left / right
                }
            }
            return left
        }

        private fun parsePower(): Double {
            var left = parseFactor()
            while (peek() == "**") {
                next()
                val right = parseFactor()
                left = left.pow(right)
            }
            return left
        }

        private fun parseFactor(): Double {
            val token = next()
            return when {
                token == "(" -> {
                    val result = parseAddSub()
                    if (next() != ")") throw IllegalArgumentException("Missing closing parenthesis")
                    result
                }
                token == "sqrt" -> sqrt(parseFactor())
                token == "-" -> -parseFactor()
                token == "+" -> parseFactor()
                else -> token.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $token")
            }
        }
    }

    private fun parseExpression(tokens: List<String>): Double {
        return ExpressionParser(tokens).parse()
    }

    private fun formatNumber(num: Double): String {
        return if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", num).trimEnd('0').trimEnd('.')
        }
    }
}
