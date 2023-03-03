import java.nio.file.Path
import java.util.*


class ProjectProperties {

    companion object {
        val mainProperties = getProperties("/main.properties")
        val tableProperties = getProperties("/table.properties")
        val datauserProperties = getProperties("/datauser.properties")
        private fun getProperties(name: String): Properties {
            val properties = Properties()
            try {
                properties.load(ProjectProperties::class.java.getResourceAsStream(name))
            } catch (e: Exception) {
                throw NoSuchFileException(Path.of("src/main/resources/$name").toFile())
            }
            return properties
        }
    }
}