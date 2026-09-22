package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.analytics.AddressAnswer
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsKkmList
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSieveBar
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSourceBar
import kz.mybrain.superkassa.desktop.ui.analytics.KkmGroup
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapLegend
import kz.mybrain.superkassa.desktop.ui.analytics.MapLegend
import kz.mybrain.superkassa.desktop.ui.analytics.mapCount
import kz.mybrain.superkassa.desktop.ui.analytics.MapTally
import kz.mybrain.superkassa.desktop.ui.analytics.Placement
import kz.mybrain.superkassa.desktop.ui.analytics.UnderMap
import kz.mybrain.superkassa.desktop.ui.analytics.emptyMapReason
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.kkmMarks
import kz.mybrain.superkassa.desktop.ui.analytics.onScreen
import kz.mybrain.superkassa.desktop.ui.analytics.placement
import kz.mybrain.superkassa.desktop.ui.analytics.sievePlaces
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.map.MapGeocoder
import kz.mybrain.superkassa.desktop.ui.map.MapMarks
import kz.mybrain.superkassa.desktop.ui.map.MapTiles
import kz.mybrain.superkassa.desktop.ui.map.MapView
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue

/**
 * Общая оснастка снимков аналитики и торговых точек.
 *
 * Снимки смотрит человек, и собирать их состав в каждом наборе заново
 * значит расходиться в мелочах: одна касса в одном наборе окажется
 * с номером КГД, в другом без него, и разница на картинке будет не та,
 * которую проверяют.
 *
 * Ни одного обращения в сеть: плитки берутся с заведомо недоступного
 * источника и складываются в свой временный каталог — на снимке нужно
 * ровно то, что владелец видит без связи.
 */
internal object Look {

    val texts = analyticsTexts(Language.Ru)
    val cabinet = cabinetTexts(Language.Ru)

    /** Середина Алматы: с неё начинается карта приложения. */
    const val LATITUDE = 43.238949
    const val LONGITUDE = 76.889709

    /** Увеличение, на котором виден город. */
    const val CITY_ZOOM = 12

    /** Настолько в стороне, что в один ярлычок с соседом не сойдётся. */
    const val APART = 0.01

    fun shot(name: String, bytes: ByteArray) {
        val file = File("/tmp/$name.png")
        file.writeBytes(bytes)
        assertTrue(file.length() > 0, "снимок $name пуст")
    }

    /**
     * Рабочее место со своим временным каталогом.
     *
     * Настройки пишутся файлом рядом с указанным, и общий каталог задел бы
     * настройки владельца. Узел не спрашивается вовсе: на любой запрос отказ.
     *
     * Язык задаётся тот же, что и у надписей снимка: у нового рабочего
     * места он казахский, и части экрана, берущие надписи у рабочего места,
     * выходили на снимке на другом языке, чем остальные.
     */
    fun session(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        val directory = Files.createTempDirectory("an").toFile()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
            .also { it.switchLanguage(Language.Ru) }
    }

    /** Плитки, которых не будет: ни сети, ни чужого кэша. */
    fun tiles(): MapTiles = MapTiles(
        source = "file:///superkassa-no-tiles",
        folder = Files.createTempDirectory("tiles").toFile()
    )

    fun model(): AnalyticsMapModel = AnalyticsMapModel(CabinetSession(), MapGeocoder())

    /** Карточка под картой развёрнута, как у нового рабочего места; своё хранилище — чужие настройки не трогать. */
    fun panel(): AnalyticsMapCard = AnalyticsMapCard(Preferences(File(Files.createTempDirectory("an").toFile(), "kkm")))

    /** Легенда карты — так же развёрнута и так же со своим хранилищем. */
    fun legend(): AnalyticsMapLegend =
        AnalyticsMapLegend(Preferences(File(Files.createTempDirectory("an").toFile(), "kkm")))

    /** Касса аналитики: одна и та же во всех наборах снимков. */
    fun kkm(
        at: Int,
        place: String? = "Магазин на Абая",
        placeId: String? = "p-1",
        address: String? = "г. Алматы, пр. Абая, 10",
        blocked: Boolean = false,
        status: String? = "REGISTERED",
        shiftOpen: Boolean = false
    ) = AnalyticsKkm(
        cashRegisterId = "c$at",
        kkmId = 2000300 + at,
        registrationNumber = "%012d".format(4500000L + at),
        internalName = "Касса $at",
        retailPlaceId = placeId,
        retailPlaceName = place,
        address = address,
        status = status,
        blocked = blocked,
        shiftStatus = if (shiftOpen) "OPEN" else "CLOSED",
        shiftNumber = at.toLong(),
        lastContactAt = "2026-09-20T19:47:00Z"
    )

    fun view(placed: List<AnalyticsKkm>, without: List<AnalyticsKkm> = emptyList()) = KkmMapView(
        placedCount = placed.size,
        withoutPositionCount = without.size,
        placed = placed,
        withoutPosition = without
    )
}

/**
 * Разметка раздела карты так, как её собирает `AnalyticsMapPane`.
 *
 * Сам раздел спрашивает кабинет, а состояние держит при себе, и подставить
 * ему «сто касс» или «ни одной» снаружи нельзя. Здесь те же ряды в том же
 * порядке и той же ширины: на снимке видно ровно то, что увидит владелец.
 */
@Composable
internal fun MapLook(
    model: AnalyticsMapModel,
    laid: Placement,
    groups: List<KkmGroup>,
    view: KkmMapView?,
    whole: Int = laid.placed.size
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
        AnalyticsSourceBar(model, laid, Look.texts) {}
        AnalyticsSieveBar(model, sievePlaces(view), Look.texts)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                MapOrReason(model, laid, groups, whole, Modifier.weight(1f))
                UnderMap(model, laid, groups, Look.texts, Look.cabinet, remember { Look.panel() })
            }
            AnalyticsKkmList(
                placed = laid.placed,
                unplaced = laid.unplaced,
                chosen = model.chosen,
                source = model.source,
                texts = Look.texts,
                onChoose = { model.show(it, groups) },
                modifier = Modifier.width(Sizes.unplacedColumn).fillMaxHeight(),
                sieved = model.sieve.set
            )
        }
    }
}

/** Пока ни одной точки нет, на месте карты стоит объяснение. */
@Composable
private fun MapOrReason(
    model: AnalyticsMapModel,
    laid: Placement,
    groups: List<KkmGroup>,
    whole: Int,
    modifier: Modifier
) {
    if (laid.placed.isEmpty()) {
        val reason = emptyMapReason(laid, model.sieve.set, Look.texts)
        EmptyState(reason.icon, reason.title, reason.hint, modifier, centered = true)
        return
    }
    val legend = remember { Look.legend() }
    Box(modifier) {
        // Полотно называет свой размер само: ярлычки считают место
        // от окна карты, а не от окна приложения.
        MapView(model.map, Look.tiles(), Look.cabinet.map, Modifier.fillMaxSize(), onTap = { _, _ -> model.forget() }) { canvas ->
            val shown = onScreen(groups, model.map, canvas)
            MapMarks(model.map, canvas, kkmMarks(shown, model)) { picked ->
                model.open(groups.first { it.id == picked.id })
            }
            Box(Modifier.fillMaxSize()) {
                MapTally(
                    shown = mapCount(shown, laid.placed.size, whole),
                    sieved = model.sieve.set,
                    texts = Look.texts,
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.snug)
                )
                MapLegend(legend, Look.texts, Modifier.align(Alignment.BottomEnd).padding(Spacing.snug))
            }
        }
    }
}

/**
 * Кассы, разложенные по карте.
 *
 * Координаты приходят не от кабинета, а от поиска по адресу, и здесь его
 * заменяет заданный ответ: `null` означает «ещё ищем», отсутствие адреса
 * в наборе — «не нашли». Так на снимке получаются все три причины,
 * по которым кассы нет на карте.
 */
internal fun laidOut(view: KkmMapView, found: Map<String, Pair<Double, Double>?>): Placement =
    placement(view) { address ->
        if (address !in found) return@placement AddressAnswer.Missing
        val point = found[address] ?: return@placement AddressAnswer.Searching
        AddressAnswer.Found(point.first, point.second)
    }

/**
 * Ярлычки мест по разложенным кассам.
 *
 * Увеличение берётся у самой карты, как и в разделе: клетка места
 * считается в точках полотна, и на увеличении страны в один ярлычок
 * сходится то, что на увеличении города стоит порознь.
 */
internal fun groupsOf(laid: Placement, zoom: Int): List<KkmGroup> = kkmGroups(laid.placed, zoom)
