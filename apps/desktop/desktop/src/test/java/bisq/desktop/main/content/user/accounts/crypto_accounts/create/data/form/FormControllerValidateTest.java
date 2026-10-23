/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.common.observable.Observable;
import bisq.desktop.common.Transitions;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class FormControllerValidateTest {
    private OtherFormController controller;

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX already initialized.
        }
        Res.setAndApplyLanguageTag("en");
    }

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        controller = new OtherFormController(null, new CryptoPaymentMethod("LTC"));
        controller.getView().onViewAttached();
        controller.getView().address.setText("address");
    }

    @AfterEach
    void tearDown() {
        Transitions.setSettingsService(null);
    }

    private boolean validateWithAutoConf(boolean isAutoConf, String maxTradeAmount) {
        controller.model.getIsAutoConf().set(isAutoConf);
        controller.getView().autoConfMaxTradeAmount.setText(maxTradeAmount);
        return controller.validate();
    }

    @Test
    void validateRejectsMaxTradeAmountTextThePayloadCreationCannotParse() {
        assertFalse(validateWithAutoConf(true, "NaN"));
        assertFalse(validateWithAutoConf(true, "0.5d"));
        assertFalse(validateWithAutoConf(true, "abc"));
        assertFalse(validateWithAutoConf(true, "2"));
    }

    @Test
    void validateAcceptsMaxTradeAmountTheParserHandles() {
        assertTrue(validateWithAutoConf(true, "0.5"));
        assertTrue(validateWithAutoConf(true, "0.123456789"));
        assertTrue(validateWithAutoConf(true, ""));
    }

    @Test
    void validateIgnoresMaxTradeAmountWhenAutoConfIsOff() {
        assertTrue(validateWithAutoConf(false, "NaN"));
    }
}
