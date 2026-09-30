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

package bisq.desktop.main.content.chat.message_container.list.message_box.markdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportMarkdownRendererTest {
    @Test
    void mapsFaqAndWikiSourceIconsToChatAndBookGlyphs() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://faq"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://wiki"))
                .isEqualTo("nav-learn");
    }

    @Test
    void sourceIconMappingIsCaseInsensitiveAndSafeForUnknownInput() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("BISQ-ICON://FAQ"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://unknown"))
                .isNull();
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl(null))
                .isNull();
    }
}
