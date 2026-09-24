package kz.mybrain.superkassa.domain.print.port

/**
 * Что печати нужно снаружи: принтер и диск этой машины и выбор принтера кассы.
 *
 * Собирается в точке сборки платформы: печатают системный принтер и окно
 * сохранения файла машины, а выбор хранит рабочее место.
 *
 * @property printOut принтер и диск этой машины для печатной формы.
 * @property printChoices принтер кассы, копии и вид файла.
 */
data class PrintPorts(val printOut: PrintOut, val printChoices: PrintChoices)
