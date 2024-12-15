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

package bisq.desktop.main.content.bisq_easy.components;

public class TextInputFontUtils {
    private final static String INPUT_TEXT_9_STYLE_CLASS = "input-text-9";
    private final static String INPUT_TEXT_10_STYLE_CLASS = "input-text-10";
    private final static String INPUT_TEXT_11_STYLE_CLASS = "input-text-11";
    private final static String INPUT_TEXT_12_STYLE_CLASS = "input-text-12";
    private final static String INPUT_TEXT_13_STYLE_CLASS = "input-text-13";
    private final static String INPUT_TEXT_14_STYLE_CLASS = "input-text-14";
    private final static String INPUT_TEXT_15_STYLE_CLASS = "input-text-15";
    private final static String INPUT_TEXT_16_STYLE_CLASS = "input-text-16";
    private final static String INPUT_TEXT_17_STYLE_CLASS = "input-text-17";
    private final static String INPUT_TEXT_18_STYLE_CLASS = "input-text-18";
    private final static String INPUT_TEXT_19_STYLE_CLASS = "input-text-19";

    public static String getFontStyleBasedOnTextLength(int charCount) {
        if (charCount < 10) {
            return INPUT_TEXT_9_STYLE_CLASS;
        }
        if (charCount == 10) {
            return INPUT_TEXT_10_STYLE_CLASS;
        }
        if (charCount == 11) {
            return INPUT_TEXT_11_STYLE_CLASS;
        }
        if (charCount == 12) {
            return INPUT_TEXT_12_STYLE_CLASS;
        }
        if (charCount == 13) {
            return INPUT_TEXT_13_STYLE_CLASS;
        }
        if (charCount == 14) {
            return INPUT_TEXT_14_STYLE_CLASS;
        }
        if (charCount == 15) {
            return INPUT_TEXT_15_STYLE_CLASS;
        }
        if (charCount == 16) {
            return INPUT_TEXT_16_STYLE_CLASS;
        }
        if (charCount == 17) {
            return INPUT_TEXT_17_STYLE_CLASS;
        }
        if (charCount == 18) {
            return INPUT_TEXT_18_STYLE_CLASS;
        }
        return INPUT_TEXT_19_STYLE_CLASS;
    }
}
