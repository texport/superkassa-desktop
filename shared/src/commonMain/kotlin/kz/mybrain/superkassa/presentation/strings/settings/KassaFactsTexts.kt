package kz.mybrain.superkassa.presentation.strings.settings

import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи сведений о кассе на этой машине и данных её авторизации в БФД.
 *
 * Своим файлом: сведения читают до входа, когда поддержка разбирает кассу,
 * которая не открылась или не пускает, и слов у них своя дюжина.
 */
data class KassaFactsTexts(
    val title: String,
    val hint: String,
    val appVersion: String,
    val coreVersion: String,
    val dataDirectory: String,
    val kkmCount: String,
    val unread: String,
    val ofdAuth: String,
    val nextRequest: String
)

/** Надписи сведений о кассе на выбранном языке. */
fun kassaFactsTexts(language: Language): KassaFactsTexts = when (language) {
    Language.Kk -> kazakhFacts
    Language.Ru -> russianFacts
    Language.En -> englishFacts
}

private val russianFacts = KassaFactsTexts(
    title = "Сведения о кассе",
    hint = "Первое, что спрашивает поддержка: какие версии стоят, как работает касса и где лежат её данные. " +
        "Видно и до входа — когда касса не открылась или не пускает.",
    appVersion = "Версия приложения",
    coreVersion = "Версия ядра",
    dataDirectory = "Каталог данных",
    kkmCount = "Касс на этой машине",
    unread = "Касса не ответила",
    ofdAuth = "Данные авторизации БФД",
    nextRequest = "Номер следующего запроса"
)

private val kazakhFacts = KassaFactsTexts(
    title = "Касса туралы мәліметтер",
    hint = "Қолдау қызметі алдымен сұрайтыны: қандай нұсқалар тұр, касса қалай жұмыс істейді және деректері " +
        "қайда жатыр. Кіргенге дейін де көрінеді — касса ашылмағанда немесе кіргізбегенде.",
    appVersion = "Қолданба нұсқасы",
    coreVersion = "Ядро нұсқасы",
    dataDirectory = "Деректер каталогы",
    kkmCount = "Осы машинадағы кассалар",
    unread = "Касса жауап бермеді",
    ofdAuth = "БФД авторизация деректері",
    nextRequest = "Келесі сұраныс нөмірі"
)

private val englishFacts = KassaFactsTexts(
    title = "Register facts",
    hint = "What support asks first: which versions are installed, how the register runs and where its data " +
        "lives. Visible before sign-in too — when the register did not open or does not let anyone in.",
    appVersion = "App version",
    coreVersion = "Core version",
    dataDirectory = "Data folder",
    kkmCount = "Registers on this machine",
    unread = "The register did not answer",
    ofdAuth = "BFD authorisation data",
    nextRequest = "Next request number"
)
