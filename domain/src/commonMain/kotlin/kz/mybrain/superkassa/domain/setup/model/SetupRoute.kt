package kz.mybrain.superkassa.domain.setup.model

/**
 * Каким путём подключают кассу.
 *
 * Через кабинет — когда владелец с ключом ЭЦП сам заводит кассу в кабинете
 * БФД и ставит её на учёт. Вручную — когда кассу в БФД завёл сервисник
 * или бухгалтер, и на руках только идентификатор и токен: заставлять в этом
 * случае проходить кабинет значит требовать чужой ключ.
 */
enum class SetupWay { ViaCabinet, ByHand }

/**
 * Шаги мастера подключения.
 *
 * Порядок задан не удобством, а зависимостями: без номера кассу
 * не завести в кабинете, без кассы в кабинете не подать заявление,
 * без учёта в КГД кабинет не выпустит токен, без токена не завести
 * кассу здесь. На ручном пути кабинет и учёт заменяет один шаг —
 * идентификатор и токен, выданные в БФД без владельца.
 */
enum class SetupStep { Way, Factory, Cabinet, Application, Credentials, Admin }

/**
 * Шаги мастера по одному пути — по порядку, от первого до заведения кассы.
 *
 * Мастер показывает по одному шагу на экран, и вопросы «какой шаг
 * следующий», «с какого продолжить брошенное» и «можно ли стоять на этом
 * шаге» решаются здесь, по пройденному, а не на экране.
 *
 * @property choosing выбор пути — сам первый шаг; там, где кабинета нет,
 *   выбирать не из чего, и мастер начинается с заводского номера.
 */
class SetupRoute(val way: SetupWay, val choosing: Boolean) {

    /** Шаги пути по порядку. */
    val steps: List<SetupStep> = buildList {
        if (choosing) add(SetupStep.Way)
        add(SetupStep.Factory)
        if (way == SetupWay.ViaCabinet) {
            add(SetupStep.Cabinet)
            add(SetupStep.Application)
        } else {
            add(SetupStep.Credentials)
        }
        add(SetupStep.Admin)
    }

    /** С него мастер открывается. */
    val first: SetupStep get() = steps.first()

    /** Номер шага от единицы — для «Шаг 2 из 5». */
    fun number(step: SetupStep): Int = steps.indexOf(step) + 1

    /** Шаг после [step]; `null` — [step] последний. */
    fun after(step: SetupStep): SetupStep? = steps.getOrNull(steps.indexOf(step) + 1)

    /**
     * Шаг, на котором остановились: первый, который по [draft] не пройден.
     *
     * Пройденное живёт на диске, и мастер, брошенный на середине, открывается
     * там, где его оставили, а не с первого шага.
     */
    fun resume(draft: KkmSetupDraft): SetupStep = steps.firstOrNull { !draft.passed(it) } ?: steps.last()

    /** Шаги поверх первого, которые открываются, чтобы продолжить с [resume]. */
    fun toResume(draft: KkmSetupDraft): List<SetupStep> = steps.subList(1, steps.indexOf(resume(draft)) + 1)

    /**
     * Можно ли стоять на шаге [step] при пройденном [draft].
     *
     * Шаг, которого нет на этом пути или предыдущий которого не пройден, —
     * история «назад», пережившая «Начать заново» или выгрузку приложения, —
     * не открывается: на нём нечего делать.
     */
    fun opens(step: SetupStep, draft: KkmSetupDraft): Boolean = step in steps && when (step) {
        SetupStep.Cabinet -> draft.factoryNumber != null
        SetupStep.Application -> draft.cabinetRegisterId != null
        SetupStep.Admin -> way == SetupWay.ByHand || draft.cabinetRegisterId != null
        else -> true
    }

    companion object {
        /**
         * Путь мастера, продолженного по [draft]: выбранный прежде.
         *
         * Без кабинета путь один — вручную, что бы ни было выбрано на другой
         * машине; не выбранный ещё путь — через кабинет, раз он есть.
         *
         * @param withCabinet кабинет на этой платформе есть.
         */
        fun wayOf(draft: KkmSetupDraft, withCabinet: Boolean): SetupWay =
            if (withCabinet) draft.way ?: SetupWay.ViaCabinet else SetupWay.ByHand
    }
}
