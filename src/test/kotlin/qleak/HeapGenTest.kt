package qleak

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HeapDumpExtension::class)
class HeapGenTest {

    @Test
    internal fun can_heap_dump(heapDumper: HeapDumpExtension.HeapDumper) {
        val hprofPath = heapDumper.dumpHeap()

        assertThat(hprofPath).exists()
    }
}