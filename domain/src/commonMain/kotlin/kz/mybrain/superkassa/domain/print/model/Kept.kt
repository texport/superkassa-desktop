package kz.mybrain.superkassa.domain.print.model

/** Чем кончилось сохранение формы в файл. */
sealed interface Kept {

    /** Сохранена в файл [name]. */
    data class Saved(val name: String) : Kept

    /** Владелец закрыл окно выбора файла: он передумал, говорить не о чем. */
    data object Cancelled : Kept

    /** На этом устройстве сохранять в файл пока некуда: так и сказать, а не промолчать. */
    data object Unavailable : Kept
}
