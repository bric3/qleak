package qleak

import io.micronaut.configuration.picocli.PicocliRunner
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import shark.HeapAnalyzer
import shark.Hprof
import shark.HprofHeapGraph
import shark.HprofRecord
import shark.ObjectInspectors
import shark.OnAnalysisProgressListener
import shark.OnHprofRecordListener
import java.io.File
import java.util.concurrent.atomic.AtomicLong


@Command(name = "qleak",
         header = [
             "@|green        _            _    |@",
             "@|green   __ _| | ___  __ _| | __|@",
             "@|green  / _` | |/ _ \\/ _` | |/ /|@",
             "@|green | (_| | |  __/ (_| |   < |@",
             "@|green  \\__, |_|\\___|\\__,_|_|\\_\\|@",
             "@|green     |_|                  |@"
         ],
         description = ["Hprof leak analyzer"],
         mixinStandardHelpOptions = true,
         versionProvider = ManifestVersionProvider::class)
class QLeakCommand : Runnable {

//    @Option(names = ["-v", "--verbose"], description = ["..."])
//    private var verbose: Boolean = false

    @ArgGroup(exclusive = true, multiplicity = "1")
    var readOperation: ReadOperation? = null

    class ReadOperation {
        // TODO merge
        @Option(names = ["-s", "--simple-name-prefix"], required = true)
        var simpleNamePrefix: String? = null
        @Option(names = ["-q", "--qualified-name-prefix"], required = true)
        var qualifiedNamePrefix: String? = null
        @Option(names = ["-r", "--read-all-records"], required = true)
        var readAllRecords: Boolean = false
        @Option(names = ["-t", "--read-all-threads"], required = true)
        var readAllThreadNames: Boolean = false // TODO thread name filter
        @Option(names = ["-a", "--analyze"], required = true)
        var analyze: Boolean = false
    }

    @Parameters(description = ["hprof file location"],
                paramLabel = "HPROF")
    lateinit var hprofPath: File

    override fun run() {
        println("Parsing the HPROF file '$hprofPath'")
        println()

        if (readOperation?.analyze!!) {
            val heapAnalyzer = HeapAnalyzer(OnAnalysisProgressListener.NO_OP)
            val analysis = heapAnalyzer.analyze(
                    heapDumpFile = hprofPath,
                    computeRetainedHeapSize = true,
                    objectInspectors = ObjectInspectors.jdkDefaults
            )
            println(analysis)
        } else {
            Hprof.open(hprofPath)
                    .use { hprof ->
                        if (readOperation?.readAllRecords!!) {
                            hprof.reader.readHprofRecords(
                                    recordTypes = setOf(HprofRecord.StringRecord::class),
                                    listener = OnHprofRecordListener { position, record ->
                                        println((record as HprofRecord.StringRecord).string)
                                    })
                        } else if (!readOperation?.simpleNamePrefix.isNullOrBlank()) {
                            val graph = HprofHeapGraph.indexHprof(hprof)
                            graph.instances
                                    .filter { it.instanceClassSimpleName.startsWith(readOperation!!.simpleNamePrefix!!) }
                                    .forEach { println(it) }
                        } else if (!readOperation?.qualifiedNamePrefix.isNullOrBlank()) {
                            val graph = HprofHeapGraph.indexHprof(hprof)
                            graph.instances
                                    .filter { it.instanceClassName.startsWith(readOperation!!.qualifiedNamePrefix!!) }
                                    .forEach { println(it) }
                        } else if (readOperation?.readAllThreadNames!!) {
                            val heapGraph = HprofHeapGraph.indexHprof(hprof)
                            val threadClass = heapGraph.findClassByName("java.lang.Thread")!!
                            val threadNames: Sequence<String> = threadClass.instances.map { instance ->
                                val nameField = instance["java.lang.Thread", "name"]!!
                                nameField.value.readAsJavaString()!!
                            }
                            val counter = AtomicLong(0)
                            threadNames
                                    .onEach { counter.incrementAndGet() }
                                    .sorted().forEach { println(it) }
                            counter.let { println("\nThread count : $it") }
                        }

    //                    assertThat(testInstances).hasSize(1)
    //                    val test = testInstances[0]
    //                    val folderPath = test[JvmHprofParsingTest::class.name, "testFolder"]!!
    //                            .valueAsInstance!![TemporaryFolder::class.name, "folder"]!!
    //                            .valueAsInstance!![File::class.name, "path"]!!
    //                            .value.readAsJavaString()!!
    //
    //                    assertThat(folderPath).isEqualTo(testFolder.root.path)
                    }
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(QLeakCommand::class.java, *args)
        }
    }
}
