package kz.mybrain.superkassa.strings.api.cabinet

/**
 * Объяснения разделов кабинета — те, что живут под значком у заголовка.
 *
 * Объяснение предмета, а не подпись к заголовку: «Виды деятельности»
 * без него читались как список неизвестно чего. Разойдясь по наборам,
 * подсказки начали расходиться и по языку.
 *
 * Одной группой, а не по сценариям: все подсказки выходят под значком
 * у заголовка карточки и правятся вместе. Группа заодно отвечает на вопрос
 * «где подсказка этого раздела»: искать её в одном месте.
 */
data class CabinetHintTexts(
    val signIn: String,

    /**
     * Что идёт, пока владелец ждёт подписи, и что делать, если окна
     * NCALayer на экране нет.
     *
     * Окно подписи открывает не приложение, а NCALayer, и встать оно
     * может за главным окном: владелец смотрел на ожидание, считая,
     * что подписывать ещё нечего.
     */
    val signWait: String,

    /**
     * Запрос NCALayer принял, а подписи не вернул.
     *
     * Своё объяснение, а не «Запустите NCALayer»: он запущен и на связи.
     * Ровно эту строку про работающий NCALayer владелец и читал спустя
     * три минуты ожидания.
     */
    val signNoAnswer: String,
    val address: String,
    val cardVersionsEmpty: String,
    val placesEmpty: String,
    val registersEmpty: String,
    val okedsEmpty: String,
    val chooseRegister: String,
    val documentsEmpty: String,
    val documentsNoneInPeriod: String,
    val actionsEmpty: String,
    val pickRegisterFirst: String,
    val technicalUnknown: String,
    val token: String,
    val internalName: String,
    val receipts: String,
    val cardMissing: String,
    val okedSearch: String,
    val technicalState: String,
    val bfdNoAnswer: String,
    val addressStep: String,
    val okedManual: String,
    val placeNotFound: String,
    val registers: String,
    val places: String,
    val placesTree: String,
    val okeds: String,
    val passport: String,
    val onThisMachine: String,
    val receiptCard: String,
    val reportCard: String,
    val shiftCard: String,
    val cashMovement: String,
    val factoryNumber: String,
    val card: String,
    val actionsJournal: String,
    val newPlaceNotChosen: String
)
