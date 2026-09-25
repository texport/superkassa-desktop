package kz.mybrain.superkassa.presentation.setup

/**
 * Что владелец может сделать в мастере подключения.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface SetupActions {
    fun reload() = Unit

    fun chooseWay(way: SetupWay) = Unit

    /** Спрашивает, забыть ли пройденное; `false` — снимает вопрос. */
    fun askStartOver(ask: Boolean) = Unit

    fun startOver() = Unit

    fun getFactory() = Unit

    /** Касса заведена в кабинете: запоминается вместе с идентификатором у БФД и названием. */
    fun rememberRegister(id: String, kkmId: Int, name: String?) = Unit

    /** Набранное в форме того пути, которым идёт владелец. */
    fun edit(form: KkmForm) = Unit

    /**
     * Заводит кассу. Через кабинет токен выпускается в миг заведения,
     * вручную — берётся набранный.
     *
     * @param onDone касса заведена и читается: мастер своё сделал.
     */
    fun connect(onDone: () -> Unit) = Unit
}
