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

package bisq.desktop.main.content.academy.wallets;

import bisq.desktop.main.content.academy.AcademyView;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletsAcademyView extends AcademyView<WalletsAcademyModel, WalletsAcademyController> {

    public WalletsAcademyView(WalletsAcademyModel model, WalletsAcademyController controller) {
        super(model, controller);
        
        Label whatIsAWalletHeadline = addHeadlineLabel("whatIsAWalletHeadline");
        Label whatIsAWalletContent = addContentLabel("whatIsAWalletContent");
        Label howToPickHeadline = addHeadlineLabel("howToPickHeadline");
        Label howToPickContent = addContentLabel("howToPickContent");

        Hyperlink learnMore = addLearnMoreHyperlink();
        
        VBox.setMargin(whatIsAWalletHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(howToPickHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(howToPickContent, new Insets(0, 0, 15, 0));
    }

    @Override
    protected String getKey() {
        return "wallets";
    }

    @Override
    protected String getIconId() {
        return "learn-wallets";
    }


    @Override
    protected String getUrl() {
        return "https://bitcoin.org/en/choose-your-wallet";
    }
}
