package kz.mybrain.superkassa.domain.kassa.model

/**
 * Тот же итог с преобразованным значением.
 *
 * Отказ и сбой проходят как есть: преобразовывать в них нечего, а слова
 * кассы и код остаются теми, что она сказала.
 */
inline fun <T, R> Answer<T>.map(transform: (T) -> R): Answer<R> = when (this) {
    is Answer.Done -> Answer.Done(transform(value))
    is Answer.Refused -> this
    is Answer.Failed -> this
}
