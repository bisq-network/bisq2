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

import javax.annotation.Nullable;
import java.util.List;

public record SupportMarkdownDocument(List<Line> lines,
                                      boolean hasMarkdownFormatting) {
    public SupportMarkdownDocument {
        lines = List.copyOf(lines);
    }

    public record Line(LineType type,
                       List<Segment> segments) {
        public Line {
            if (type == null) {
                type = LineType.TEXT;
            }
            segments = List.copyOf(segments);
        }
    }

    public record Segment(SegmentType type,
                          String text,
                          @Nullable String url) {
    }

    public enum SegmentType {
        TEXT,
        BOLD,
        ITALIC,
        STRIKETHROUGH,
        CODE,
        LINK,
        IMAGE
    }

    public enum LineType {
        TEXT,
        HEADING_3,
        HORIZONTAL_RULE
    }
}
