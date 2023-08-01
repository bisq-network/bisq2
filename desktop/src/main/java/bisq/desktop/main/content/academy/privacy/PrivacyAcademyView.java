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

package bisq.desktop.main.content.academy.privacy;

import bisq.desktop.main.content.academy.AcademyView;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivacyAcademyView extends AcademyView<PrivacyAcademyModel, PrivacyAcademyController> {

    public PrivacyAcademyView(PrivacyAcademyModel model, PrivacyAcademyController controller) {
        super(model, controller);

        Label introContent = addContentLabel("introContent");
        Label whyPrivacyHeadline = addHeadlineLabel("whyPrivacyHeadline");
        Label whyPrivacyContent = addContentLabel("whyPrivacyContent");
        Label giveUpPrivacyHeadline = addHeadlineLabel("giveUpPrivacyHeadline");
        Label giveUpPrivacyContent = addContentLabel("giveUpPrivacyContent");
        Label bisqProtectsPrivacyHeadline = addHeadlineLabel("bisqProtectsPrivacyHeadline");
        Label bisqProtectsPrivacyContent = addContentLabel("bisqProtectsPrivacyContent");

        Hyperlink learnMore = addLearnMoreHyperlink();

        VBox.setMargin(introContent, new Insets(25, 0, 0, 0));
        VBox.setMargin(whyPrivacyHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(giveUpPrivacyHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(bisqProtectsPrivacyHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(bisqProtectsPrivacyContent, new Insets(0, 0, 15, 0));
    }

    @Override
    protected String getKey() {
        return "privacy";
    }

    @Override
    protected String getIconId() {
        return "learn-privacy";
    }


    @Override
    protected String getUrl() {
        return "https://bitcoin.org/en/protect-your-privacy";
    }
}
