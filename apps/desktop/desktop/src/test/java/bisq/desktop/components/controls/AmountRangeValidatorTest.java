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

package bisq.desktop.components.controls;

import bisq.desktop.components.controls.validator.AmountRangeValidator;
import bisq.i18n.Res;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountRangeValidatorTest {
    private MaterialTextField textField;

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
        textField = new MaterialTextField();
        textField.setValidator(new AmountRangeValidator("BTC", "0.001", "1"));
    }

    private boolean isValid(String text) {
        textField.setText(text);
        return textField.validate();
    }

    @Test
    void shouldRejectBlank() {
        assertFalse(isValid(""));
        assertFalse(isValid(" "));
    }

    @Test
    void shouldAcceptAmountsWithinTheRange() {
        assertTrue(isValid("0.001"));
        assertTrue(isValid("0.5"));
        assertTrue(isValid("1"));
        assertTrue(isValid("0,5"));
        assertTrue(isValid("0.12345678"));
        // The parser cuts the digits beyond the precision, so payload creation does not throw for it
        assertTrue(isValid("0.123456789"));
    }

    @Test
    void shouldRejectAmountsOutsideTheRange() {
        assertFalse(isValid("0.00099999"));
        assertFalse(isValid("1.00000001"));
        assertFalse(isValid("-0.5"));
    }

    @Test
    void shouldRejectTextTheDoubleParserAcceptedButTheAmountParserThrowsOn() {
        assertFalse(isValid("NaN"));
        assertFalse(isValid("Infinity"));
        assertFalse(isValid("0.5d"));
        assertFalse(isValid("0.5f"));
        assertFalse(isValid("abc"));
        assertFalse(isValid("1E-1000000000"));
    }
}
