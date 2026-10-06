# BISQ Easy Bitcoin Payment Data

In a BISQ Easy trade the buyer sends the seller the data for the bitcoin payment: a bitcoin address for the main chain, or a Lightning invoice. The seller's application shows the data as text and as a QR code, and the seller pays from it with a wallet. A wallet pays the amount which the data requests: the amount of a BOLT 11 invoice, the amount parameter of a BIP 21 bitcoin URI, or the amount of a BOLT 12 offer or invoice. The seller's application shows the trade amount, but not the amount which the data requests.

## Rules

1. The bitcoin payment data requests no amount or the trade amount of the contract. This applies to every BOLT 11 invoice in the data, also behind a `lightning:` prefix or inside a bitcoin URI, and to every amount parameter of a bitcoin URI. An amount which cannot be read, and every BOLT 12 offer or invoice, counts as another amount.
2. Rule 1 applies to every reading of the data which a wallet can make. A reading percent-decodes the data up to three times and can remove whitespace, control and format characters and "+" before and after each decoding.
3. The seller's node checks rules 1 and 2 when it receives the data, before it stores and shows the data. Data which breaks them fails the trade, and the seller's node reports the failure to the buyer.
4. The buyer's node does not send data which breaks rules 1 and 2, also when the API asks it to; the API answers with 400. The buyer's desktop application tells the buyer to use data without amount or with the trade amount.

## Reasons

- Rule 1: the seller confirms the payment in its wallet, which shows the requested amount. A seller who relies on the trade amount in the trade view could pay more than the trade amount, for example ten times it with a Lightning invoice for 0.01 BTC in a trade of 0.001 BTC. Data without amount lets the seller enter the trade amount. The node does not read the amount of a BOLT 12 offer or invoice, and the buyer is asked for a BOLT 11 invoice.
- Rule 2: a wallet decodes a bitcoin URI before it reads its parameters, and some wallets remove whitespace from the data, before or after decoding. Form decoding turns "+" into a space. An amount which only one reading shows still reaches such a wallet.
- Rule 3: the buyer chooses the data, and a modified client can send any data. Only the seller's node can protect the seller.
- Rule 4: an honest buyer whose wallet created an invoice for another amount would otherwise fail the trade at rule 3.

## Limitations

- An LNURL or a Lightning address carries no amount. The wallet asks the server of the buyer for an invoice, and this server can propose another amount. The seller has to check the amount in its wallet.

## Compatibility

- No message or storage change. A buyer of an earlier version can send data which breaks rule 1. The seller's node of this version fails such a trade instead of showing the data.
