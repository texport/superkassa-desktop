package kz.mybrain.superkassa.desktop.app

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * Узел, запущенный вместе с кассой.
 *
 * Касса без узла не работает, и до сих пор его поднимали отдельно —
 * сборкой из исходников. Проверить кассу на чужой машине это делало
 * работой на полдня: поставить Java, собрать ядро, собрать узел.
 *
 * Установщик несёт узел внутри: `node.jar` лежит среди ресурсов
 * приложения, запускается той же Java, которую установщик принёс с собой,
 * и останавливается вместе с кассой. Кассиру остаётся один значок.
 *
 * Узел не поднимается в двух случаях: ресурса нет (так собрано
 * приложение при разработке — узел там свой, запущенный из Gradle)
 * и адрес узла ведёт на другую машину (один узел на несколько рабочих
 * мест). Уже отвечающий узел второй раз тоже не запускается.
 */
class LocalNode(
    private val address: String,
    private val home: File = defaultHome(),
    /** Где установщик разложил ресурсы приложения; при разработке их нет. */
    private val resources: File? = packagedResources(),
    /**
     * Удалось ли вернуть файлу право на запуск.
     *
     * Отдельным правилом, потому что проверить его на рабочей машине
     * нельзя: право возвращает владелец файла, а в общей папке владелец —
     * не кассир. Подставляется в проверках.
     */
    private val allowRun: (File) -> Boolean = { it.canExecute() || it.setExecutable(true, false) }
) {

    private var started: Process? = null

    /**
     * Поднимает узел, если он нужен и его ещё нет.
     *
     * @return `true`, если узел был запущен этим вызовом.
     */
    fun start(): Boolean {
        val jar = File(resources ?: return false, JAR_NAME).takeIf { it.isFile } ?: return false
        val place = placeOf(address) ?: return false
        if (answers(place)) return false
        val process = runCatching { launch(jar) }.getOrNull() ?: return false
        started = process
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
        return true
    }

    /** Ждёт, пока узел начнёт отвечать; `false` — не дождались. */
    fun awaitReady(millis: Long = READY_TIMEOUT): Boolean {
        val place = placeOf(address) ?: return false
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            if (answers(place)) return true
            if (started?.isAlive == false) return false
            Thread.sleep(POLL_INTERVAL)
        }
        return false
    }

    /** Останавливает узел, если его поднимали здесь. */
    fun stop() {
        val process = started ?: return
        started = null
        process.destroy()
        if (!process.waitFor(STOP_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
        }
    }

    /**
     * Запускает узел той же Java, что несёт установщик.
     *
     * Рабочая папка — та же, где лежат настройки рабочего места: узел
     * кладёт рядом свою базу, и она переживает переустановку кассы.
     * Вывод уходит в файл: окна у узла нет, а разбирать отказ запуска
     * по пустому месту нечем.
     */
    private fun launch(jar: File): Process {
        val java = javaBinary() ?: error("рантайм узла не собран")
        home.mkdirs()
        return ProcessBuilder(java.path, "-jar", jar.path)
            .directory(home)
            .redirectErrorStream(true)
            .redirectOutput(File(home, LOG_NAME))
            .start()
    }

    /**
     * Чем запускать узел.
     *
     * Рантайм самой кассы не годится: jpackage кладёт его без запускающего
     * файла. Рядом с узлом лежит собранный для него `jlink`-рантайм —
     * им и запускаем.
     *
     * Право на запуск нужно не только самой Java. Узел собирает печатную
     * форму браузером, а `ProcessBuilder` запускает чужие программы через
     * `lib/jspawnhelper`: без права у него любая печатная форма отвечала
     * «внутренняя ошибка сервера», и в журнале стояло `posix_spawn failed`
     * с подсказкой про несовпадение версий JDK, которого не было.
     */
    fun javaBinary(): File? =
        runnable(resources?.resolve(RUNTIME_NAME)) ?: runnable(ownRuntime())

    /**
     * Запускающий файл рантайма, если им можно воспользоваться.
     *
     * Право на запуск теряется по дороге: установщик раскладывает ресурсы
     * приложения обычными файлами, и `jlink`-овский `bin/java` приезжает
     * без него. Вернуть его удаётся не всегда — программы ставятся в общие
     * папки, принадлежащие не кассиру.
     */
    @Suppress("ReturnCount")
    private fun runnable(runtime: File?): File? {
        val home = runtime ?: return null
        val java = sequenceOf("java.exe", "java").map { File(home, "bin/$it") }.firstOrNull { it.isFile }
            ?: return null
        val needed = listOf(java) + HELPERS.map { File(home, it) }.filter { it.isFile }
        return java.takeIf { needed.all(allowRun) }
    }

    /**
     * Свой список рантайма — там, где у кассира есть права.
     *
     * Нужен, когда касса поставлена в общую папку: `/opt` в Linux,
     * `/Applications` в macOS. Там файлы принадлежат не кассиру, право
     * на запуск ему не вернуть, и узел не поднимался бы вовсе. Список
     * снимается один раз, при первом запуске.
     */
    private fun ownRuntime(): File? {
        val source = resources?.resolve(RUNTIME_NAME)?.takeIf { it.isDirectory } ?: return null
        val own = File(home, RUNTIME_NAME)
        if (!File(own, "bin").isDirectory) {
            runCatching { source.copyRecursively(own, overwrite = true) }.getOrElse { return null }
        }
        return own
    }

    companion object {

        /** Имя узла среди ресурсов приложения. */
        const val JAR_NAME = "node.jar"

        /** Имя рантайма узла среди ресурсов приложения. */
        const val RUNTIME_NAME = "node-runtime"

        /**
         * Что в рантайме запускается, кроме самой Java.
         *
         * `jspawnhelper` — через него `ProcessBuilder` запускает чужие
         * программы; узлу он нужен для печатной формы.
         */
        val HELPERS = listOf("lib/jspawnhelper")

        /** Куда уходит вывод узла: имя рядом с настройками рабочего места. */
        private const val LOG_NAME = "node.log"

        /** Файл, в который узел пишет свой вывод. */
        fun output(home: File = defaultHome()): File = File(home, LOG_NAME)

        /**
         * Где установщик разложил ресурсы приложения.
         *
         * Свойство выставляет сама сборка Compose; при запуске из Gradle
         * его нет — и узла в ресурсах тоже нет.
         */
        const val RESOURCES_DIR = "compose.application.resources.dir"

        const val READY_TIMEOUT = 90_000L
        const val POLL_INTERVAL = 250L
        const val STOP_TIMEOUT = 15_000L
        const val PROBE_TIMEOUT = 300

        /** Имена, которыми называют эту же машину. */
        val OWN_HOSTS = setOf("127.0.0.1", "localhost", "0.0.0.0", "::1", "[::1]")

        fun defaultHome(): File = Preferences.defaultFile().parentFile

        fun packagedResources(): File? =
            System.getProperty(RESOURCES_DIR)?.let(::File)?.takeIf { it.isDirectory }

        /** Адрес узла на этой машине; `null` — узел чужой или адрес непонятен. */
        fun placeOf(address: String): InetSocketAddress? {
            val uri = runCatching { URI(address.trim()) }.getOrNull() ?: return null
            val host = uri.host?.takeIf { it in OWN_HOSTS } ?: return null
            val port = uri.port.takeIf { it > 0 } ?: return null
            return InetSocketAddress(host, port)
        }

        fun answers(place: InetSocketAddress): Boolean = runCatching {
            Socket().use { it.connect(place, PROBE_TIMEOUT) }
            true
        }.getOrDefault(false)
    }
}
