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

package bisq.http_api.rest_api.domain.payment_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.*;
import bisq.dto.DtoMappings;
import bisq.dto.account.fiat.FiatAccountDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Path("/payment-accounts/fiat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Fiat Payment Accounts API", description = "API for managing fiat payment accounts. Supports all FiatPaymentRail types including CUSTOM (user-defined), SEPA, REVOLUT, ZELLE, and more.")
public class FiatPaymentAccountsRestApi extends RestApiBase {
    private final AccountService accountService;

    public FiatPaymentAccountsRestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Operation(
            summary = "Get fiat payment accounts",
            description = "Retrieve all fiat payment accounts",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fiat payment accounts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = FiatAccountDto.class, type = "array"))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getPaymentAccounts() {
        try {
            List<FiatAccountDto> accounts = accountService.getAccounts().stream()
                    .filter(this::isFiatAccount)
                    .map(this::convertToDto)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return buildOkResponse(accounts);
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Check if account is a fiat payment account
     */
    private boolean isFiatAccount(Account<? extends PaymentMethod<?>, ?> account) {
        return account.getPaymentMethod() instanceof FiatPaymentMethod;
    }

    /**
     * Convert account to DTO. Returns Optional.empty() for unsupported account types.
     * TODO: Add support for other fiat account types (SEPA, Revolut, Zelle, etc.)
     */
    private Optional<FiatAccountDto> convertToDto(Account<? extends PaymentMethod<?>, ?> account) {
        // Currently only UserDefinedFiatAccount (CUSTOM payment rail) is supported
        if (account instanceof UserDefinedFiatAccount userDefinedAccount) {
            return Optional.of(DtoMappings.UserDefinedFiatAccountMapping.fromBisq2Model(userDefinedAccount));
        }

        // TODO: Add conversions for other account types when implemented:
        // if (account instanceof SepaAccount sepaAccount) {
        //     return Optional.of(DtoMappings.SepaAccountMapping.fromBisq2Model(sepaAccount));
        // }
        // if (account instanceof RevolutAccount revolutAccount) {
        //     return Optional.of(DtoMappings.RevolutAccountMapping.fromBisq2Model(revolutAccount));
        // }
        // ... etc

        return Optional.empty();
    }

    @GET
    @Operation(
            summary = "Get selected fiat payment account",
            description = "Get the currently selected fiat payment account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Selected fiat payment account retrieved successfully",
                            content = @Content(schema = @Schema(implementation = FiatAccountDto.class))),
                    @ApiResponse(responseCode = "204", description = "No account selected"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response getSelectedPaymentAccount() {
        try {
            Optional<Account<? extends PaymentMethod<?>, ?>> selectedAccount = accountService.getSelectedAccount();
            if (selectedAccount.isEmpty() || !isFiatAccount(selectedAccount.get())) {
                return buildNoContentResponse();
            }

            return convertToDto(selectedAccount.get())
                    .map(this::buildOkResponse)
                    .orElse(buildNoContentResponse());
        } catch (Exception e) {
            log.error("Failed to retrieve selected payment account", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Operation(
            summary = "Add new fiat payment account",
            description = "Create a new fiat payment account. The payment rail type is specified in the request body.",
            requestBody = @RequestBody(
                    description = "Fiat account details including payment rail type, account name, and payload",
                    content = @Content(schema = @Schema(implementation = AddFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Fiat payment account created successfully",
                            content = @Content(schema = @Schema(implementation = FiatAccountDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input or unsupported payment rail"),
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
            FiatAccountDto accountDto = request.account();
            if (accountDto == null) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data is required"));
                return;
            }

            // Create and add account
            try {
                Account<? extends PaymentMethod<?>, ?> account = DtoMappings.FiatAccountMapping.toBisq2Model(accountDto);
                accountService.addPaymentAccount(account);
            } catch (IllegalArgumentException e) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()));
                return;
            }

            asyncResponse.resume(buildResponse(Response.Status.CREATED, accountDto));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DELETE
    @Operation(
            summary = "Delete fiat payment account",
            description = "Delete a fiat payment account by account name",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Fiat payment account successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Account is not a fiat payment account"),
                    @ApiResponse(responseCode = "404", description = "Account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    @Path("/{accountName}")
    public void removeAccount(@PathParam("accountName") String accountName,
                              @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountName);
            if (result.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found"));
                return;
            }

            Account<? extends PaymentMethod<?>, ?> toRemove = result.get();

            // Validate that it's a fiat payment account
            if (!isFiatAccount(toRemove)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a fiat payment account"));
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
            summary = "Update fiat payment account",
            description = "Update an existing fiat payment account by replacing it with new data. The payment rail type is specified in the request body.",
            requestBody = @RequestBody(
                    description = "Updated fiat account details including payment rail type, account name, and payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SaveFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Fiat payment account updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data or unsupported payment rail"),
                    @ApiResponse(responseCode = "404", description = "Fiat payment account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    @Path("/{accountName}")
    public void saveAccount(@PathParam("accountName") String accountName,
                            @Valid SaveFiatAccountRequest request,
                            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            FiatAccountDto accountDto = request.account();
            if (accountDto == null) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data is required"));
                return;
            }

            // Find existing account
            Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountName);
            if (result.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found"));
                return;
            }

            Account<? extends PaymentMethod<?>, ?> existingAccount = result.get();

            // Validate it's a fiat account
            if (!isFiatAccount(existingAccount)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a fiat payment account"));
                return;
            }

            // Update account
            try {
                Account<? extends PaymentMethod<?>, ?> newAccount = DtoMappings.FiatAccountMapping.toBisq2Model(accountDto);
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
            summary = "Set selected fiat payment account",
            description = "Set the currently selected fiat payment account. Pass null or omit selectedAccount to unselect the current account.",
            requestBody = @RequestBody(
                    description = "The fiat payment account to set as selected. Pass null to unselect the current account.",
                    required = false,
                    content = @Content(schema = @Schema(implementation = SetSelectedFiatAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Selected fiat payment account set successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "404", description = "Payment account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response setSelectedPaymentAccount(@Valid SetSelectedFiatAccountRequest request) {
        try {
            FiatAccountDto accountDto = request.selectedAccount();
            if (accountDto != null) {
                Optional<Account<? extends PaymentMethod<?>, ?>> result = accountService.findAccount(accountDto.accountName());
                if (result.isEmpty()) {
                    return buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found");
                }

                Account<? extends PaymentMethod<?>, ?> foundAccount = result.get();

                // Validate it's a fiat payment account
                if (!isFiatAccount(foundAccount)) {
                    return buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a fiat payment account");
                }

                // Set the selected account
                accountService.setSelectedAccount(foundAccount);
                log.info("Set selected payment account: {}", accountDto.accountName());
            } else {
                // Explicitly unselect the current account
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