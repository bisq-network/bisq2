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


package bisq.api.dto.chat;

import bisq.common.json.JsonMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one Jackson behavior {@code ChatRequestValidation#citationError} depends on without a
 * null-guard: an {@code Optional} record component deserializes to {@code Optional.empty()}, never
 * Java {@code null}, whether the JSON omits the property or spells out {@code null}. The other tests
 * construct {@link CitationDto} in Java, so only this one exercises the wire path a real client body
 * takes — if a Jackson upgrade ever changes the absent-property answer, this is what fails instead of
 * a request 500ing.
 */
class CitationDtoJsonTest {
    @Test
    void anAbsentChatMessageIdDeserializesToEmptyNotNull() throws Exception {
        CitationDto dto = JsonMapperProvider.get().readValue(
                "{\"authorUserProfileId\":\"author\",\"text\":\"quoted\"}", CitationDto.class);

        assertThat(dto.chatMessageId()).isNotNull().isEmpty();
    }

    @Test
    void anExplicitlyNullChatMessageIdDeserializesToEmptyNotNull() throws Exception {
        CitationDto dto = JsonMapperProvider.get().readValue(
                "{\"authorUserProfileId\":\"author\",\"text\":\"quoted\",\"chatMessageId\":null}", CitationDto.class);

        assertThat(dto.chatMessageId()).isNotNull().isEmpty();
    }
}
