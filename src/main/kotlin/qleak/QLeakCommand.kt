package qleak

import io.micronaut.configuration.picocli.PicocliRunner
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File


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

    @Parameters(index = "0",
                description = ["hprof file location"],
                paramLabel = "HPROF")
    lateinit var hprofPath: File

    override fun run() {
        // business logic here
        println("Hi!")
        println("The file is $hprofPath")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(QLeakCommand::class.java, *args)
        }
    }
}
