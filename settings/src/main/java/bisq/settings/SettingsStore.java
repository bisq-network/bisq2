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

package bisq.settings;

import bisq.common.monetary.Market;
import bisq.common.monetary.MarketRepository;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class SettingsStore implements PersistableStore<SettingsStore> {
    private final Cookie cookie;
    private DisplaySettings displaySettings = new DisplaySettings();
    private final Map<String, Boolean> dontShowAgainMap = new HashMap<>();
    private final List<Market> markets;
    @Setter
    private Market selectedMarket;

    public SettingsStore() {
        cookie = new Cookie();
        markets = MarketRepository.getMajorMarkets();
        selectedMarket = MarketRepository.getDefault();
    }

    public SettingsStore(Cookie cookie,
                         DisplaySettings displaySettings,
                         Map<String, Boolean> dontShowAgainMap,
                         List<Market> markets,
                         Market selectedMarket) {
        this.cookie = cookie;
        this.displaySettings = displaySettings;
        this.dontShowAgainMap.putAll(dontShowAgainMap);
        this.markets = markets;
        this.selectedMarket = selectedMarket;
    }

    @Override
    public bisq.settings.protobuf.SettingsStore toProto() {
        return bisq.settings.protobuf.SettingsStore.newBuilder()
                .setCookie(cookie.toProto())
                .setDisplaySettings(displaySettings.toProto())
                .putAllDontShowAgainMap(dontShowAgainMap)
                .addAllMarkets(markets.stream().map(Market::toProto).collect(Collectors.toList()))
                .setSelectedMarket(selectedMarket.toProto())
                .build();
    }

    public static SettingsStore fromProto(bisq.settings.protobuf.SettingsStore proto) {
        return new SettingsStore(Cookie.fromProto(proto.getCookie()),
                DisplaySettings.fromProto((proto.getDisplaySettings())),
                proto.getDontShowAgainMapMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                proto.getMarketsList().stream().map(Market::fromProto).collect(Collectors.toList()),
                Market.fromProto(proto.getSelectedMarket()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.settings.protobuf.SettingsStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public SettingsStore getClone() {
        return new SettingsStore(cookie,
                displaySettings,
                dontShowAgainMap,
                markets,
                selectedMarket);
    }

    @Override
    public void applyPersisted(SettingsStore persisted) {
        cookie.putAll(persisted.getCookie().getMap());
        displaySettings = persisted.getDisplaySettings();
        dontShowAgainMap.putAll(persisted.getDontShowAgainMap());
        markets.clear();
        markets.addAll(persisted.getMarkets());
        selectedMarket = persisted.getSelectedMarket();
    }
}