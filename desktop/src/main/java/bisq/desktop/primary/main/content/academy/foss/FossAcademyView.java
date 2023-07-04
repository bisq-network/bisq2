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

package bisq.desktop.primary.main.content.academy.foss;

import bisq.desktop.primary.main.content.academy.AcademyView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FossAcademyView extends AcademyView<FossAcademyModel, FossAcademyController> {

    public FossAcademyView(FossAcademyModel model, FossAcademyController controller) {
        super(model, controller);
    }

    @Override
    protected String getKey() {
        return "foss";
    }

    @Override
    protected String getIconId() {
        return "learn-openSource";
    }


    @Override
    protected String getUrl() {
        return "https://en.wikipedia.org/wiki/Free_and_open-source_software";
    }
}
