package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.LocalNode
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Чем касса запускает узел.
 *
 * Установщик раскладывает ресурсы обычными файлами: у `jlink`-овского
 * `bin/java` нет права на запуск. В `.deb` он лежит как `-rw-r--r--`
 * в `/opt/superkassa`, где файлы принадлежат не кассиру, — вернуть право
 * там некому, и узел не поднялся бы вовсе. Проверка на рабочей машине
 * этого не показывает: сборка лежит в своей папке, и право возвращается.
 */
class LocalNodeTest {

    private fun runtimeIn(directory: File): File {
        val runtime = File(directory, "node-runtime")
        File(runtime, "bin").mkdirs()
        File(runtime, "lib").mkdirs()
        File(runtime, "bin/java").writeText("#!/bin/sh\n")
        File(runtime, "lib/jspawnhelper").writeText("двоичный")
        File(directory, "node.jar").writeText("узел")
        return directory
    }

    @Test
    fun `право на запуск возвращается прямо в ресурсах`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()

        val java = LocalNode(ADDRESS, home, resources).javaBinary()

        assertNotNull(java)
        assertTrue(java.canExecute())
        assertEquals(resources, java.parentFile.parentFile.parentFile)
    }

    /**
     * Касса поставлена в общую папку — `/opt/superkassa` в Linux,
     * `/Applications` в macOS. Файлы там принадлежат не кассиру, право
     * на запуск вернуть некому, и рантайм снимается к себе.
     */
    @Test
    fun `из общей папки рантайм снимается к себе`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()
        val theirs = File(resources, "node-runtime/bin/java")

        val java = LocalNode(ADDRESS, home, resources, allowRun = { it != theirs }).javaBinary()

        assertNotNull(java)
        assertEquals(File(home, "node-runtime/bin/java"), java)
        assertTrue(File(home, "node.jar").exists().not())
    }

    /**
     * Узел собирает печатную форму браузером, а запускает чужие программы
     * через `lib/jspawnhelper`. Без права на запуск у него печатная форма
     * отвечала «внутренняя ошибка сервера», и в журнале стояло
     * `posix_spawn failed` с подсказкой про версии JDK, которой там не было.
     * Проверялось же только право у самой Java.
     */
    @Test
    fun `право на запуск нужно и помощнику запуска`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()
        val helper = File(resources, "node-runtime/lib/jspawnhelper")

        val java = LocalNode(ADDRESS, home, resources).javaBinary()

        assertNotNull(java)
        assertTrue(helper.canExecute(), "помощник запуска остался без права на запуск")
    }

    /**
     * Java запускается, а помощник запуска нет — этого уже достаточно,
     * чтобы снять рантайм к себе: печатная форма без него не собирается.
     */
    @Test
    fun `рантайм снимается к себе и из-за помощника запуска`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()
        val theirs = File(resources, "node-runtime/lib/jspawnhelper")

        val java = LocalNode(ADDRESS, home, resources, allowRun = { it != theirs }).javaBinary()

        assertEquals(File(home, "node-runtime/bin/java"), java)
        assertTrue(File(home, "node-runtime/lib/jspawnhelper").isFile, "помощник запуска не снят")
    }

    /**
     * Узлу называют рабочее место явно. Одной рабочей папки процесса
     * мало: узел, поднятый из другой папки, считал относительный путь
     * к базе от неё и молча заводил пустую базу — кассы владельца
     * выглядели исчезнувшими.
     */
    @Test
    fun `узлу называют рабочее место`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()
        val node = LocalNode(ADDRESS, home, resources)

        val command = node.command(node.javaBinary()!!, File(resources, "node.jar"))

        assertTrue("-D${LocalNode.HOME_PROPERTY}=${home.path}" in command, command.toString())
        assertEquals(listOf("-jar", File(resources, "node.jar").path), command.takeLast(2))
    }

    @Test
    fun `без ресурсов узел не поднимается`() {
        val home = Files.createTempDirectory("home").toFile()

        assertNull(LocalNode(ADDRESS, home, resources = null).javaBinary())
        assertEquals(false, LocalNode(ADDRESS, home, resources = null).start())
    }

    /**
     * Узел на другой машине — не наше дело.
     *
     * Один узел обслуживает несколько рабочих мест, и поднимать его копию
     * у себя значит развести две кассы с одним регистрационным номером.
     */
    @Test
    fun `чужой узел не поднимается`() {
        val resources = runtimeIn(Files.createTempDirectory("resources").toFile())
        val home = Files.createTempDirectory("home").toFile()

        assertEquals(false, LocalNode("http://192.168.50.35:8080", home, resources).start())
    }

    private companion object {
        const val ADDRESS = "http://127.0.0.1:8080"
    }
}
