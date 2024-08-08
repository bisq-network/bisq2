package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LightningInvoiceValidationTest {
    @Test
    public void testValidateLightningInvoiceHash() {
        assertTrue(LightningInvoiceValidation.validateInvoice("lnbc20m1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqhp58yjmdan79s6qqdhdzgynm4zwqd5d7xmw5fk98klysy043l2ahrqsfpp3qjmp7lwpagxun9pygexvgpjdc4jdj85fr9yq20q82gphp2nflc7jtzrcazrra7wwgzxqc8u7754cdlpfrmccae92qgzqvzq2ps8pqqqqqqpqqqqq9qqqvpeuqafqxu92d8lr6fvg0r5gv0heeeqgcrqlnm6jhphu9y00rrhy4grqszsvpcgpy9qqqqqqgqqqqq7qqzqj9n4evl6mr5aj9f58zp6fyjzup6ywn3x6sk8akg5v4tgn2q8g4fhx05wf6juaxu9760yp46454gpg5mtzgerlzezqcqvjnhjh8z3g2qqdhhwkj"));
        assertTrue(LightningInvoiceValidation.validateInvoice("lnbc1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq9qrsgq357wnc5r2ueh7ck6q93dj32dlqnls087fxdwk8qakdyafkq3yap9us6v52vjjsrvywa6rt52cm9r9zqt8r2t7mlcwspyetp5h2tztugp9lfyql"));

        assertFalse(LightningInvoiceValidation.validateInvoice("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        assertFalse(LightningInvoiceValidation.validateInvoice("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"));
        assertFalse(LightningInvoiceValidation.validateInvoice("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w"));
        assertFalse(LightningInvoiceValidation.validateInvoice(""));
    }
}