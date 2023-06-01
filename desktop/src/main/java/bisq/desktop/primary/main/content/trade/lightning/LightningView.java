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

package bisq.desktop.primary.main.content.trade.lightning;

import bisq.account.protocol_type.ProtocolType;
import bisq.desktop.primary.main.content.trade.ProtocolRoadmapView;

public class LightningView extends ProtocolRoadmapView<LightningModel, LightningController> {

    public LightningView(LightningModel model, LightningController controller) {
        super(model, controller);
    }

    @Override
    protected String getKey() {
        return ProtocolType.LIGHTNING_X.name();
    }

    @Override
    protected String getIconId() {
        return "protocol-lightning";
    }

    @Override
    protected String getUrl() {
        return "https://github.com/bisq-network/proposals/issues/373";
    }
}
