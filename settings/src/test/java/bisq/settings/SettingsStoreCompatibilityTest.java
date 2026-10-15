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

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.proto.UnresolvableProtobufException;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.UnknownFieldSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A settings file of version 2.1.13 must load with this version, and a field number must keep the type and the
 * meaning which it has in version 2.1.13. The tests write the bytes of the SettingsStore message of version 2.1.13
 * field by field, with the numbers and types of that version. They do not use the generated class of this
 * version, because that class would write the layout of this version.
 */
class SettingsStoreCompatibilityTest {
    private static final Market WALLET_MARKET = new Market("BTC", "CHF", "Bitcoin", "Swiss Franc");
    private static final Market SELECTED_MU_SIG_MARKET = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Market FAVOURITE_MARKET = new Market("BTC", "GBP", "Bitcoin", "British Pound");
    private static final Market MU_SIG_FIAT_MARKET = new Market("BTC", "SEK", "Bitcoin", "Swedish Krona");
    private static final Market MU_SIG_OTHER_MARKET = new Market("LTC", "BTC", "Litecoin", "Bitcoin");

    // Field 30 of version 2.1.13 is a map. Version 2.1.13 fills it only after the MuSig offerbook was used, which
    // needs the dev mode. Release users have no entry in it.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void storeOfVersion2_1_13LoadsWithItsWalletMarketAndItsOtherSettings(boolean withMuSigMarketMapEntry)
            throws Exception {
        SettingsStore store = load(storeOfVersion2_1_13(withMuSigMarketMapEntry));

        assertThat(store.selectedWalletMarket.get()).isEqualTo(WALLET_MARKET);
        // Version 2.1.13 has no field for these two markets. Its wallet market and its map are not read as such.
        assertThat(store.muSigLastSelectedFiatMarket.get()).isEqualTo(MarketRepository.getDefaultBtcFiatMarket());
        assertThat(store.muSigLastSelectedOtherMarket.get()).isEqualTo(MarketRepository.getDefaultCryptoBtcMarket());

        assertThat(store.dontShowAgainMap).isEqualTo(Map.of("dontShowAgainKey", true));
        assertThat(store.useAnimations.get()).isTrue();
        assertThat(store.selectedMuSigMarket.get()).isEqualTo(SELECTED_MU_SIG_MARKET);
        assertThat(store.bisqEasyTradeRulesConfirmed.get()).isTrue();
        assertThat(store.chatNotificationType.get()).isEqualTo(ChatNotificationType.MENTION);
        assertThat(store.isTacAccepted.get()).isTrue();
        assertThat(Set.copyOf(store.consumedAlertIds)).isEqualTo(Set.of("alertId"));
        assertThat(store.closeMyOfferWhenTaken.get()).isTrue();
        assertThat(store.languageTag.get()).isEqualTo("de");
        assertThat(store.preventStandbyMode.get()).isTrue();
        assertThat(Set.copyOf(store.supportedLanguageTags)).isEqualTo(Set.of("de", "en"));
        assertThat(store.difficultyAdjustmentFactor.get()).isEqualTo(2.0);
        assertThat(Set.copyOf(store.favouriteMarkets)).isEqualTo(Set.of(FAVOURITE_MARKET));
        assertThat(store.maxTradePriceDeviation.get()).isEqualTo(0.07);
        assertThat(store.showBuyOffers.get()).isTrue();
        assertThat(store.backupLocation.get()).isEqualTo("/backup");
        assertThat(store.totalMaxBackupSizeInMB.get()).isEqualTo(50.0);
        assertThat(store.bisqEasyOfferbookMessageTypeFilter.get()).isEqualTo(ChatMessageType.OFFER);
        assertThat(store.numDaysAfterRedactingTradeData.get()).isEqualTo(60);
        assertThat(store.muSigActivated.get()).isTrue();
        assertThat(store.autoAddToContactsList.get()).isTrue();
        assertThat(store.muSigTradeRulesConfirmed.get()).isTrue();
    }

    // Version 2.1.13 reads field 31 as the wallet market and field 30 as a map. So a settings file of this version
    // stays readable for version 2.1.13 only if it has the wallet market in field 31 and nothing in field 30.
    @Test
    void walletMarketIsWrittenToField31AndField30IsNotWritten() throws Exception {
        UnknownFieldSet fields = UnknownFieldSet.parseFrom(storeOfThisVersion().toAny().getValue());

        assertThat(marketOf(fields, 31)).isEqualTo(WALLET_MARKET);
        assertThat(fields.hasField(30)).isFalse();
        // The two MuSig markets use numbers which version 2.1.13 does not know, so it ignores them
        assertThat(marketOf(fields, 38)).isEqualTo(MU_SIG_FIAT_MARKET);
        assertThat(marketOf(fields, 39)).isEqualTo(MU_SIG_OTHER_MARKET);
    }

    @Test
    void storeOfThisVersionKeepsItsMarketsWhenItIsLoadedAgain() throws Exception {
        SettingsStore store = load(storeOfThisVersion().toAny().getValue().toByteArray());

        assertThat(store.selectedWalletMarket.get()).isEqualTo(WALLET_MARKET);
        assertThat(store.muSigLastSelectedFiatMarket.get()).isEqualTo(MU_SIG_FIAT_MARKET);
        assertThat(store.muSigLastSelectedOtherMarket.get()).isEqualTo(MU_SIG_OTHER_MARKET);
    }

    private static SettingsStore storeOfThisVersion() {
        SettingsStore store = new SettingsStore();
        store.selectedWalletMarket.set(WALLET_MARKET);
        store.muSigLastSelectedFiatMarket.set(MU_SIG_FIAT_MARKET);
        store.muSigLastSelectedOtherMarket.set(MU_SIG_OTHER_MARKET);
        return store;
    }

    // The persistence layer stores the SettingsStore message in an Any and loads it with the resolver of the store
    private static SettingsStore load(byte[] storeBytes) throws UnresolvableProtobufException {
        Any any = Any.newBuilder()
                .setTypeUrl("type.googleapis.com/settings.SettingsStore")
                .setValue(ByteString.copyFrom(storeBytes))
                .build();
        return (SettingsStore) new SettingsStore().getResolver().fromAny(any);
    }

    private static Market marketOf(UnknownFieldSet fields, int fieldNumber) throws Exception {
        ByteString marketBytes = fields.getField(fieldNumber).getLengthDelimitedList().get(0);
        return Market.fromProto(bisq.common.protobuf.Market.parseFrom(marketBytes));
    }

    // The fields of the SettingsStore message of version 2.1.13, in the order of their numbers. Protobuf does not
    // write a field which has the default value of its type, so the fields 6, 16, 18, 21, 22 and 24 (all false)
    // are not in the bytes.
    private static byte[] storeOfVersion2_1_13(boolean withMuSigMarketMapEntry) throws IOException {
        return message(out -> {
            out.writeByteArray(1, new byte[0]); // Cookie cookie
            out.writeByteArray(2, message(entry -> { // map<string, bool> dontShowAgainMap
                entry.writeString(1, "dontShowAgainKey");
                entry.writeBool(2, true);
            }));
            out.writeBool(3, true); // bool useAnimations
            out.writeByteArray(4, market(SELECTED_MU_SIG_MARKET)); // common.Market selectedMuSigMarket
            out.writeSInt64(5, 30000); // sint64 minRequiredReputationScore
            out.writeBool(7, true); // bool bisqEasyTradeRulesConfirmed
            out.writeEnum(8, 2); // ChatNotificationType chatNotificationType = CHATNOTIFICATIONTYPE_MENTION
            out.writeBool(9, true); // bool isTacAccepted
            out.writeString(10, "alertId"); // repeated string consumedAlertIds
            out.writeBool(11, true); // bool closeMyOfferWhenTaken
            out.writeString(12, "de"); // string languageTag
            out.writeBool(13, true); // bool preventStandbyMode
            out.writeString(14, "de"); // repeated string supportedLanguageTags
            out.writeString(14, "en");
            out.writeDouble(15, 2.0); // double difficultyAdjustmentFactor
            out.writeByteArray(17, market(FAVOURITE_MARKET)); // repeated common.Market favouriteMarkets
            out.writeDouble(19, 0.07); // double maxTradePriceDeviation
            out.writeBool(20, true); // bool showBuyOffers
            out.writeString(23, "/backup"); // string backupLocation
            out.writeDouble(25, 50); // double totalMaxBackupSizeInMB
            out.writeEnum(26, 2); // ChatMessageType bisqEasyOfferbookMessageTypeFilter = CHATMESSAGETYPE_OFFER
            out.writeSInt32(27, 60); // sint32 numDaysAfterRedactingTradeData
            out.writeBool(28, true); // bool muSigActivated
            out.writeBool(29, true); // bool autoAddToContactsList
            if (withMuSigMarketMapEntry) {
                // map<string, common.Market> muSigLastSelectedMarketByBaseCurrencyMap
                out.writeByteArray(30, message(entry -> {
                    entry.writeString(1, "BTC");
                    entry.writeByteArray(2, market(SELECTED_MU_SIG_MARKET));
                }));
            }
            out.writeByteArray(31, market(WALLET_MARKET)); // common.Market selectedWalletMarket
            out.writeBool(32, true); // bool muSigTradeRulesConfirmed
        });
    }

    // The message common.Market is the same in version 2.1.13 and in this version
    private static byte[] market(Market market) throws IOException {
        return message(out -> {
            out.writeString(1, market.getBaseCurrencyCode());
            out.writeString(2, market.getQuoteCurrencyCode());
            out.writeString(3, market.getBaseCurrencyName());
            out.writeString(4, market.getQuoteCurrencyName());
        });
    }

    private static byte[] message(FieldWriter fieldWriter) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        fieldWriter.write(out);
        out.flush();
        return bytes.toByteArray();
    }

    @FunctionalInterface
    private interface FieldWriter {
        void write(CodedOutputStream out) throws IOException;
    }
}
