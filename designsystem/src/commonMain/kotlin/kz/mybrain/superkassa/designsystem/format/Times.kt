package kz.mybrain.superkassa.designsystem.format

import kotlin.time.Duration

/**
 * Часы и минуты без даты: час графика и остаток ожидания.
 *
 * Вид цифровой и одинаковый на трёх языках, как и у [Dates].
 */
object Times {

    /** Час целиком: «14:00», «09:00». */
    fun hour(hour: Int): String = "${twoDigits(hour.toLong())}:00"

    /**
     * Остаток времени минутами и секундами: «2:58».
     *
     * Не «178 секунд»: столько владелец переводит в минуты сам, а смотрит он
     * на это между нажатием и окном подписи.
     */
    fun countdown(left: Duration): String =
        left.toComponents { minutes, seconds, _ -> "$minutes:${twoDigits(seconds.toLong())}" }

    private fun twoDigits(value: Long): String = value.toString().padStart(2, '0')
}
