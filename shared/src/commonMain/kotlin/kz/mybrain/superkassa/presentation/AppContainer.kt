package kz.mybrain.superkassa.presentation

import kz.mybrain.superkassa.domain.journal.Journal
import kz.mybrain.superkassa.domain.kassa.Kassa
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.domain.workplace.WorkplaceMemory
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.strings.Language

/**
 * Всё, что экранам нужно снаружи: порты `domain`, собранные из `data`.
 *
 * Собирается один раз — в точке сборки платформенного приложения, `Main.kt`
 * настольной кассы и `MainActivity` Android, — и отдаётся каркасу окна.
 * Экран берёт отсюда нужное через фабрику своей модели и `data` не видит.
 *
 * Поля разложены по областям. Область, которой понадобился новый порт,
 * дописывает поле под своей строкой и строку сборки в каждой точке сборки;
 * чужие строки не трогаются, и правки областей не пересекаются.
 */
class AppContainer(
    // Общее: касса в процессе, вход, строка сообщений, память места, журнал.
    val kassa: Kassa,
    val signIn: SignIn,
    val notices: Notices,
    val memory: WorkplaceMemory,
    val journal: Journal,
    /** Язык кассира сейчас: модели говорят с ним в строке сообщений. */
    val language: () -> Language,
    // Касса: продажа, возврат, деньги.

    // Журнал: история, очередь, печать.

    // Настройки и кассиры.

    // Кабинет и мастер заведения кассы.

    // Аналитика.

)
