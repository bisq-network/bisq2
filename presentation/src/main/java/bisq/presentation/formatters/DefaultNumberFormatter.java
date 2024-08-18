package bisq.presentation.formatters;

import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Slf4j
public class DefaultNumberFormatter {
    /**
     * To avoid confusion from different number formats from different locales we provide a default number format.
     * This contains a '.' as decimal separator and a space as group separator. It supports max. 8 fraction digits and
     * RoundingMode.DOWN is used (equal to cut off 9th digit).
     */
    public static final DecimalFormat DEFAULT_NUMBER_FORMAT = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
    public static final DecimalFormatSymbols DEFAULT_SEPARATORS = new DecimalFormatSymbols();

    static {
        DEFAULT_SEPARATORS.setGroupingSeparator(' ');
        DEFAULT_SEPARATORS.setDecimalSeparator('.');
        DEFAULT_NUMBER_FORMAT.setDecimalFormatSymbols(DEFAULT_SEPARATORS);
        DEFAULT_NUMBER_FORMAT.setRoundingMode(RoundingMode.DOWN);
        DEFAULT_NUMBER_FORMAT.setMaximumFractionDigits(8);
    }

    /**
     * Reformat a numeric string from users locale format or any other valid number format to the DEFAULT_NUMBER_FORMAT
     *
     * @param numberValue A numeric string value.
     * @return The formatted number according to the DEFAULT_NUMBER_FORMAT
     */
    public static String reformat(String numberValue) {
        return format(MathUtils.parseToDouble(numberValue));
    }

    public static String format(Number value) {
        return DEFAULT_NUMBER_FORMAT.format(value);
    }

    public static String format(double value, DecimalFormat decimalFormat) {
        return decimalFormat.format(value);
    }
}
