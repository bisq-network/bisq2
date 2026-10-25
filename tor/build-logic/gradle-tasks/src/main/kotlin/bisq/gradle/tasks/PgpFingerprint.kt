package bisq.gradle.tasks

import java.util.Locale.ROOT

object PgpFingerprint {
    fun normalize(fingerprint: String): String =
        fingerprint.filterNot { it.isWhitespace() }  // Remove all spaces
            .lowercase(ROOT)
}