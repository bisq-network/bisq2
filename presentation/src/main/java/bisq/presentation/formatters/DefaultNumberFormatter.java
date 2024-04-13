package bisq.presentation.formatters;

import bisq.common.locale.LocaleRepository;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
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
        return reformat(numberValue, LocaleRepository.getDefaultLocale());
    }

    @VisibleForTesting
    static String reformat(String numberValue, Locale inputLocale) {
        double inputNumberValue = parseToDoubleWithNumberFormat(numberValue, inputLocale);
        return formatDouble(inputNumberValue, DEFAULT_NUMBER_FORMAT);
    }

    @VisibleForTesting
    static String formatDouble(double value, DecimalFormat decimalFormat) {
        return decimalFormat.format(value);
    }

    @VisibleForTesting
    static double parseToDoubleWithNumberFormat(String value, Locale inputLocale) {
        NumberFormat numberFormat = NumberFormat.getInstance(inputLocale);
        try {
            return numberFormat.parse(value).doubleValue();
        } catch (ParseException e) {
            log.warn("Could not parse {} with locale {} because of: {}. We try to parse to double instead.",
                    value, inputLocale, ExceptionUtil.getMessageOrToString(e));
            return clearAndParseToDouble(value); // throws if not a number
        }
    }

    // This is unsafe if not checked first for the input locale.
    // E.g. 123,456 would become 123.456 but if the input was English locale where the ',' is a thousand separator
    // we would have converted it to a decimal separator
    @VisibleForTesting
    static double clearAndParseToDouble(String value) {
        String cleaned = StringUtils.removeAllWhitespaces(value)
                .replace(",", ".");
        return Double.parseDouble(cleaned);
    }

}
