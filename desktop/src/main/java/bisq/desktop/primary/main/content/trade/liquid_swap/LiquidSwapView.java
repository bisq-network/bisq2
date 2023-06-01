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

package bisq.desktop.primary.main.content.trade.liquid_swap;

import bisq.account.protocol.SwapProtocolType;
import bisq.desktop.primary.main.content.trade.ProtocolRoadmapView;

public class LiquidSwapView extends ProtocolRoadmapView<LiquidSwapModel, LiquidSwapController> {
    public LiquidSwapView(LiquidSwapModel model, LiquidSwapController controller) {
        super(model, controller);
    }

    @Override
    protected String getKey() {
        return SwapProtocolType.LIQUID_SWAP.name();
    }

    @Override
    protected String getIconId() {
        return "protocol-liquid";
    }

    @Override
    protected String getUrl() {
        return "https://www.blockstream.com/liquid/";
    }
}
