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

package bisq.desktop.main.content.academy.bitcoin;

import bisq.desktop.main.content.academy.AcademyBaseView;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BitcoinAcademyView extends AcademyBaseView<BitcoinAcademyModel, BitcoinAcademyController> {

    public BitcoinAcademyView(BitcoinAcademyModel model, BitcoinAcademyController controller) {
        super(model, controller);

        addSubHeadlineLabel("academy.bitcoin.subHeadline");

        Label whatIsBitcoinHeadline = addHeadlineLabel("academy.bitcoin.whatIsBitcoinHeadline");
        addContentLabel("academy.bitcoin.whatIsBitcoinContent");
        Label whyUseBitcoinHeadline = addHeadlineLabel("academy.bitcoin.whyUseBitcoinHeadline");
        Label whyUseBitcoinContent = addContentLabel("academy.bitcoin.whyUseBitcoinContent");
        addLearnMoreHyperlink();

        setHeadlineMargin(whatIsBitcoinHeadline);
        setHeadlineMargin(whyUseBitcoinHeadline);
        setLastLabelMargin(whyUseBitcoinContent);
    }

    @Override
    protected String getKey() {
        return "bitcoin";
    }

    @Override
    protected String getIconId() {
        return "learn-bitcoin";
    }

    @Override
    protected String getUrl() {
        return "https://bitcoin.org";
    }
}
