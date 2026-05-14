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
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class SupportMarkdownRendererTest {
    @Test
    @DisplayName("maps faq and wiki source icons to chat and book glyphs")
    void maps_faq_and_wiki_source_icons_to_chat_and_book_glyphs() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://faq"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://wiki"))
                .isEqualTo("nav-learn");
    }

    @Test
    @DisplayName("source icon mapping is case insensitive and safe for unknown input")
    void source_icon_mapping_is_case_insensitive_and_safe_for_unknown_input() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("BISQ-ICON://FAQ"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://unknown"))
                .isNull();
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl(null))
                .isNull();
    }
}
