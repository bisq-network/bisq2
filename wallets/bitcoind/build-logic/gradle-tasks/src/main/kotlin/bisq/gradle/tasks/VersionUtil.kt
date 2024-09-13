package bisq.gradle.tasks

import org.gradle.api.Project
import java.io.File

object VersionUtil {
    fun getVersionFromFile(project: Project): String =
            File(project.layout.projectDirectory.toString() + "/version.txt").readText()
}