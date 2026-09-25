package kz.mybrain.superkassa.strings.impl.cabinet.company

import kz.mybrain.superkassa.strings.api.cabinet.company.CompanyTexts

/** Надписи [CompanyTexts] по-русски. */
internal val companyTextsRu = CompanyTexts(
    title = "Компания",
    fromEds = "Компания здесь та, чьей ЭЦП вы вошли: название и реквизиты " +
        "выданы КГД и в кабинете не правятся. Под ней и заводятся торговые точки, " +
        "кассы и заявления",
    okeds = "Виды деятельности",
    okedsEmpty = "Виды деятельности не заданы",
    primaryOked = "Основной",
    saveOkeds = "Сохранить виды деятельности",
    addOked = "Добавить вид деятельности",
    makePrimary = "Сделать основным",
    okedSearch = "Поиск в классификаторе ОКЭД",
    okedNotFound = "Ничего не нашлось. Классификатор знает только свои формулировки: аптека в нём называется " +
        "торговлей фармацевтическими товарами",
    okedNarrowSearch = "Классификатор длиннее показанного: «Показать ещё» в конце списка или уточните запрос",
    okedCode = "Код ОКЭД",
    okedName = "Наименование вида деятельности",
    showMore = "Показать ещё"
)
