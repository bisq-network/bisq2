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

package bisq.api.rest_api.endpoints.payment_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.UserDefinedFiatAccountDtoMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/*
 * Temporary REST API for fiat-only account management.
 * This endpoint is planned to be removed once MuSig is enabled and the client
 * migrates to PaymentAccountsRestApi, which accepts both fiat and crypto accounts.
 */
@Slf4j
@Path("/payment-accounts/fiat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User-Defined Fiat Payment Accounts API", description = "API for managing user-defined fiat payment accounts (CUSTOM rail only).")
public class UserDefinedPaymentAccountsRestApi extends RestApiBase {
    private final AccountService accountService;

    public UserDefinedPaymentAccountsRestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Operation(
            summary = "Get user-defined fiat payment accounts",
            description = "Retrieve all user-defined fiat payment accounts (CUSTOM rail only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User-defined fiat payment accounts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PaymentAccountDto.class, type = "array"))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getUserDefinedPaymentAccounts() {
        try {
            List<PaymentAccountDto> accounts = accountService.getAccounts().stream()
                    .filter(this::isUserDefinedAccount)
                    .map(this::convertToDto)
                    .toList();

            return buildOkResponse(accounts);
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Check if account is a user-defined fiat payment account.
     */
    private boolean isUserDefinedAccount(Account<? extends PaymentMethod<?>, ?> account) {
        return account instanceof UserDefinedFiatAccount;
    }

    private PaymentAccountDto convertToDto(Account<? extends PaymentMethod<?>, ?> account) {
        return PaymentAccountDtoMapping.fromBisq2Model(account);
    }

    @GET
    @Operation(
            summary = "Get selected user-defined fiat payment account",
            description = "Get the currently selected user-defined fiat payment account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Selected user-defined fiat payment account retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PaymentAccountDto.class))),
                    @ApiResponse(responseCode = "204", description = "No account selected"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response getSelectedPaymentAccount() {
        try {
            Optional<Account<? extends PaymentMethod<?>, ?>> selectedAccount = accountService.findSelectedAccount();
            if (selectedAccount.isEmpty() || !isUserDefinedAccount(selectedAccount.get())) {
                return buildNoContentResponse();
            }

            return buildOkResponse(convertToDto(selectedAccount.get()));
        } catch (Exception e) {
            log.error("Failed to retrieve selected payment account", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Operation(
            summary = "Add new user-defined fiat payment account",
            description = "Create a new user-defined fiat payment account (CUSTOM rail only).",
            requestBody = @RequestBody(
                    description = "User-defined fiat account details (must be UserDefinedFiatAccountDto / CUSTOM)",
                    content = @Content(schema = @Schema(implementation = AddFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User-defined fiat payment account created successfully",
                            content = @Content(schema = @Schema(implementation = PaymentAccountDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input or unsupported account type"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    public void addAccount(@Valid AddFiatAccountRequest request,
                           @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            PaymentAccountDto accountDto = request.account();
            if (accountDto == null) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data is required"));
                return;
            }
            if (!(accountDto instanceof UserDefinedFiatAccountDto userDefinedDto)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST,
                        "Account must be of type UserDefinedFiatAccountDto"));
                return;
            }

            try {
                UserDefinedFiatAccount account = UserDefinedFiatAccountDtoMapping.toBisq2Model(userDefinedDto);
                accountService.addPaymentAccount(account);
            } catch (IllegalArgumentException e) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()));
                return;
            }

            asyncResponse.resume(buildResponse(Response.Status.CREATED, userDefinedDto));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DELETE
    @Operation(
            summary = "Delete user-defined fiat payment account",
            description = "Delete a user-defined fiat payment account by account name (provided as query parameter)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User-defined fiat payment account successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Account is not a user-defined fiat payment account"),
                    @ApiResponse(responseCode = "404", description = "Account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    public void removeAccount(@QueryParam("accountName") String accountName,
                              @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
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

            Account<? extends PaymentMethod<?>, ?> toRemove = result.get();

            if (!isUserDefinedAccount(toRemove)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a user-defined fiat payment account"));
                return;
            }

            accountService.removePaymentAccount(toRemove);
            asyncResponse.resume(buildNoContentResponse());
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PUT
    @Operation(
            summary = "Update user-defined fiat payment account",
            description = "Update an existing user-defined fiat payment account by replacing it with new data. The account name to update is provided as a query parameter.",
            requestBody = @RequestBody(
                    description = "Updated user-defined fiat account details (must be UserDefinedFiatAccountDto / CUSTOM)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SaveFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "User-defined fiat payment account updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data or unsupported account type"),
                    @ApiResponse(responseCode = "404", description = "User-defined fiat payment account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    public void saveAccount(@QueryParam("accountName") String accountName,
                            @Valid SaveFiatAccountRequest request,
                            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            if (accountName == null || accountName.trim().isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account name query parameter is required"));
                return;
            }

            PaymentAccountDto accountDto = request.account();
            if (accountDto == null) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data is required"));
                return;
            }
            if (!(accountDto instanceof UserDefinedFiatAccountDto userDefinedDto)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST,
                        "Account must be of type UserDefinedFiatAccountDto"));
                return;
            }

            Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountName);
            if (result.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found"));
                return;
            }

            Account<? extends PaymentMethod<?>, ?> existingAccount = result.get();

            if (!isUserDefinedAccount(existingAccount)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a user-defined fiat payment account"));
                return;
            }

            try {
                UserDefinedFiatAccount newAccount = UserDefinedFiatAccountDtoMapping.toBisq2Model(userDefinedDto);
                accountService.updatePaymentAccount(accountName, newAccount);
            } catch (IllegalArgumentException e) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()));
                return;
            }

            asyncResponse.resume(buildNoContentResponse());
        } catch (Exception e) {
            log.error("Failed to save payment account", e);
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PATCH
    @Operation(
            summary = "Set selected user-defined fiat payment account",
            description = "Set the currently selected user-defined fiat payment account. Pass null or omit selectedAccount to unselect.",
            requestBody = @RequestBody(
                    description = "The user-defined fiat payment account to set as selected. Pass null to unselect.",
                    required = false,
                    content = @Content(schema = @Schema(implementation = SetSelectedFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Selected user-defined fiat payment account set successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "Payment account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response setSelectedPaymentAccount(@Valid SetSelectedFiatAccountRequest request) {
        try {
            PaymentAccountDto accountDto = request.selectedAccount();
            if (accountDto != null) {
                if (!(accountDto instanceof UserDefinedFiatAccountDto)) {
                    return buildErrorResponse(Response.Status.BAD_REQUEST,
                            "Account must be of type UserDefinedFiatAccountDto");
                }
                Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountDto.accountName());
                if (result.isEmpty()) {
                    return buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found");
                }

                Account<? extends PaymentMethod<?>, ?> foundAccount = result.get();

                if (!isUserDefinedAccount(foundAccount)) {
                    return buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a user-defined fiat payment account");
                }

                accountService.setSelectedAccount(foundAccount);
                log.info("Set selected payment account: {}", accountDto.accountName());
            } else {
                accountService.setSelectedAccount(null);
                log.info("Unselected payment account");
            }

            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error updating selected payment account", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}

