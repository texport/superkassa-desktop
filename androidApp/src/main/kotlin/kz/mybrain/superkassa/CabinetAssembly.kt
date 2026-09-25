package kz.mybrain.superkassa

import kz.mybrain.superkassa.data.cabinet.RemoteCabinet
import kz.mybrain.superkassa.data.eds.ChosenSigner
import kz.mybrain.superkassa.data.eds.EgovSigner
import kz.mybrain.superkassa.data.eds.KeyFileSigning
import kz.mybrain.superkassa.data.local.DocumentFiles
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.data.log.AppJournal
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.integrations.egovmobile.EgovDocument
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Кабинет БФД на Android — с подписью ЭЦП двумя способами.
 *
 * NCALayer на Android нет: подписывают eGov mobile (по умолчанию —
 * ключ у владельца в телефоне, QR или приложение на этом устройстве)
 * и файл ключа `.p12` провайдером Kalkan. Способ владелец выбирает
 * у кнопки входа; оба подписывающих спрашивают его через один стол
 * подписи, который показывает окно над всей кассой.
 *
 * @param screen активность на экране: над ней открываются выбор файла
 *   ключа и окно «Сохранить».
 * @param language язык кассира: на нём eGov mobile называет подпись.
 */
internal fun androidCabinet(
    preferences: Preferences,
    screen: ForegroundActivity,
    language: () -> Language
): RemoteCabinet {
    val desk = SignDesk()
    val egov = EgovSigner.open(desk, AppJournal(LogSource.Signature)) { egovDocument(language()) }
    val signer = ChosenSigner(
        linkedMapOf(
            SignMethod.EgovMobile to egov,
            SignMethod.KeyFile to KeyFileSigning(screen, desk)
        ),
        desk
    )
    return RemoteCabinet.open(
        address = preferences.cabinetUrl,
        signer = signer,
        files = DocumentFiles(screen),
        journal = AppJournal(LogSource.Cabinet),
        signing = signer
    )
}

/** Как подпись называется в eGov mobile: названия на трёх языках, описание — на языке кассира. */
private fun egovDocument(language: Language) = EgovDocument(
    description = textsOf(language).cabinet.eds.egovDocument,
    nameRu = textsOf(Language.Ru).cabinet.eds.egovDocument,
    nameKk = textsOf(Language.Kk).cabinet.eds.egovDocument,
    nameEn = textsOf(Language.En).cabinet.eds.egovDocument
)
