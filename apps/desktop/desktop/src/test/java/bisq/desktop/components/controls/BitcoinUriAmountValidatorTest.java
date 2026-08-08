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

import bisq.desktop.components.controls.validator.BitcoinUriAmountValidator;
import bisq.i18n.Res;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitcoinUriAmountValidatorTest {
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
        BitcoinUriAmountValidator validator = new BitcoinUriAmountValidator();
        textField.setValidator(validator);
    }

    @AfterEach
    void tearDown() {
        textField = null;
    }

    @Test
    void shouldAcceptBlank() {
        textField.setText("");

        assertTrue(textField.validate());
    }

    @Test
    void shouldAcceptZero() {
        textField.setText("0");

        assertTrue(textField.validate());
    }

    @Test
    void shouldAcceptWholeNumber() {
        textField.setText("1");

        assertTrue(textField.validate());
    }

    @Test
    void shouldAcceptDecimalAmount() {
        textField.setText("0.001");

        assertTrue(textField.validate());
    }

    @Test
    void shouldAcceptOneSatoshi() {
        textField.setText("0.00000001");

        assertTrue(textField.validate());
    }

    @Test
    void shouldAcceptEightDecimalPlaces() {
        textField.setText("1.23456789");

        assertTrue(textField.validate());
    }

    @Test
    void shouldRejectNegativeAmount() {
        textField.setText("-1");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectMoreThanEightDecimalPlaces() {
        textField.setText("1.234567891");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectScientificNotationLowercase() {
        textField.setText("1e-3");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectScientificNotationUppercase() {
        textField.setText("1E-3");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectCommaDecimalSeparator() {
        textField.setText("1,23");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectAlphabeticInput() {
        textField.setText("abc");

        assertFalse(textField.validate());
    }

    @Test
    void shouldRejectMixedInput() {
        textField.setText("1.2abc");

        assertFalse(textField.validate());
    }

    @Test
    void shouldAcceptTwentyOneMillionBitcoin() {
        textField.setText("21000000");

        assertTrue(textField.validate());
    }
}
