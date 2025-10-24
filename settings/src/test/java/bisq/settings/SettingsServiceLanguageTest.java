package bisq.settings;

import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsServiceLanguageTest {

    @TempDir
    Path tempDir;

    private String originalDefaultLanguage;
    private Locale originalDefaultLocale;

    @BeforeEach
    void setUp() {
        originalDefaultLanguage = LanguageRepository.getDefaultLanguage();
        originalDefaultLocale = LocaleRepository.getDefaultLocale();
    }

    @AfterEach
    void tearDown() {
        LanguageRepository.setDefaultLanguage(originalDefaultLanguage);
        LocaleRepository.setDefaultLocale(originalDefaultLocale);
    }

    private SettingsService newService() {
        PersistenceService ps = new PersistenceService(tempDir.toFile().getAbsolutePath());
        return new SettingsService(ps);
    }

    @Test
    void setLanguageCode_normalizesUnderscoreToHyphen() {
        SettingsService service = newService();
        service.setLanguageCode("pt_BR");
        assertThat(service.getLanguageCode().get()).isEqualTo("pt-BR");
    }

    @Test
    void setLanguageCode_acceptsHyphenatedTags() {
        SettingsService service = newService();
        service.setLanguageCode("af-ZA");
        assertThat(service.getLanguageCode().get()).isEqualTo("af-ZA");
    }

    @Test
    void setLanguageCode_acceptsPcmLowercase() {
        SettingsService service = newService();
        service.setLanguageCode("pcm");
        assertThat(service.getLanguageCode().get()).isEqualTo("pcm");
    }

    @Test
    void onPersistedApplied_migratesLegacyUnderscoreValue() {
        SettingsService service = newService();
        // Simulate persisted legacy underscore value
        service.getPersistableStore().languageCode.set("pt_BR");

        // Apply persisted and migration logic
        service.onPersistedApplied(new SettingsStore());

        // Should be normalized and stored as hyphenated tag
        assertThat(service.getLanguageCode().get()).isEqualTo("pt-BR");

        // Default language and default locale should be consistent
        assertThat(LanguageRepository.getDefaultLanguage()).isEqualTo("pt-BR");
        Locale defaultLocale = LocaleRepository.getDefaultLocale();
        assertThat(defaultLocale.getLanguage()).isEqualTo("pt");
        assertThat(defaultLocale.getCountry()).isEqualTo("BR");
    }
}

