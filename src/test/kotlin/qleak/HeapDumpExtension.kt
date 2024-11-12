package qleak

import com.sun.management.HotSpotDiagnosticMXBean
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.Comparator


class HeapDumpExtension : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private val HEAP_DUMP_DIR = """HeapDump"""

    override fun beforeTestExecution(context: ExtensionContext?) {
        val tempDir = Files.createTempDirectory(context!!.requiredTestMethod.name)
        getStore(context).put(HEAP_DUMP_DIR, tempDir)
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        val tempDir = getStore(context!!).get(HEAP_DUMP_DIR)
        Files.walk(tempDir as Path)
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .forEach { it.delete() };
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == HeapDumper::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return HeapDumper(getStore(extensionContext).get(HEAP_DUMP_DIR) as Path)
    }

    class HeapDumper(private val heapDumpsPath: Path) {
        @Throws(IOException::class)
        fun dumpHeap(): Path {
            val hprofPath = heapDumpsPath.resolve("heapDump-" + UUID.randomUUID() + ".hprof").toAbsolutePath()
            Companion.hotSpotDiag.dumpHeap(hprofPath.toString(), /* live */ true)
            return hprofPath
        }

        companion object {
            private val hotSpotDiag by lazy {
                ManagementFactory.newPlatformMXBeanProxy(
                        ManagementFactory.getPlatformMBeanServer(),
                        "com.sun.management:type=HotSpotDiagnostic",
                        HotSpotDiagnosticMXBean::class.java
                )
            }
        }
    }

    private fun getStore(context: ExtensionContext): Store {
        return context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))
    }
}
