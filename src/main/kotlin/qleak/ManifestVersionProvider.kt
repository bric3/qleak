package qleak

import picocli.CommandLine.IVersionProvider
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest


class ManifestVersionProvider : IVersionProvider {
    override fun getVersion(): Array<String> {
        val resources = ManifestVersionProvider::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            try {
                val manifest = Manifest(url.openStream())

                if (isApplicableManifest(manifest)) {
                    val attr = manifest.getMainAttributes()
                    return arrayOf("%s %s".format(
                            get(attr, "Implementation-Title"),
                            get(attr, "Implementation-Version")))
                }
            } catch (ex: IOException) {
                return arrayOf("Unable to read from $url: $ex")
            }

        }
        return arrayOf<String>()
    }

    private fun isApplicableManifest(manifest: Manifest): Boolean {
        return QLeakCommand::class.qualifiedName == get(manifest.mainAttributes, "Main-Class")
    }

    private operator fun get(attributes: Attributes, key: String): String {
        return attributes.get(Attributes.Name(key)).toString()
    }
}
