package qleak

//import io.micronaut.configuration.picocli.PicocliRunner
//import io.micronaut.context.ApplicationContext
//import io.micronaut.context.env.Environment
import io.quarkus.picocli.runtime.PicocliRunner
import io.quarkus.runtime.Quarkus
import io.quarkus.test.junit.QuarkusTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.function.Consumer

@QuarkusTest
class QleakCommandTest {
    @Test
    fun should_report_usage() {
//        val ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)

        val baos = ByteArrayOutputStream()
        System.setOut(PrintStream(baos))

        val args = arrayOf("-h")
//        PicocliRunner.run(QLeakCommand::class.java, ctx, *args)
        Quarkus.run(
                PicocliRunner::class.java,
                Consumer { integer -> },
                *args
        )

        assertThat(baos.toString()).contains("Usage: qleak").contains("HPROF")
    }

    @Test
    @Disabled("--version option does not work in test mode")
    fun should_report_version() {
//        val ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)

        val baos = ByteArrayOutputStream()
        System.setErr(PrintStream(baos))

        val args = arrayOf("--version")
//        PicocliRunner.run(QLeakCommand::class.java, ctx, *args)
        Quarkus.run(
                PicocliRunner::class.java,
                Consumer { integer -> },
                *args
        )

//        assertThat(baos.toString()).matches("qleak [\\d.]{3,}")
    }
}