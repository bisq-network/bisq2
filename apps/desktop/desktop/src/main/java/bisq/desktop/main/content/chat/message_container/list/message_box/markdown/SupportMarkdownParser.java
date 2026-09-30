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

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SupportMarkdownParser {
    private static final Pattern REDUNDANT_SOURCE_TYPE_PATTERN =
            Pattern.compile("^(\\s*)\\[(FAQ|WIKI)]\\s*", Pattern.CASE_INSENSITIVE);

    private SupportMarkdownParser() {
    }

    public static SupportMarkdownDocument parse(String markdown) {
        String value = markdown == null ? "" : markdown;
        String[] lines = value.split("\\R", -1);
        List<SupportMarkdownDocument.Line> parsedLines = new ArrayList<>(lines.length);
        boolean hasFormatting = false;
        for (String line : lines) {
            ParsedLine parsedLine = parseLine(line);
            parsedLines.add(new SupportMarkdownDocument.Line(parsedLine.getLineType(), parsedLine.getSegments()));
            hasFormatting = hasFormatting || parsedLine.hasFormatting;
        }
        return new SupportMarkdownDocument(parsedLines, hasFormatting);
    }

    private static ParsedLine parseLine(String line) {
        if (isHorizontalRule(line)) {
            return new ParsedLine(SupportMarkdownDocument.LineType.HORIZONTAL_RULE, List.of(), true);
        }

        HeadingMatch headingLevel3 = parseHeadingLevel3(line);
        if (headingLevel3 != null) {
            ParsedLine parsedInline = parseInlineTextLine(headingLevel3.text());
            return new ParsedLine(SupportMarkdownDocument.LineType.HEADING_3, parsedInline.getSegments(), true);
        }

        return parseInlineTextLine(line);
    }

    private static ParsedLine parseInlineTextLine(String line) {
        List<SupportMarkdownDocument.Segment> segments = new ArrayList<>();
        StringBuilder plainText = new StringBuilder();
        boolean hasFormatting = false;

        int index = 0;
        while (index < line.length()) {
            if (isEscapedMarkupCharacter(line, index)) {
                plainText.append(line.charAt(index + 1));
                index += 2;
                continue;
            }

            LinkMatch markdownImage = parseMarkdownImage(line, index);
            if (markdownImage != null) {
                flushTextSegment(segments, plainText);
                segments.add(new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.IMAGE,
                        markdownImage.label(),
                        markdownImage.url()));
                hasFormatting = true;
                index = markdownImage.nextIndex();
                continue;
            }

            LinkMatch markdownLink = parseMarkdownLink(line, index);
            if (markdownLink != null) {
                flushTextSegment(segments, plainText);
                segments.add(new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.LINK,
                        markdownLink.label(),
                        markdownLink.url()));
                hasFormatting = true;
                index = markdownLink.nextIndex();
                continue;
            }

            LinkMatch autoLink = parseAutoLink(line, index);
            if (autoLink != null) {
                flushTextSegment(segments, plainText);
                segments.add(new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.LINK,
                        autoLink.label(),
                        autoLink.url()));
                hasFormatting = true;
                index = autoLink.nextIndex();
                continue;
            }

            InlineMatch strong = parseDelimitedInline(line, index, "**", SupportMarkdownDocument.SegmentType.BOLD);
            if (strong != null) {
                flushTextSegment(segments, plainText);
                segments.add(strong.segment());
                hasFormatting = true;
                index = strong.nextIndex();
                continue;
            }

            InlineMatch strike = parseDelimitedInline(line, index, "~~", SupportMarkdownDocument.SegmentType.STRIKETHROUGH);
            if (strike != null) {
                flushTextSegment(segments, plainText);
                segments.add(strike.segment());
                hasFormatting = true;
                index = strike.nextIndex();
                continue;
            }

            InlineMatch inlineCode = parseDelimitedInline(line, index, "`", SupportMarkdownDocument.SegmentType.CODE);
            if (inlineCode != null) {
                flushTextSegment(segments, plainText);
                segments.add(inlineCode.segment());
                hasFormatting = true;
                index = inlineCode.nextIndex();
                continue;
            }

            InlineMatch italicStar = parseDelimitedInline(line, index, "*", SupportMarkdownDocument.SegmentType.ITALIC);
            if (italicStar != null) {
                flushTextSegment(segments, plainText);
                segments.add(italicStar.segment());
                hasFormatting = true;
                index = italicStar.nextIndex();
                continue;
            }

            InlineMatch italicUnderscore = parseDelimitedInline(line, index, "_", SupportMarkdownDocument.SegmentType.ITALIC);
            if (italicUnderscore != null) {
                flushTextSegment(segments, plainText);
                segments.add(italicUnderscore.segment());
                hasFormatting = true;
                index = italicUnderscore.nextIndex();
                continue;
            }

            plainText.append(line.charAt(index));
            index++;
        }

        flushTextSegment(segments, plainText);
        if (segments.isEmpty()) {
            segments.add(new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "", null));
        }

        segments = collapseRedundantSourceTypePrefixes(segments);
        return new ParsedLine(SupportMarkdownDocument.LineType.TEXT, segments, hasFormatting);
    }

    private static HeadingMatch parseHeadingLevel3(String line) {
        int index = 0;
        while (index < line.length() && index < 3 && line.charAt(index) == ' ') {
            index++;
        }
        if (!line.startsWith("###", index)) {
            return null;
        }

        int contentStart = index + 3;
        if (contentStart >= line.length() || !Character.isWhitespace(line.charAt(contentStart))) {
            return null;
        }
        while (contentStart < line.length() && Character.isWhitespace(line.charAt(contentStart))) {
            contentStart++;
        }
        if (contentStart >= line.length()) {
            return null;
        }
        return new HeadingMatch(line.substring(contentStart));
    }

    private static List<SupportMarkdownDocument.Segment> collapseRedundantSourceTypePrefixes(List<SupportMarkdownDocument.Segment> segments) {
        List<SupportMarkdownDocument.Segment> normalized = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            SupportMarkdownDocument.Segment current = segments.get(i);
            if (!isSupportSourceIcon(current) || i + 1 >= segments.size()) {
                normalized.add(current);
                continue;
            }

            SupportMarkdownDocument.Segment next = segments.get(i + 1);
            if (next.type() != SupportMarkdownDocument.SegmentType.TEXT) {
                normalized.add(current);
                continue;
            }

            String cleaned = REDUNDANT_SOURCE_TYPE_PATTERN.matcher(next.text()).replaceFirst("$1");
            normalized.add(current);
            if (!cleaned.isEmpty()) {
                normalized.add(new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, cleaned, null));
            }
            i++;
        }
        return normalized;
    }

    private static InlineMatch parseDelimitedInline(String line,
                                                    int startIndex,
                                                    String delimiter,
                                                    SupportMarkdownDocument.SegmentType segmentType) {
        if (!line.startsWith(delimiter, startIndex)) {
            return null;
        }
        int contentStart = startIndex + delimiter.length();
        if (contentStart >= line.length()) {
            return null;
        }

        int contentEnd = line.indexOf(delimiter, contentStart);
        if (contentEnd <= contentStart) {
            return null;
        }

        String content = line.substring(contentStart, contentEnd);
        if (content.isBlank()) {
            return null;
        }

        return new InlineMatch(
                new SupportMarkdownDocument.Segment(segmentType, content, null),
                contentEnd + delimiter.length());
    }

    private static LinkMatch parseMarkdownLink(String line, int startIndex) {
        if (line.charAt(startIndex) != '[') {
            return null;
        }

        int textEnd = line.indexOf(']', startIndex + 1);
        if (textEnd < 0 || textEnd + 1 >= line.length() || line.charAt(textEnd + 1) != '(') {
            return null;
        }

        int urlEnd = line.indexOf(')', textEnd + 2);
        if (urlEnd < 0) {
            return null;
        }

        String label = line.substring(startIndex + 1, textEnd).trim();
        String url = line.substring(textEnd + 2, urlEnd).trim();
        if (label.isEmpty() || !MarkdownSecurityUtils.isSafeHttpUrl(url)) {
            return null;
        }

        return new LinkMatch(label, url, urlEnd + 1);
    }

    private static LinkMatch parseMarkdownImage(String line, int startIndex) {
        if (startIndex + 1 >= line.length() || line.charAt(startIndex) != '!' || line.charAt(startIndex + 1) != '[') {
            return null;
        }

        int textEnd = line.indexOf(']', startIndex + 2);
        if (textEnd < 0 || textEnd + 1 >= line.length() || line.charAt(textEnd + 1) != '(') {
            return null;
        }

        int urlEnd = line.indexOf(')', textEnd + 2);
        if (urlEnd < 0) {
            return null;
        }

        String alt = line.substring(startIndex + 2, textEnd).trim();
        String url = line.substring(textEnd + 2, urlEnd).trim();
        if (!isSupportedImageUrl(url)) {
            return null;
        }

        return new LinkMatch(alt, url, urlEnd + 1);
    }

    private static LinkMatch parseAutoLink(String line, int startIndex) {
        if (!line.startsWith("http://", startIndex) && !line.startsWith("https://", startIndex)) {
            return null;
        }

        int end = startIndex;
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
            end++;
        }

        String rawUrl = line.substring(startIndex, end);
        String normalized = trimTrailingUrlPunctuation(rawUrl);
        if (normalized.isEmpty() || !MarkdownSecurityUtils.isSafeHttpUrl(normalized)) {
            return null;
        }

        return new LinkMatch(normalized, normalized, startIndex + normalized.length());
    }

    private static String trimTrailingUrlPunctuation(String rawUrl) {
        String value = rawUrl;
        while (!value.isEmpty()) {
            char last = value.charAt(value.length() - 1);
            if (last == '.' || last == ',' || last == ';' || last == ':' || last == '!' || last == '?') {
                value = value.substring(0, value.length() - 1);
                continue;
            }
            break;
        }
        return value;
    }

    private static boolean isSupportedImageUrl(String url) {
        return "bisq-icon://faq".equalsIgnoreCase(url) || "bisq-icon://wiki".equalsIgnoreCase(url);
    }

    private static boolean isSupportSourceIcon(SupportMarkdownDocument.Segment segment) {
        return segment.type() == SupportMarkdownDocument.SegmentType.IMAGE && isSupportedImageUrl(segment.url());
    }

    private static boolean isHorizontalRule(String line) {
        String trimmed = line.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        char first = trimmed.charAt(0);
        if (first != '-' && first != '*' && first != '_') {
            return false;
        }
        for (int i = 1; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEscapedMarkupCharacter(String line, int index) {
        if (line.charAt(index) != '\\' || index + 1 >= line.length()) {
            return false;
        }
        char next = line.charAt(index + 1);
        return next == '\\' || next == '*' || next == '_' || next == '~' || next == '`'
                || next == '[' || next == ']' || next == '(' || next == ')' || next == '!';
    }

    private static void flushTextSegment(List<SupportMarkdownDocument.Segment> segments, StringBuilder plainText) {
        if (plainText.isEmpty()) {
            return;
        }
        segments.add(new SupportMarkdownDocument.Segment(
                SupportMarkdownDocument.SegmentType.TEXT,
                plainText.toString(),
                null));
        plainText.setLength(0);
    }

    private record InlineMatch(SupportMarkdownDocument.Segment segment,
                               int nextIndex) {
    }

    private record LinkMatch(String label,
                             String url,
                             int nextIndex) {
    }

    private record HeadingMatch(String text) {
    }

    @Getter
    private static final class ParsedLine {
        private final SupportMarkdownDocument.LineType lineType;
        private final List<SupportMarkdownDocument.Segment> segments;
        private final boolean hasFormatting;

        private ParsedLine(SupportMarkdownDocument.LineType lineType,
                           List<SupportMarkdownDocument.Segment> segments,
                           boolean hasFormatting) {
            this.lineType = lineType;
            this.segments = segments;
            this.hasFormatting = hasFormatting;
        }
    }
}
