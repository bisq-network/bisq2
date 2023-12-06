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

import bisq.desktop.main.content.academy.AcademyBaseView;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivacyAcademyView extends AcademyBaseView<PrivacyAcademyModel, PrivacyAcademyController> {

    public PrivacyAcademyView(PrivacyAcademyModel model, PrivacyAcademyController controller) {
        super(model, controller);

        addSubHeadlineLabel("academy.privacy.subHeadline");

        Label introContent = addContentLabel("academy.privacy.introContent");
        Label whyPrivacyHeadline = addHeadlineLabel("academy.privacy.whyPrivacyHeadline");
        addContentLabel("academy.privacy.whyPrivacyContent");
        Label giveUpPrivacyHeadline = addHeadlineLabel("academy.privacy.giveUpPrivacyHeadline");
        addContentLabel("academy.privacy.giveUpPrivacyContent");
        Label bisqProtectsPrivacyHeadline = addHeadlineLabel("academy.privacy.bisqProtectsPrivacyHeadline");
        Label bisqProtectsPrivacyContent = addContentLabel("academy.privacy.bisqProtectsPrivacyContent");
        addLearnMoreHyperlink();

        setHeadlineMargin(introContent);
        setHeadlineMargin(whyPrivacyHeadline);
        setHeadlineMargin(giveUpPrivacyHeadline);
        setHeadlineMargin(bisqProtectsPrivacyHeadline);
        setLastLabelMargin(bisqProtectsPrivacyContent);
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
