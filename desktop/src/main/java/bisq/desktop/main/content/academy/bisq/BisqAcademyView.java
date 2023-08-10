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

package bisq.desktop.main.content.academy.bisq;

import bisq.desktop.main.content.academy.AcademyBaseView;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqAcademyView extends AcademyBaseView<BisqAcademyModel, BisqAcademyController> {

    public BisqAcademyView(BisqAcademyModel model, BisqAcademyController controller) {
        super(model, controller);

        Label exchangeDecentralizedHeadline = addHeadlineLabel("exchangeDecentralizedHeadline");
        addContentLabel("exchangeDecentralizedContent");
        Label whyBisqHeadline = addHeadlineLabel("whyBisqHeadline");
        addContentLabel("whyBisqContent");
        Label tradeSafelyHeadline = addHeadlineLabel("tradeSafelyHeadline");
        Label tradeSafelyContent = addContentLabel("tradeSafelyContent");
        addLearnMoreHyperlink();

        setHeadlineMargin(exchangeDecentralizedHeadline);
        setHeadlineMargin(whyBisqHeadline);
        setHeadlineMargin(tradeSafelyHeadline);
        setLastLabelMargin(tradeSafelyContent);
    }

    @Override
    protected String getKey() {
        return "bisq";
    }

    @Override
    protected String getIconId() {
        return "learn-bisq";
    }

    @Override
    protected String getUrl() {
        return "https://bisq.network/";
    }
}
