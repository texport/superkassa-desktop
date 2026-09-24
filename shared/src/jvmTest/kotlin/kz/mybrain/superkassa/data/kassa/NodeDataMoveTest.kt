package kz.mybrain.superkassa.data.kassa

import io.github.texport.superkassa.importnode.api.NodeImportResult
import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import java.io.File
import java.io.RandomAccessFile
import java.net.ServerSocket
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Перенос данных прежнего узла при запуске — на временных каталогах.
 *
 * Каталог данных рабочей кассы здесь не трогается: у каждой проверки своё
 * рабочее место узла и свой каталог кассы. База узла — искусственная:
 * отказы, которые кассир должен понять, случаются раньше, чем её читают,
 * а сам перенос по существу проверяет ядро.
 */
class NodeDataMoveTest {
    private val root: File = createTempDirectory("node-move-").toFile()
    private val home = File(root, "home")
    private val kassa = File(home, "kassa")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `без базы узла касса начинает с чистого каталога`() {
        home.mkdirs()

        val moved = NodeDataMove.run(home, kassa, nodeAddress = null)

        assertEquals(NodeImportResult.NoNodeData, moved.getOrNull())
    }

    @Test
    fun `отвечающий узел — касса не открывается, кассиру сказано закрыть узел`() = ServerSocket(0).use { node ->
        nodeDatabase()

        val problem = refused(nodeAddress = "127.0.0.1:${node.localPort}")

        assertEquals(StartRefusal.NodeRunning, problem.refusal)
        assertTrue(problem.detail.isNotBlank(), "обслуживанию нечего передать")
    }

    @Test
    fun `касса уже открыта в другом окне — так и сказано`() {
        nodeDatabase()
        kassa.mkdirs()
        RandomAccessFile(File(kassa, "superkassa.lock"), "rw").use { file ->
            file.channel.lock().use { assertEquals(StartRefusal.KassaRunning, refused().refusal) }
        }
    }

    @Test
    fun `своя база кассы рядом с базой узла — переносить некуда, ничего не тронуто`() {
        nodeDatabase()
        kassa.mkdirs()
        File(kassa, "superkassa.db").writeText("")

        assertEquals(StartRefusal.BothDatabases, refused().refusal)
        assertEquals("", File(kassa, "superkassa.db").readText(), "база кассы тронута отказом переноса")
    }

    @Test
    fun `нечитаемая база узла — к обслуживанию, база узла цела`() {
        val database = nodeDatabase()

        assertEquals(StartRefusal.NodeDataUnfit, refused().refusal)
        assertEquals(GARBAGE, database.readText(), "перенос тронул базу узла")
        assertFalse(File(kassa, "superkassa.db").exists(), "касса получила недоперенесённую базу")
    }

    /** Сбой не переноса, а самого каталога: касса его не создала. */
    @Test
    fun `каталог кассы не создаётся — так и сказано`() {
        home.mkdirs()
        val blocked = File(home, "kassa").apply { writeText("файл на месте каталога") }

        val problem = NodeDataMove.run(home, File(blocked, "inner"), nodeAddress = null).exceptionOrNull()
            ?.let(NodeDataMove::problemOf)

        assertEquals(StartRefusal.KassaNotOpened, problem?.refusal)
    }

    /** База узла там, где её ищет узел без своих настроек; содержимое — не база. */
    private fun nodeDatabase(): File = File(home, "data/core.db").apply {
        parentFile.mkdirs()
        writeText(GARBAGE)
    }

    private fun refused(nodeAddress: String? = null) =
        NodeDataMove.problemOf(requireNotNull(NodeDataMove.run(home, kassa, nodeAddress).exceptionOrNull()))

    private companion object {
        const val GARBAGE = "не база SQLite"
    }
}
