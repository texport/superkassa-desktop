package kz.mybrain.superkassa.domain.update.model

import kz.mybrain.superkassa.domain.version.model.AppVersion

/**
 * Выпуск кассы: метка, страница выпуска и установщик под эту систему.
 *
 * Из описания выпуска берётся только то, что нужно проверке; какой из
 * файлов выпуска ставится на эту систему, решает служба выпусков.
 *
 * @property installer установщик под эту систему; `null` — своего нет,
 *   и кассиру открывают страницу выпуска.
 */
data class Release(val tag: String, val page: String, val installer: Installer? = null)

/** Чем закончился вопрос о свежем выпуске. */
sealed interface ReleaseAnswer {
    data class Found(val release: Release) : ReleaseAnswer

    /** Служба выпусков не ответила или ответила не выпуском; причина — для журнала. */
    data class Unreachable(val reason: String) : ReleaseAnswer
}

/**
 * Новая версия кассы, которую можно поставить.
 *
 * @param installer установщик под эту систему; без него — страница
 *   выпуска, где кассир выберет файл сам.
 */
data class AvailableUpdate(
    val version: AppVersion,
    val page: String,
    val installer: Installer?
)

/** Чем закончилась проверка, запущенная рукой. */
sealed interface UpdateOutcome {
    data object UpToDate : UpdateOutcome

    /**
     * Установлена сборка разработчика: выпуски ей не предлагаются.
     *
     * Она подписана не ключом выпусков и собрана не из метки: выпуск поверх
     * неё на Android не встанет, а «1.0.0-dev» ниже любого «1.0.0» — и касса
     * звала бы «Скачать» на каждой машине разработчика.
     */
    data object Development : UpdateOutcome

    data class Available(val update: AvailableUpdate) : UpdateOutcome
    data object Unreachable : UpdateOutcome
}

/**
 * Найденная версия после проверки.
 *
 * Недоступность службы ничего не меняет: найденное раньше остаётся
 * найденным, а сеть на рабочем месте пропадает часто.
 */
fun UpdateOutcome.after(before: AvailableUpdate?): AvailableUpdate? = when (this) {
    is UpdateOutcome.Available -> update
    UpdateOutcome.UpToDate, UpdateOutcome.Development -> null
    UpdateOutcome.Unreachable -> before
}
