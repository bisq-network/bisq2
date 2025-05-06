## Payment account domain 

### AccountPayload

The `AccountPayload` data is sent over the wire, thus only contain data the user need to share in the trade with the peer.

`AccountPayload` is the base class of all accountPayloads
```
String id;
String paymentMethodName;
```

Subclasses of `AccountPayload`:

- `CountryBasedAccountPayload`:
    ```
    String countryCode;
    ```
- And several accountPayloads which are not based on  `CountryBasedAccountPayload`.

Subclasses of `CountryBasedAccountPayload`: 
- `BankAccountPayload`
    ```
    protected Optional<String> holderName;
    protected Optional<String> bankName;
    protected Optional<String> branchId;
    protected Optional<String> accountNr;
    protected Optional<String> accountType;
    protected Optional<String> holderTaxId;
    protected Optional<String> bankId;
    protected Optional<String> nationalAccountId;
    ```
- And several accountPayloads which are not based on  `BankAccountPayload`.

Subclasses of `BankAccountPayload`:
- `AchTransferAccountPayload`
- `CashDepositAccountPayload`
- `NationalBankAccountPayload`

We do not have yet `SameBankAccountPayload` and `SpecificBanksAccountPayload` as in Bisq 1.
Those are for supporting special conditions to save bank fees.

### Account

`Account` is the local data which caries the accountPayload, next to other local data
```
protected final long creationDate;
protected final String accountName;
protected final AccountPayload accountPayload;
protected final PaymentMethod paymentMethod;
```    

#### PaymentMethod

`PaymentMethod` - Abstract base class.
```
protected final String name;
protected transient final PaymentRail paymentRail;
protected transient final String displayString;
protected transient final String shortDisplayString;
public abstract List<TradeCurrency> getTradeCurrencies();
```

Subclasses:
- `FiatPaymentMethod`
- `BitcoinPaymentMethod`
- `CryptoPaymentMethod` carries the `currencyCode` of the asset.
```
private final String currencyCode;
```

#### PaymentRail
Interface with a `name()` method.

Implemented by:
- `FiatPaymentRail` enum of all fiat payment rails (e.g. SEPA, WISE,...)
```
private final List<Country> countries;
private final List<TradeCurrency> tradeCurrencies;
private final List<String> currencyCodes;
```
- `BitcoinPaymentRail` enum of all Bitcoin payment rails (how is the Bitcoin transferred? - main-chain, LN, Liquid,...)
- `CryptoPaymentRail` enum of all crypto asset payment rails (how is the asset transferred? - native chain, Liquid,...)


