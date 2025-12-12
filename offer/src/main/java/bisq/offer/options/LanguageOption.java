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

package bisq.offer.options;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class LanguageOption implements OfferOption {

    private final List<String> supportedLanguageCodes;

    public LanguageOption(List<String> supportedLanguageCodes) {
        this.supportedLanguageCodes = supportedLanguageCodes;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(supportedLanguageCodes.size() < 10,
                "supportedLanguageCodes must be < 10");
        checkArgument(supportedLanguageCodes.toString().length() < 100,
                "supportedLanguageCodes.toString().length() must be < 100");
    }

    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        return getOfferOptionBuilder(serializeForHash)
                .setLanguageOption(bisq.offer.protobuf.LanguageOption.newBuilder()
                        .addAllSupportedLanguageCodes(supportedLanguageCodes));
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static LanguageOption fromProto(bisq.offer.protobuf.LanguageOption proto) {
        return new LanguageOption(new ArrayList<>(proto.getSupportedLanguageCodesList()));
    }
}