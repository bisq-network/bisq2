package bisq.api.rest_api.endpoints.payment_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethodUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMapping;
import bisq.api.dto.mappings.account.crypto.CryptoPaymentMethodDtoMapping;
import bisq.api.dto.mappings.account.fiat.FiatPaymentMethodMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Path("/payment-accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payment Accounts API", description = "API for retrieving fiat and crypto payment accounts.")
public class PaymentAccountsRestApi extends RestApiBase {
    private final AccountService accountService;

    public PaymentAccountsRestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Operation(
            summary = "Get payment accounts",
            description = "Retrieve fiat and crypto payment accounts",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment accounts retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = PaymentAccountDto.class))
                            )),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getPaymentAccounts() {
        try {
            var accounts = accountService.getAccounts();

            List<PaymentAccountDto> paymentAccounts = accounts.stream()
                    .filter(account -> isFiatAccount(account) || isCryptoAccount(account))
                    .map(PaymentAccountDtoMapping::fromBisq2Model)
                    .toList();

            return buildOkResponse(paymentAccounts);
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Operation(
            summary = "Add new payment account",
            description = "Create a new fiat or crypto payment account.",
            requestBody = @RequestBody(
                    description = "Payment account details",
                    content = @Content(
                            schema = @Schema(implementation = PaymentAccountDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Zelle account",
                                            value = "{\n  \"accountName\": \"My Zelle Account\",\n  \"paymentRail\": \"ZELLE\",\n  \"accountPayload\": {\n    \"holderName\": \"John Doe\",\n    \"emailOrMobileNr\": \"john.doe@example.com\"\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "ACH transfer account",
                                            value = "{\n  \"accountName\": \"My Ach Account\",\n  \"paymentRail\": \"ACH_TRANSFER\",\n  \"accountPayload\": {\n    \"holderName\": \"John Doe\",\n    \"holderAddress\": \"Some Address\",\n    \"bankName\": \"Bank of Test\",\n    \"routingNr\": \"123456789\",\n    \"accountNr\": \"000123456789\",\n    \"bankAccountType\": \"CHECKING\"\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "Monero account",
                                            value = "{\n  \"accountName\": \"My Monero Account\",\n  \"paymentRail\": \"MONERO\",\n  \"accountPayload\": {\n    \"currencyCode\": \"XMR\",\n    \"address\": \"84f....\",\n    \"isInstant\": false,\n    \"isAutoConf\": false,\n    \"autoConfNumConfirmations\": 1,\n    \"autoConfMaxTradeAmount\": 100000,\n    \"autoConfExplorerUrls\": \"https://xmrchain.net\",\n    \"useSubAddresses\": false,\n    \"mainAddress\": null,\n    \"privateViewKey\": null,\n    \"subAddress\": null,\n    \"accountIndex\": null,\n    \"initialSubAddressIndex\": null\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "Other crypto account",
                                            value = "{\n  \"accountName\": \"My LTC Account\",\n  \"paymentRail\": \"OTHER_CRYPTO_ASSET\",\n  \"accountPayload\": {\n    \"currencyCode\": \"LTC\",\n    \"address\": \"ltc1....\",\n    \"isInstant\": false,\n    \"isAutoConf\": false,\n    \"autoConfNumConfirmations\": 1,\n    \"autoConfMaxTradeAmount\": 100000,\n    \"autoConfExplorerUrls\": \"https://blockchair.com/litecoin\"\n  }\n}"
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Payment account created successfully",
                            content = @Content(schema = @Schema(implementation = PaymentAccountDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "409", description = "Payment account already exists"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    public void addAccount(@Valid PaymentAccountDto request,
                           @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response ->
                response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out")));
        try {
            if (request == null) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data is required"));
                return;
            }

            try {
                Account<? extends PaymentMethod<?>, ?> account = PaymentAccountDtoMapping.toBisq2Model(request);
                if (!accountService.addPaymentAccount(account)) {
                    asyncResponse.resume(buildErrorResponse(Response.Status.CONFLICT,
                            "Payment account already exists: " + request.accountName()));
                    return;
                }

                Optional<Account<? extends PaymentMethod<?>, ?>> createdAccount = accountService.findAccount(account.getAccountName());
                if (createdAccount.isEmpty()) {
                    asyncResponse.resume(buildErrorResponse("Account was created but could not be loaded"));
                    return;
                }

                PaymentAccountDto createdAccountDto = PaymentAccountDtoMapping.fromBisq2Model(createdAccount.get());
                asyncResponse.resume(buildResponse(Response.Status.CREATED, createdAccountDto));
            } catch (IllegalArgumentException e) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()));
            }
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DELETE
    @Operation(
            summary = "Delete payment account",
            description = "Delete a payment account by account name (fiat or crypto)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Payment account successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Account name is required"),
                    @ApiResponse(responseCode = "404", description = "Account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    public void removeAccount(@QueryParam("accountName") String accountName,
                              @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response ->
                response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out")));
        try {
            if (accountName == null || accountName.trim().isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account name is required"));
                return;
            }

            Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountName);
            if (result.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found"));
                return;
            }

            accountService.removePaymentAccount(result.get());
            asyncResponse.resume(buildNoContentResponse());
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GET
    @Path("/payment-methods/fiat")
    @Operation(
            summary = "Get fiat payment methods",
            description = "Retrieve all available fiat payment methods as displayed by the desktop payment method selection",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fiat payment methods retrieved successfully",
                            content = @Content(schema = @Schema(implementation = FiatPaymentMethodDto.class, type = "array"))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @SuppressWarnings("deprecation")
    public Response getFiatPaymentMethods() {
        try {
            List<FiatPaymentMethodDto> items = FiatPaymentRailUtil.getPaymentRails().stream()
                    .filter(rail -> rail != FiatPaymentRail.CUSTOM)
                    .filter(rail -> rail != FiatPaymentRail.CASH_APP)
                    .map(FiatPaymentMethod::fromPaymentRail)
                    .map(FiatPaymentMethodMapping::fromBisq2Model)
                    .sorted(Comparator.comparing(FiatPaymentMethodDto::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return buildOkResponse(items);
        } catch (Exception e) {
            log.error("Failed to retrieve fiat payment methods", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Path("/payment-methods/crypto")
    @Operation(
            summary = "Get crypto payment methods",
            description = "Retrieve all available crypto payment methods",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Crypto payment methods retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CryptoPaymentMethodDto.class, type = "array"))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getCryptoPaymentMethods() {
        try {
            List<CryptoPaymentMethodDto> items = CryptoPaymentMethodUtil.getPaymentMethods().stream()
                    .filter(e -> !e.getCode().equals("BTC"))
                    .map(CryptoPaymentMethodDtoMapping::fromBisq2Model)
                    .toList();
            return buildOkResponse(items);
        } catch (Exception e) {
            log.error("Failed to retrieve crypto payment methods", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean isFiatAccount(Account<? extends PaymentMethod<?>, ?> account) {
        return account.getPaymentMethod() instanceof FiatPaymentMethod;
    }

    public boolean isCryptoAccount(Account<? extends PaymentMethod<?>, ?> account) {
        return account instanceof CryptoAssetAccount;
    }
}
