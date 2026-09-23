package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.data.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.data.cabinet.KkmMapView
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.node.ServerClient
import kz.mybrain.superkassa.presentation.analytics.AddressAnswer
import kz.mybrain.superkassa.presentation.analytics.AnalyticsKkmList
import kz.mybrain.superkassa.presentation.analytics.AnalyticsMapCard
import kz.mybrain.superkassa.presentation.analytics.AnalyticsMapLegend
import kz.mybrain.superkassa.presentation.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.presentation.analytics.AnalyticsSieveBar
import kz.mybrain.superkassa.presentation.analytics.AnalyticsSourceBar
import kz.mybrain.superkassa.presentation.analytics.HeadOverMap
import kz.mybrain.superkassa.presentation.analytics.KkmGroup
import kz.mybrain.superkassa.presentation.analytics.MapAndDetails
import kz.mybrain.superkassa.presentation.analytics.MapLegend
import kz.mybrain.superkassa.presentation.analytics.MapTally
import kz.mybrain.superkassa.presentation.analytics.Placement
import kz.mybrain.superkassa.presentation.analytics.UnderMap
import kz.mybrain.superkassa.presentation.analytics.emptyMapReason
import kz.mybrain.superkassa.presentation.analytics.groupCell
import kz.mybrain.superkassa.presentation.analytics.kkmGroups
import kz.mybrain.superkassa.presentation.analytics.kkmMarks
import kz.mybrain.superkassa.presentation.analytics.mapCount
import kz.mybrain.superkassa.presentation.analytics.onScreen
import kz.mybrain.superkassa.presentation.analytics.placement
import kz.mybrain.superkassa.presentation.analytics.sievePlaces
import kz.mybrain.superkassa.presentation.components.EmptyState
import kz.mybrain.superkassa.presentation.map.MapGeocoder
import kz.mybrain.superkassa.presentation.map.MapMarks
import kz.mybrain.superkassa.presentation.map.MapTiles
import kz.mybrain.superkassa.presentation.map.MapView
import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.cabinetTexts
import kz.mybrain.superkassa.presentation.theme.Spacing
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
 * порядке и та же раскладка карты и подробностей: на снимке видно ровно
 * то, что увидит владелец.
 */
@Composable
internal fun MapLook(
    model: AnalyticsMapModel,
    laid: Placement,
    groups: List<KkmGroup>,
    view: KkmMapView?,
    whole: Int = laid.placed.size
) {
    val head = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
            AnalyticsSourceBar(model, laid, Look.texts) {}
            AnalyticsSieveBar(model, sievePlaces(view), Look.texts)
        }
    }
    HeadOverMap(Modifier.fillMaxSize(), head = head) {
        MapAndDetails(
            modifier = Modifier.fillMaxSize(),
            map = { MapOrReason(model, laid, groups, whole, Modifier.fillMaxSize()) },
            list = {
                AnalyticsKkmList(
                    placed = laid.placed,
                    unplaced = laid.unplaced,
                    chosen = model.chosen,
                    source = model.source,
                    texts = Look.texts,
                    onChoose = { model.show(it, groups) },
                    modifier = Modifier.fillMaxSize(),
                    sieved = model.sieve.set
                )
            },
            card = { UnderMap(model, laid, groups, Look.texts, Look.cabinet, remember { Look.panel() }) }
        )
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
            MapMarks(model.map, canvas, kkmMarks(shown, model), groupCell(1f)) { picked ->
                model.open(groups.first { it.id == picked.id })
            }
            Column(Modifier.padding(Spacing.snug), verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                MapTally(shown = mapCount(shown, laid.placed.size, whole), sieved = model.sieve.set, texts = Look.texts)
                MapLegend(legend, Look.texts)
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
