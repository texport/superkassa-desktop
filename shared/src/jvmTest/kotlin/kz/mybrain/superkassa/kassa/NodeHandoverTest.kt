package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.importnode.api.NodeImportResult
import kz.mybrain.superkassa.data.node.NodeHandover
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Перенос данных узла при запуске — на временных каталогах.
 *
 * Каталог рабочего места не трогается: перенос на нём включается явно
 * и только когда все разделы переведены на кассу процесса.
 */
class NodeHandoverTest {
    private val root: File = createTempDirectory("handover-").toFile()
    private val nodeHome = File(root, "node").apply { mkdirs() }
    private val kassaDir = File(root, "kassa")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `выключенный перенос каталогов не трогает`() {
        assertNull(NodeHandover.run(nodeHome, kassaDir, nodeAddress = null, enabled = false))
        assertFalse(kassaDir.exists(), "выключенный перенос завёл каталог кассы")
    }

    @Test
    fun `у узла без базы переносить нечего, и узел не списан`() {
        val result = NodeHandover.run(nodeHome, kassaDir, nodeAddress = null, enabled = true)

        assertEquals(NodeImportResult.NoNodeData, result)
        assertFalse(NodeHandover.retired(result))
    }

    @Test
    fun `после переноса узел больше не запускается`() {
        assertTrue(NodeHandover.retired(NodeImportResult.AlreadyImported))
        assertFalse(NodeHandover.retired(null))
    }

    @Test
    fun `перенос включается только явно`() {
        assertFalse(NodeHandover.enabled(variable = null, property = null))
        assertTrue(NodeHandover.enabled(variable = "true", property = null))
        assertTrue(NodeHandover.enabled(variable = " ", property = "true"))
        assertFalse(NodeHandover.enabled(variable = "false", property = "true"))
    }
}
