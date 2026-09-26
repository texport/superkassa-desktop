package kz.mybrain.superkassa.presentation.print.preview

import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.presentation.common.print.PrintActions

/**
 * Что можно сделать в самом окне печатной формы и в окне пина.
 *
 * Действия по умолчанию пустые — для снимков вида.
 */
internal interface PaperActions {

    /** Печать открытой формы: касса для этого уже не нужна. */
    fun printShown() = Unit

    /** Сохранение открытой формы в файл выбранного вида. */
    fun saveShown() = Unit

    /** Поделиться открытой формой с покупателем путём [way]. */
    fun shareShown(way: ShareWay) = Unit

    /** Повтор того, на чём касса отказала. */
    fun retry() = Unit

    fun close() = Unit

    /** Владелец ввёл пин: работа продолжается с того места, где встала. */
    fun enterPin(pin: String) = Unit

    fun cancelPin() = Unit
}

/** Действия печати, выполняемые этой моделью. */
internal fun PrintViewModel.actions(): PrintActions {
    val model = this
    return object : PrintActions {
        override fun preview(documentId: String?, file: String?) =
            model.preview(documentId?.let(PrintSource::Journal), file)

        override fun previewPacket(packet: String, name: String, file: String?) =
            model.preview(PrintSource.Packet(packet, name), file)

        override fun print(documentId: String) = model.print(PrintSource.Journal(documentId))

        override fun printPacket(packet: String) = model.print(PrintSource.Packet(packet, PACKET))

        override val shareWays: List<ShareWay> get() = model.sharing.ways

        override fun share(documentId: String, way: ShareWay) =
            model.sharing.share(PrintSource.Journal(documentId), file = null, way = way)
    }
}

/** Действия окна печатной формы, выполняемые этой моделью. */
internal fun PrintViewModel.paperActions(): PaperActions {
    val model = this
    return object : PaperActions {
        override fun printShown() = model.printShown()

        override fun saveShown() = model.saveShown()

        override fun shareShown(way: ShareWay) {
            val source = model.shown ?: return
            model.sharing.share(source, model.state.value.savingName, way)
        }

        override fun retry() = model.retry()

        override fun close() = model.close()

        override fun enterPin(pin: String) = model.enterPin(pin)

        override fun cancelPin() = model.cancelPin()
    }
}

/** Имя документа, напечатанного без просмотра: файла у него не будет. */
private const val PACKET = "document"
