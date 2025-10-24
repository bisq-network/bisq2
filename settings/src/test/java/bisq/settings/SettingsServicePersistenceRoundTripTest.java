package bisq.settings;

import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsServicePersistenceRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsAndReadsBack_pcm() {
        PersistenceService ps1 = new PersistenceService(tempDir.toFile().getAbsolutePath());
        SettingsService s1 = new SettingsService(ps1);
        s1.initialize().join();

        s1.setLanguageCode("pcm");

        // Force a persist by calling the persistence directly and wait for it to complete
        // This bypasses the rate limiter
        s1.getPersistence().persistAsync(s1.getPersistableStore().getClone()).join();

        // Simulate restart: new service with same persistence dir
        PersistenceService ps2 = new PersistenceService(tempDir.toFile().getAbsolutePath());
        SettingsService s2 = new SettingsService(ps2);
        // Read persisted data BEFORE initializing (this is what the application does)
        ps2.readAllPersisted().join();
        s2.initialize().join();

        assertThat(s2.getLanguageCode().get()).isEqualTo("pcm");
    }
}

