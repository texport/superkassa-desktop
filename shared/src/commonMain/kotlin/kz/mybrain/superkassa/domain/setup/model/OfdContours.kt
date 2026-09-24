package kz.mybrain.superkassa.domain.setup.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse

/**
 * Куда касса шлёт чеки: БФД и её контур.
 *
 * Поставщик один на весь продукт, и выбирать его владельцу нечего; контуров
 * у БФД три, а отвечает пока один.
 */
object OfdContours {

    /** Код единственной базы фискальных данных этого продукта. */
    const val PROVIDER: String = "BFD"

    /**
     * Поднят ли контур.
     *
     * Касса о готовности контура не сообщает: справочник перечисляет контуры
     * протокола, а не поднятые серверы, — поэтому неподнятые названы здесь
     * и вычёркиваются по мере запуска. Спрятать их значило бы обещать,
     * что контур у БФД один; неподнятый гаснет, а не пропадает.
     */
    fun raised(code: String): Boolean = code !in UNRAISED

    /**
     * Контур этого рабочего места.
     *
     * Рабочее место стоит в одной торговой точке и шлёт чеки в один контур,
     * и это записано у касс, уже заведённых здесь. Расходятся кассы во
     * мнении — берётся контур большинства: одна перенесённая не должна
     * перевешивать остальные. Касс нет или их контура нет среди [known] —
     * первый поднятый: подставить нечего, а погашенный выбором не станет.
     *
     * @param known коды контуров, которые называет касса.
     */
    fun ofWorkplace(kkms: List<KkmResponse>, known: List<String>): String =
        kkms.mapNotNull { it.ofdEnvironment }
            .filter { it in known }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: known.firstOrNull(::raised).orEmpty()

    private val UNRAISED = setOf("TEST", "PROD")
}
