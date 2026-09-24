package kz.mybrain.superkassa.presentation.cabinet.company

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel

/**
 * Что владелец делает с компанией. Действия по умолчанию пустые — для
 * снимков вида, где нажимать некому.
 */
interface CompanyActions {
    fun load() = Unit

    fun save() = Unit

    fun add(entry: OkedEntry, title: String) = Unit

    fun remove(code: String) = Unit

    fun markPrimary(code: String) = Unit

    fun search(query: String) = Unit

    fun more() = Unit
}

/** Действия раздела, выполняемые этой моделью. */
fun CompanyViewModel.actions(): CompanyActions {
    val model = this
    return object : CompanyActions {
        override fun load() = model.load()

        override fun save() = model.save()

        override fun add(entry: OkedEntry, title: String) = model.add(entry, title)

        override fun remove(code: String) = model.remove(code)

        override fun markPrimary(code: String) = model.markPrimary(code)

        override fun search(query: String) = model.search(query)

        override fun more() = model.more()
    }
}

/** Модель компании окна: переживает смену вкладки и раздела. */
@Composable
fun companyViewModel(cabinet: CabinetViewModel): CompanyViewModel = viewModel { CompanyViewModel(cabinet) }
