package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Как называется касса на экране аналитики.
 *
 * Владелец сети узнаёт кассу по своему названию — «Касса у входа», —
 * а не по номеру КГД: номер он видит в заявлении раз в жизни кассы.
 * Названия нет — остаётся номер КГД, а у кассы, ещё не поставленной
 * на учёт, нет и его: тогда номер самой машины.
 */
fun kkmTitle(kkm: AnalyticsKkm): String = title(kkm.internalName, kkm.registrationNumber, kkm.kkmId)

/** То же для строки адреса обмена: списки должны звать кассу одинаково. */
fun kkmTitle(row: ExchangeAddress): String = title(row.internalName, row.registrationNumber, row.kkmId)

/**
 * Как названа строка торговой сводки — касса или торговая точка.
 *
 * Тип строки один на обе таблицы, и название берётся по тому же правилу:
 * своё имя владельца, а если его нет — то, чем строку называет кабинет.
 */
fun unitTitle(row: SalesUnit): String =
    row.name?.takeIf { it.isNotBlank() }
        ?: row.retailPlaceName?.takeIf { it.isNotBlank() }
        ?: row.registrationNumber?.takeIf { it.isNotBlank() }
        ?: row.id?.takeIf { it.isNotBlank() }
        ?: Glyphs.DASH

/**
 * Торговая точка строки.
 *
 * Пусто, когда точка и есть сама строка: в таблице точек название уже
 * стоит первым столбцом, и повторять его в соседнем незачем.
 */
fun unitPlace(row: SalesUnit): String =
    row.retailPlaceName?.takeIf { it.isNotBlank() && it != unitTitle(row) } ?: Glyphs.DASH

private fun title(internalName: String?, registrationNumber: String?, kkmId: Int): String =
    internalName?.takeIf { it.isNotBlank() }
        ?: registrationNumber?.takeIf { it.isNotBlank() }
        ?: "№ $kkmId"

/** Источник положения словами: тот же в переключателе и в карточке кассы. */
fun sourceTitle(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.sourceAddress
    PositionSource.CabinetCoordinates -> texts.sourceCabinet
    PositionSource.KkmCoordinates -> texts.sourceKkm
}

/** Чем источник положения является по существу: строка под переключателем. */
fun sourceHint(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.sourceAddressHint
    PositionSource.CabinetCoordinates -> texts.sourceCabinetHint
    PositionSource.KkmCoordinates -> texts.sourceKkmHint
}

/** Откуда взято положение кассы — полной фразой в её карточке. */
fun positionWords(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.fromAddress
    PositionSource.CabinetCoordinates -> texts.fromCabinet
    PositionSource.KkmCoordinates -> texts.fromKkm
}
