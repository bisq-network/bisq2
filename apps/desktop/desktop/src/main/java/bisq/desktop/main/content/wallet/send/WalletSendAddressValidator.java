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

package bisq.desktop.main.content.wallet.send;

import bisq.common.application.DevMode;
import bisq.common.util.StringUtils;
import bisq.common.validation.BitcoinAddressValidation;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

// The app does not know on which network the daemon's wallet runs, so test network addresses pass only in dev mode.
public class WalletSendAddressValidator extends ValidatorBase {
    private static final Pattern TEST_NETWORK_BECH32_PATTERN = Pattern.compile("^(?i)(tb1|bcrt1)[0-9a-z]{6,87}$");
    private static final Pattern TEST_NETWORK_BASE58_PATTERN = Pattern.compile("^[mn2][a-km-zA-HJ-NP-Z1-9]{25,34}$");

    public WalletSendAddressValidator() {
        super(Res.get("validation.invalidBitcoinAddress"));
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        hasErrors.set(canonical(textField.getText(), DevMode.isDevMode()).isEmpty());
    }

    // The address as the daemon gets it: surrounding whitespace and a bitcoin: URI prefix removed, bech32 in lower case.
    static Optional<String> canonical(String address, boolean devMode) {
        if (StringUtils.isEmpty(address) || hasRequiredUriParameter(address)) {
            return Optional.empty();
        }
        String canonical = BitcoinAddressValidation.canonicalize(address.trim());
        if (BitcoinAddressValidation.isValid(canonical)) {
            return Optional.of(canonical);
        }
        if (devMode && TEST_NETWORK_BECH32_PATTERN.matcher(canonical).matches()) {
            return Optional.of(canonical.toLowerCase(Locale.ROOT));
        }
        if (devMode && TEST_NETWORK_BASE58_PATTERN.matcher(canonical).matches()) {
            return Optional.of(canonical);
        }
        return Optional.empty();
    }

    // BIP 21: a URI with a req- parameter which the client does not support is invalid, and this form supports none
    private static boolean hasRequiredUriParameter(String uri) {
        int query = uri.indexOf('?');
        return query >= 0 && Arrays.stream(uri.substring(query + 1).split("&"))
                .anyMatch(parameter -> parameter.toLowerCase(Locale.ROOT).startsWith("req-"));
    }
}
