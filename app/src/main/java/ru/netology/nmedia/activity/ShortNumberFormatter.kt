package ru.netology.nmedia.activity

object ShortNumberFormatter {
    fun format(number: Int): String {
        return when {
            number < 1000 -> {
                number.toString()
            }

            number < 10_000 -> {
                val hundreds = number % 1000 / 100
                if (hundreds == 0) {
                    (number / 1000).toString() + "K"
                } else {
                    String.format("%.1fK", number.toDouble() / 1000)
                }
            }

            number < 1_000_000 -> {
                (number / 1000).toString() + "K"
            }

            else -> {
                val hundredsThousands = (number % 1_000_000) / 100_000
                if (hundredsThousands == 0) {
                    (number / 1_000_000).toString() + "M"
                } else {
                    String.format("%.1fM", number.toDouble() / 1_000_000)
                }
            }
        }
    }
}