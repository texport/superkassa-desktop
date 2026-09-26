package kz.mybrain.superkassa.strings.api.cabinet.company

/** Компания вошедшего и её виды деятельности по классификатору ОКЭД. */
data class CompanyTexts(
    val title: String,
    val fromEds: String,
    val okeds: String,
    val okedsEmpty: String,
    val primaryOked: String,
    val saveOkeds: String,
    val addOked: String,
    val okedSearch: String,
    val okedNotFound: String,

    /** Выдача классификатора упёрлась в предел: дальше списка нет. */
    val okedNarrowSearch: String,
    val okedCode: String,
    val okedName: String,
    val showMore: String
)
