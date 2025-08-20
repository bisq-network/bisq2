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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.backup;

import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.SeedState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class SetupWalletWizardBackupModel implements Model {
    private final int SEED_WORD_COUNT = 12;
    private final StringProperty[] seedWords = new StringProperty[SEED_WORD_COUNT];
    private final ObjectProperty<SeedState> seedState = new SimpleObjectProperty<>(SeedState.LOADING);

    public SetupWalletWizardBackupModel() {
        for (int i = 0; i < SEED_WORD_COUNT; i++) {
            seedWords[i] = new SimpleStringProperty("");
        }
    }
}

