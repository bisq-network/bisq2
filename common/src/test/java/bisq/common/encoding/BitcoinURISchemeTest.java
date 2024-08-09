package bisq.common.encoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitcoinURISchemeTest {
    @Test
    public void testExtractBitcoinAddress() {
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?amount=1234"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?amount=1234&message=mymsg"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?message=mymsg"));
        assertEquals("BC1Q6MX5CGJ3V2JSKGKPP3K4HMESGKK265WGGSLWXKWEM0ESDV2YF72QHAFCFB", BitcoinURIScheme.extractBitcoinAddress("BITCOIN:BC1Q6MX5CGJ3V2JSKGKPP3K4HMESGKK265WGGSLWXKWEM0ESDV2YF72QHAFCFB"));
    }
}