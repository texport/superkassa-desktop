package kz.mybrain.superkassa.domain.workplace.port

import kz.mybrain.superkassa.domain.workplace.model.LookChoice

/**
 * Где рабочее место хранит вид окна между запусками.
 *
 * На настольной кассе — файлами в каталоге данных, на Android — в настройках
 * приложения. Выбор глазами фискальной работы не касается, и храниться
 * вместе с кассой ему незачем.
 */
interface LookMemory {

    /** Выбор, сохранённый в прошлый раз; запись сохраняет новый. */
    var look: LookChoice
}
