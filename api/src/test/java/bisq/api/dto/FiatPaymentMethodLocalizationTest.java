package bisq.api.dto;

import bisq.account.payment_method.fiat.FiatPaymentMethodChargebackRisk;
import bisq.i18n.Res;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@code Res.get()} calls respond to language changes.
 * <p>
 * This test covers the bug where {@code SettingsService.setLanguageTag()} only persisted
 * the language value without calling {@code Res.setAndApplyLanguageTag()}, causing all
 * API responses using {@code Res.get()} to return stale (old-language) strings.
 */
class FiatPaymentMethodLocalizationTest {

    @BeforeAll
    static void initBundles() {
        Res.setAndApplyLanguageTag("en");
    }

    @AfterEach
    void resetToEnglish() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void chargebackRisk_toString_respondsToLanguageChange() {
        Res.setAndApplyLanguageTag("en");
        assertEquals("Low", FiatPaymentMethodChargebackRisk.LOW.toString());
        assertEquals("Very low", FiatPaymentMethodChargebackRisk.VERY_LOW.toString());
        assertEquals("Medium", FiatPaymentMethodChargebackRisk.MEDIUM.toString());
        assertEquals("Moderate", FiatPaymentMethodChargebackRisk.MODERATE.toString());

        Res.setAndApplyLanguageTag("es");
        assertEquals("bajas", FiatPaymentMethodChargebackRisk.LOW.toString());
        assertEquals("muy bajas", FiatPaymentMethodChargebackRisk.VERY_LOW.toString());
        assertEquals("Medio", FiatPaymentMethodChargebackRisk.MEDIUM.toString());
        assertEquals("algunas", FiatPaymentMethodChargebackRisk.MODERATE.toString());
    }

    @Test
    void resGet_withoutApply_doesNotUpdateBundles() {
        // Documents the root cause of the bug: Res.setLanguageTag() without
        // updateBundles() does NOT reload resource bundles. This is what
        // SettingsService.setLanguageTag() was doing before the fix — it stored
        // the tag and persisted it, but never called Res.setAndApplyLanguageTag(),
        // so all Res.get() calls kept returning the old language.
        Res.setAndApplyLanguageTag("en");
        assertEquals("Low", FiatPaymentMethodChargebackRisk.LOW.toString());

        // Only set the tag, don't apply (don't reload bundles)
        Res.setLanguageTag("es");
        // Bundles are still English — this was the bug!
        assertEquals("Low", FiatPaymentMethodChargebackRisk.LOW.toString(),
                "Without updateBundles(), Res.get() should still return English");

        // Now apply — this reloads bundles and fixes it
        Res.updateBundles();
        assertEquals("bajas", FiatPaymentMethodChargebackRisk.LOW.toString(),
                "After updateBundles(), Res.get() should return Spanish");
    }

    @Test
    void multipleLanguageSwitches_allReflectedImmediately() {
        Res.setAndApplyLanguageTag("en");
        assertEquals("Low", FiatPaymentMethodChargebackRisk.LOW.toString());

        Res.setAndApplyLanguageTag("es");
        assertEquals("bajas", FiatPaymentMethodChargebackRisk.LOW.toString());

        Res.setAndApplyLanguageTag("de");
        assertEquals("geringe", FiatPaymentMethodChargebackRisk.LOW.toString());

        Res.setAndApplyLanguageTag("en");
        assertEquals("Low", FiatPaymentMethodChargebackRisk.LOW.toString());
    }
}
