package kz.mybrain.superkassa.domain.print.model

/**
 * В каком виде сохраняется печатная форма.
 *
 * PDF уходит покупателю, HTML — в бухгалтерию, картинка повторяет экран.
 */
enum class PrintKind(val extension: String) {
    Png("png"),
    Pdf("pdf"),
    Html("html")
}
