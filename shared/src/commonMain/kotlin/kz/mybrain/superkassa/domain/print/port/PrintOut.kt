package kz.mybrain.superkassa.domain.print.port

import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.model.PrintKind

/**
 * Куда уходит готовая печатная форма: на принтер рабочего места или в файл.
 *
 * Ни одного обращения к кассе отсюда не идёт: форма уже нарисована,
 * здесь только принтер, лента и диск этой машины.
 */
interface PrintOut {

    /** Принтеры, которые видит эта машина; пусто — принтера нет вовсе. */
    suspend fun printers(): List<String>

    /**
     * Сама лента без полей страницы вокруг.
     *
     * Касса рисует ленту на странице, и по бокам остаётся её фон:
     * на экране кассир принимал его за часть документа, а на печати
     * он съедал ширину бумаги.
     */
    suspend fun tape(png: ByteArray): ByteArray

    /**
     * Печатает ленту: своей шириной, а длинную — по страницам.
     *
     * @param printer имя принтера; `null` — принтер системы по умолчанию.
     * @param widthMm ширина ленты в миллиметрах; ноль — по ширине страницы.
     * @return ушло ли задание на принтер.
     */
    suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Boolean

    /**
     * Спрашивает, куда положить файл, и кладёт его туда.
     *
     * @return сохранено, владелец передумал или сохранять здесь некуда.
     */
    suspend fun keep(bytes: ByteArray, name: String, title: String): Kept
}

/**
 * Что рабочее место помнит о печати: принтер кассы, копии и вид файла.
 *
 * У кассы свой принтер — чековый, а не тот, на котором в конторе печатают
 * договоры, и за одним компьютером их бывает две.
 */
interface PrintChoices {

    /** Принтер кассы; `null` — системный по умолчанию. */
    fun printer(kkmId: String): String?

    fun choosePrinter(kkmId: String, name: String?)

    /** Сколько копий печатать: от одной до [MAX_COPIES]. */
    var copies: Int

    /** В каком виде сохранять форму. */
    fun kind(): PrintKind

    fun chooseKind(kind: PrintKind)

    companion object {
        /** Больше трёх копий чека не печатают: это чек, а не тираж. */
        const val MAX_COPIES = 3
    }
}
