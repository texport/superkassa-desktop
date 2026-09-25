package kz.mybrain.superkassa.presentation.analytics.common

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.SalesUnit
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Как названа строка торговой сводки — касса или торговая точка.
 *
 * Тип строки один на обе таблицы, и название берётся по тому же правилу:
 * своё имя владельца, а если его нет — то, чем строку называет кабинет.
 */
internal fun unitTitle(row: SalesUnit): String =
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
internal fun unitPlace(row: SalesUnit): String =
    row.retailPlaceName?.takeIf { it.isNotBlank() && it != unitTitle(row) } ?: Glyphs.DASH

/** Источник положения словами: тот же в переключателе и в карточке кассы. */
internal fun sourceTitle(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.sourceAddress
    PositionSource.CabinetCoordinates -> texts.sourceCabinet
    PositionSource.KkmCoordinates -> texts.sourceKkm
}

/** Чем источник положения является по существу: строка под переключателем. */
internal fun sourceHint(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.sourceAddressHint
    PositionSource.CabinetCoordinates -> texts.sourceCabinetHint
    PositionSource.KkmCoordinates -> texts.sourceKkmHint
}

/** Откуда взято положение кассы — полной фразой в её карточке. */
internal fun positionWords(source: PositionSource, texts: AnalyticsTexts): String = when (source) {
    PositionSource.RetailPlaceAddress -> texts.fromAddress
    PositionSource.CabinetCoordinates -> texts.fromCabinet
    PositionSource.KkmCoordinates -> texts.fromKkm
}
