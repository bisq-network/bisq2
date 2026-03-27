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

package bisq.api.dto;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.AchTransferAccountDto;
import bisq.api.dto.account.fiat.AdvancedCashAccountDto;
import bisq.api.dto.account.fiat.AliPayAccountDto;
import bisq.api.dto.account.fiat.AmazonGiftCardAccountDto;
import bisq.api.dto.account.fiat.BizumAccountDto;
import bisq.api.dto.account.fiat.CashByMailAccountDto;
import bisq.api.dto.account.fiat.CashDepositAccountDto;
import bisq.api.dto.account.fiat.DomesticWireTransferAccountDto;
import bisq.api.dto.account.fiat.F2FAccountDto;
import bisq.api.dto.account.fiat.FasterPaymentsAccountDto;
import bisq.api.dto.account.fiat.FiatAccountDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodItemDto;
import bisq.api.dto.account.fiat.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.HalCashAccountDto;
import bisq.api.dto.account.fiat.ImpsAccountDto;
import bisq.api.dto.account.fiat.InteracETransferAccountDto;
import bisq.api.dto.account.fiat.MercadoPagoAccountDto;
import bisq.api.dto.account.fiat.MoneseAccountDto;
import bisq.api.dto.account.fiat.MoneyBeamAccountDto;
import bisq.api.dto.account.fiat.MoneyGramAccountDto;
import bisq.api.dto.account.fiat.NationalBankAccountDto;
import bisq.api.dto.account.fiat.NeftAccountDto;
import bisq.api.dto.account.fiat.PayIdAccountDto;
import bisq.api.dto.account.fiat.PayseraAccountDto;
import bisq.api.dto.account.fiat.PerfectMoneyAccountDto;
import bisq.api.dto.account.fiat.Pin4AccountDto;
import bisq.api.dto.account.fiat.PixAccountDto;
import bisq.api.dto.account.fiat.PromptPayAccountDto;
import bisq.api.dto.account.fiat.RevolutAccountDto;
import bisq.api.dto.account.fiat.SameBankAccountDto;
import bisq.api.dto.account.fiat.SatispayAccountDto;
import bisq.api.dto.account.fiat.SbpAccountDto;
import bisq.api.dto.account.fiat.SepaAccountDto;
import bisq.api.dto.account.fiat.SepaInstantAccountDto;
import bisq.api.dto.account.fiat.StrikeAccountDto;
import bisq.api.dto.account.fiat.SwiftAccountDto;
import bisq.api.dto.account.fiat.SwishAccountDto;
import bisq.api.dto.account.fiat.USPostalMoneyOrderAccountDto;
import bisq.api.dto.account.fiat.USPostalMoneyOrderAccountPayloadDto;
import bisq.api.dto.account.fiat.UpholdAccountDto;
import bisq.api.dto.account.fiat.UpholdAccountPayloadDto;
import bisq.api.dto.account.fiat.UpiAccountDto;
import bisq.api.dto.account.fiat.UpiAccountPayloadDto;
import bisq.api.dto.account.fiat.WeChatPayAccountDto;
import bisq.api.dto.account.fiat.WeChatPayAccountPayloadDto;
import bisq.api.dto.account.fiat.WiseAccountDto;
import bisq.api.dto.account.fiat.WiseAccountPayloadDto;
import bisq.api.dto.account.fiat.WiseUsdAccountDto;
import bisq.api.dto.account.fiat.WiseUsdAccountPayloadDto;
import bisq.api.dto.account.fiat.ZelleAccountDto;
import bisq.api.dto.account.fiat.ZelleAccountPayloadDto;
import bisq.api.dto.account.protocol_type.TradeProtocolTypeDto;
import bisq.api.dto.chat.ChatChannelDomainDto;
import bisq.api.dto.chat.ChatMessageTypeDto;
import bisq.api.dto.chat.CitationDto;
import bisq.api.dto.chat.bisq_easy.offerbook.BisqEasyOfferbookMessageDto;
import bisq.api.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto;
import bisq.api.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto;
import bisq.api.dto.chat.reactions.BisqEasyOpenTradeMessageReactionDto;
import bisq.api.dto.common.currency.MarketDto;
import bisq.api.dto.common.monetary.CoinDto;
import bisq.api.dto.common.monetary.FiatDto;
import bisq.api.dto.common.monetary.MonetaryDto;
import bisq.api.dto.common.monetary.PriceQuoteDto;
import bisq.api.dto.common.network.AddressByTransportTypeMapDto;
import bisq.api.dto.common.network.AddressDto;
import bisq.api.dto.common.network.TransportTypeDto;
import bisq.api.dto.contract.ContractSignatureDataDto;
import bisq.api.dto.contract.PartyDto;
import bisq.api.dto.contract.RoleDto;
import bisq.api.dto.contract.bisq_easy.BisqEasyContractDto;
import bisq.api.dto.identity.IdentityDto;
import bisq.api.dto.mappings.fiat.AchTransferAccountDtoMapping;
import bisq.api.dto.mappings.fiat.AdvancedCashAccountDtoMapping;
import bisq.api.dto.mappings.fiat.AliPayAccountDtoMapping;
import bisq.api.dto.mappings.fiat.AmazonGiftCardAccountDtoMapping;
import bisq.api.dto.mappings.fiat.BizumAccountDtoMapping;
import bisq.api.dto.mappings.fiat.CashByMailAccountDtoMapping;
import bisq.api.dto.mappings.fiat.CashDepositAccountDtoMapping;
import bisq.api.dto.mappings.fiat.DomesticWireTransferAccountDtoMapping;
import bisq.api.dto.mappings.fiat.F2FAccountDtoMapping;
import bisq.api.dto.mappings.fiat.FasterPaymentsAccountDtoMapping;
import bisq.api.dto.mappings.fiat.HalCashAccountDtoMapping;
import bisq.api.dto.mappings.fiat.ImpsAccountDtoMapping;
import bisq.api.dto.mappings.fiat.InteracETransferAccountDtoMapping;
import bisq.api.dto.mappings.fiat.MercadoPagoAccountDtoMapping;
import bisq.api.dto.mappings.fiat.MoneseAccountDtoMapping;
import bisq.api.dto.mappings.fiat.MoneyBeamAccountDtoMapping;
import bisq.api.dto.mappings.fiat.MoneyGramAccountDtoMapping;
import bisq.api.dto.mappings.fiat.NationalBankAccountDtoMapping;
import bisq.api.dto.mappings.fiat.NeftAccountDtoMapping;
import bisq.api.dto.mappings.fiat.PayIdAccountDtoMapping;
import bisq.api.dto.mappings.fiat.PayseraAccountDtoMapping;
import bisq.api.dto.mappings.fiat.PerfectMoneyAccountDtoMapping;
import bisq.api.dto.mappings.fiat.Pin4AccountDtoMapping;
import bisq.api.dto.mappings.fiat.PixAccountDtoMapping;
import bisq.api.dto.mappings.fiat.PromptPayAccountDtoMapping;
import bisq.api.dto.mappings.fiat.RevolutAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SameBankAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SatispayAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SbpAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SepaAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SepaInstantAccountDtoMapping;
import bisq.api.dto.mappings.fiat.StrikeAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SwiftAccountDtoMapping;
import bisq.api.dto.mappings.fiat.SwishAccountDtoMapping;
import bisq.api.dto.mappings.fiat.UserDefinedFiatAccountDtoMapping;
import bisq.api.dto.network.identity.NetworkIdDto;
import bisq.api.dto.offer.DirectionDto;
import bisq.api.dto.offer.amount.spec.AmountSpecDto;
import bisq.api.dto.offer.amount.spec.BaseSideFixedAmountSpecDto;
import bisq.api.dto.offer.amount.spec.BaseSideRangeAmountSpecDto;
import bisq.api.dto.offer.amount.spec.FixedAmountSpecDto;
import bisq.api.dto.offer.amount.spec.QuoteSideFixedAmountSpecDto;
import bisq.api.dto.offer.amount.spec.QuoteSideRangeAmountSpecDto;
import bisq.api.dto.offer.amount.spec.RangeAmountSpecDto;
import bisq.api.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.api.dto.offer.options.OfferOptionDto;
import bisq.api.dto.offer.options.ReputationOptionDto;
import bisq.api.dto.offer.options.TradeTermsOptionDto;
import bisq.api.dto.offer.payment_method.BitcoinPaymentMethodSpecDto;
import bisq.api.dto.offer.payment_method.FiatPaymentMethodSpecDto;
import bisq.api.dto.offer.payment_method.PaymentMethodSpecDto;
import bisq.api.dto.offer.price.spec.FixPriceSpecDto;
import bisq.api.dto.offer.price.spec.FloatPriceSpecDto;
import bisq.api.dto.offer.price.spec.MarketPriceSpecDto;
import bisq.api.dto.offer.price.spec.PriceSpecDto;
import bisq.api.dto.security.keys.I2PKeyPairDto;
import bisq.api.dto.security.keys.KeyBundleDto;
import bisq.api.dto.security.keys.KeyPairDto;
import bisq.api.dto.security.keys.PrivateKeyDto;
import bisq.api.dto.security.keys.PubKeyDto;
import bisq.api.dto.security.keys.PublicKeyDto;
import bisq.api.dto.security.keys.TorKeyPairDto;
import bisq.api.dto.security.pow.ProofOfWorkDto;
import bisq.api.dto.settings.SettingsDto;
import bisq.api.dto.trade.TradeProtocolFailureDto;
import bisq.api.dto.trade.TradeRoleDto;
import bisq.api.dto.trade.bisq_easy.BisqEasyTradeDto;
import bisq.api.dto.trade.bisq_easy.BisqEasyTradePartyDto;
import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.api.dto.user.identity.UserIdentityDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.common.encoding.Hex;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.contract.ContractSignatureData;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.options.ReputationOption;
import bisq.offer.options.TradeTermsOption;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.DigestUtil;
import bisq.security.keys.I2PKeyPair;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.trade.TradeRole;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeParty;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

public class DtoMappings {
    // account.protocol_type

    public static class TradeProtocolTypeMapping {
        public static TradeProtocolType toBisq2Model(TradeProtocolTypeDto value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BISQ_EASY -> TradeProtocolType.BISQ_EASY;
                case MU_SIG -> TradeProtocolType.MU_SIG;
                case SUBMARINE -> TradeProtocolType.SUBMARINE;
                case LIQUID_MU_SIG -> TradeProtocolType.LIQUID_MU_SIG;
                case BISQ_LIGHTNING -> TradeProtocolType.BISQ_LIGHTNING;
                case LIQUID_SWAP -> TradeProtocolType.LIQUID_SWAP;
                case BSQ_SWAP -> TradeProtocolType.BSQ_SWAP;
                case LIGHTNING_ESCROW -> TradeProtocolType.LIGHTNING_ESCROW;
                case MONERO_SWAP -> TradeProtocolType.MONERO_SWAP;
            };
        }

        public static TradeProtocolTypeDto fromBisq2Model(TradeProtocolType value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BISQ_EASY -> TradeProtocolTypeDto.BISQ_EASY;
                case MU_SIG -> TradeProtocolTypeDto.MU_SIG;
                case SUBMARINE -> TradeProtocolTypeDto.SUBMARINE;
                case LIQUID_MU_SIG -> TradeProtocolTypeDto.LIQUID_MU_SIG;
                case BISQ_LIGHTNING -> TradeProtocolTypeDto.BISQ_LIGHTNING;
                case LIQUID_SWAP -> TradeProtocolTypeDto.LIQUID_SWAP;
                case BSQ_SWAP -> TradeProtocolTypeDto.BSQ_SWAP;
                case LIGHTNING_ESCROW -> TradeProtocolTypeDto.LIGHTNING_ESCROW;
                case MONERO_SWAP -> TradeProtocolTypeDto.MONERO_SWAP;
            };
        }
    }


    // chat

    public static class ChatChannelDomainMapping {
        public static ChatChannelDomain toBisq2Model(ChatChannelDomainDto value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BISQ_EASY_OFFERBOOK -> ChatChannelDomain.BISQ_EASY_OFFERBOOK;
                case BISQ_EASY_OPEN_TRADES -> ChatChannelDomain.BISQ_EASY_OPEN_TRADES;
                case DISCUSSION -> ChatChannelDomain.DISCUSSION;
                case SUPPORT -> ChatChannelDomain.SUPPORT;
                case MU_SIG_OPEN_TRADES -> ChatChannelDomain.MU_SIG_OPEN_TRADES;
            };
        }

        public static ChatChannelDomainDto fromBisq2Model(ChatChannelDomain value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BISQ_EASY_OFFERBOOK -> ChatChannelDomainDto.BISQ_EASY_OFFERBOOK;
                case BISQ_EASY_OPEN_TRADES -> ChatChannelDomainDto.BISQ_EASY_OPEN_TRADES;
                case DISCUSSION -> ChatChannelDomainDto.DISCUSSION;
                case SUPPORT -> ChatChannelDomainDto.SUPPORT;
                case MU_SIG_OPEN_TRADES -> ChatChannelDomainDto.MU_SIG_OPEN_TRADES;
                case BISQ_EASY_PRIVATE_CHAT -> ChatChannelDomainDto.DISCUSSION;
                case EVENTS -> ChatChannelDomainDto.DISCUSSION;
            };
        }
    }

    public static class CitationMapping {
        public static Citation toBisq2Model(CitationDto value) {
            return new Citation(
                    value.authorUserProfileId(),
                    value.text(),
                    value.chatMessageId()
            );
        }

        public static CitationDto fromBisq2Model(Citation value) {
            return new CitationDto(
                    value.getAuthorUserProfileId(),
                    value.getText(),
                    value.getChatMessageId()
            );
        }
    }

    public static class ChatMessageTypeMapping {
        public static ChatMessageType toBisq2Model(ChatMessageTypeDto value) {
            return ChatMessageType.valueOf(value.name());
        }

        public static ChatMessageTypeDto fromBisq2Model(ChatMessageType value) {
            return ChatMessageTypeDto.valueOf(value.name());
        }
    }


    // chat.reactions

    public static class BisqEasyOpenTradeMessageReactionMapping {
        public static BisqEasyOpenTradeMessageReaction toBisq2Model(BisqEasyOpenTradeMessageReactionDto value) {
            return new BisqEasyOpenTradeMessageReaction(
                    value.id(),
                    UserProfileMapping.toBisq2Model(value.senderUserProfile()),
                    value.receiverUserProfileId(),
                    NetworkIdMapping.toBisq2Model(value.receiverNetworkId()),
                    value.chatChannelId(),
                    ChatChannelDomainMapping.toBisq2Model(value.chatChannelDomain()),
                    value.chatMessageId(),
                    value.reactionId(),
                    value.date(),
                    value.isRemoved()
            );
        }

        public static BisqEasyOpenTradeMessageReactionDto fromBisq2Model(BisqEasyOpenTradeMessageReaction value) {
            return new BisqEasyOpenTradeMessageReactionDto(
                    value.getId(),
                    UserProfileMapping.fromBisq2Model(value.getSenderUserProfile()),
                    value.getReceiverUserProfileId(),
                    NetworkIdMapping.fromBisq2Model(value.getReceiverNetworkId()),
                    value.getChatChannelId(),
                    ChatChannelDomainMapping.fromBisq2Model(value.getChatChannelDomain()),
                    value.getChatMessageId(),
                    value.getReactionId(),
                    value.getDate(),
                    value.isRemoved()
            );
        }
    }


    // chat.bisq_easy.open_trades

    public static class BisqEasyOfferbookMessageMapping {
        public BisqEasyOfferbookMessage toBisq2Model(BisqEasyOfferbookMessageDto value) {
            return new BisqEasyOfferbookMessage(
                    value.id(),
                    ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                    value.channelId(),
                    value.authorUserProfileId(),
                    value.bisqEasyOffer().map(BisqEasyOfferMapping::toBisq2Model),
                    value.text(),
                    value.citation().map(CitationMapping::toBisq2Model),
                    value.date(),
                    value.wasEdited(),
                    ChatMessageTypeMapping.toBisq2Model(value.chatMessageType())
            );
        }

        public BisqEasyOfferbookMessageDto fromBisq2Model(BisqEasyOfferbookMessage value) {
            return new BisqEasyOfferbookMessageDto(
                    value.getId(),
                    value.getChannelId(),
                    value.getAuthorUserProfileId(),
                    value.getBisqEasyOffer().map(BisqEasyOfferMapping::fromBisq2Model),
                    value.getText(),
                    value.getCitation().map(CitationMapping::fromBisq2Model),
                    value.getDate(),
                    value.isWasEdited(),
                    ChatMessageTypeMapping.fromBisq2Model(value.getChatMessageType())
            );
        }
    }


    // chat.bisq_easy.open_trades

    public static class BisqEasyOpenTradeChannelMapping {
        // toBisq2Model not provided as we don't have the mutable data in the dto

        public static BisqEasyOpenTradeChannelDto fromBisq2Model(BisqEasyOpenTradeChannel value) {
            return new BisqEasyOpenTradeChannelDto(
                    value.getId(),
                    value.getTradeId(),
                    BisqEasyOfferMapping.fromBisq2Model(value.getBisqEasyOffer()),
                    UserIdentityMapping.fromBisq2Model(value.getMyUserIdentity()),
                    value.getTraders().stream()
                            .map(UserProfileMapping::fromBisq2Model)
                            .collect(Collectors.toSet()),
                    value.getMediator().map(UserProfileMapping::fromBisq2Model)
            );
        }
    }

    public static class BisqEasyOpenTradeMessageMapping {
        public static BisqEasyOpenTradeMessage toBisq2Model(BisqEasyOpenTradeMessageDto value) {
            // citationAuthorUserProfile is not mapped to Bisq 2 model as we use our services for obtaining it.
            return new BisqEasyOpenTradeMessage(
                    value.tradeId(),
                    value.messageId(),
                    value.channelId(),
                    UserProfileMapping.toBisq2Model(value.senderUserProfile()),
                    value.receiverUserProfileId(),
                    NetworkIdMapping.toBisq2Model(value.receiverNetworkId()),
                    value.text().orElse(null),
                    value.citation().map(CitationMapping::toBisq2Model),
                    value.date(),
                    false,
                    value.mediator().map(UserProfileMapping::toBisq2Model),
                    ChatMessageTypeMapping.toBisq2Model(value.chatMessageType()),
                    value.bisqEasyOffer().map(BisqEasyOfferMapping::toBisq2Model),
                    value.chatMessageReactions().stream().map(BisqEasyOpenTradeMessageReactionMapping::toBisq2Model).collect(Collectors.toSet())
            );
        }

        public static BisqEasyOpenTradeMessageDto fromBisq2Model(BisqEasyOpenTradeMessage value,
                                                                 Optional<UserProfileDto> citationAuthorUserProfile) {
            return new BisqEasyOpenTradeMessageDto(
                    value.getTradeId(),
                    value.getId(),
                    value.getChannelId(),
                    UserProfileMapping.fromBisq2Model(value.getSenderUserProfile()),
                    value.getReceiverUserProfileId(),
                    NetworkIdMapping.fromBisq2Model(value.getReceiverNetworkId()),
                    value.getText(),
                    value.getCitation().map(CitationMapping::fromBisq2Model),
                    value.getDate(),
                    value.getMediator().map(UserProfileMapping::fromBisq2Model),
                    ChatMessageTypeMapping.fromBisq2Model(value.getChatMessageType()),
                    value.getBisqEasyOffer().map(BisqEasyOfferMapping::fromBisq2Model),
                    value.getChatMessageReactions().stream().map(BisqEasyOpenTradeMessageReactionMapping::fromBisq2Model).collect(Collectors.toSet()),
                    citationAuthorUserProfile
            );
        }
    }


    // contract

    public static class RoleMapping {
        public static Role toBisq2Model(RoleDto value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case MAKER -> Role.MAKER;
                case TAKER -> Role.TAKER;
                case ESCROW_AGENT -> Role.ESCROW_AGENT;
            };
        }

        public static RoleDto fromBisq2Model(Role value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case MAKER -> RoleDto.MAKER;
                case TAKER -> RoleDto.TAKER;
                case ESCROW_AGENT -> RoleDto.ESCROW_AGENT;
            };
        }
    }


    public static class ContractSignatureDataMapping {
        public static ContractSignatureData toBisq2Model(ContractSignatureDataDto value) {
            return new ContractSignatureData(Base64.getDecoder().decode(value.contractHashEncoded()),
                    Base64.getDecoder().decode(value.signatureEncoded()),
                    PublicKeyMapping.toBisq2Model(value.publicKey()));
        }

        public static ContractSignatureDataDto fromBisq2Model(ContractSignatureData value) {
            return new ContractSignatureDataDto(Base64.getEncoder().encodeToString(value.getContractHash()),
                    Base64.getEncoder().encodeToString(value.getSignature()),
                    PublicKeyMapping.fromBisq2Model(value.getPublicKey()));
        }
    }

    public static class PartyMapping {
        public static Party toBisq2Model(PartyDto value) {
            return new Party(
                    RoleMapping.toBisq2Model(value.role()),
                    NetworkIdMapping.toBisq2Model(value.networkId())
            );
        }

        public static PartyDto fromBisq2Model(Party value) {
            return new PartyDto(
                    RoleMapping.fromBisq2Model(value.getRole()),
                    NetworkIdMapping.fromBisq2Model(value.getNetworkId())
            );
        }
    }

    // contract.bisq_easy

    public static class BisqEasyContractMapping {
        public static BisqEasyContract toBisq2Model(BisqEasyContractDto value) {
            // Maker is created from the offer in base class
            return new BisqEasyContract(
                    value.takeOfferDate(),
                    BisqEasyOfferMapping.toBisq2Model(value.offer()),
                    TradeProtocolType.BISQ_EASY,
                    PartyMapping.toBisq2Model(value.taker()),
                    value.baseSideAmount(),
                    value.quoteSideAmount(),
                    BitcoinPaymentMethodSpecMapping.toBisq2Model(value.baseSidePaymentMethodSpec()),
                    FiatPaymentMethodSpecMapping.toBisq2Model(value.quoteSidePaymentMethodSpec()),
                    value.mediator().map(UserProfileMapping::toBisq2Model),
                    PriceSpecMapping.toBisq2Model(value.priceSpec()),
                    value.marketPrice()
            );
        }

        public static BisqEasyContractDto fromBisq2Model(BisqEasyContract value) {
            return new BisqEasyContractDto(
                    value.getTakeOfferDate(),
                    BisqEasyOfferMapping.fromBisq2Model(value.getOffer()),
                    PartyMapping.fromBisq2Model(value.getMaker()),
                    PartyMapping.fromBisq2Model(value.getTaker()),
                    value.getBaseSideAmount(),
                    value.getQuoteSideAmount(),
                    BitcoinPaymentMethodSpecMapping.fromBisq2Model(value.getBaseSidePaymentMethodSpec()),
                    FiatPaymentMethodSpecMapping.fromBisq2Model(value.getQuoteSidePaymentMethodSpec()),
                    value.getMediator().map(UserProfileMapping::fromBisq2Model),
                    PriceSpecMapping.fromBisq2Model(value.getPriceSpec()),
                    value.getMarketPrice()
            );
        }
    }


    // common.currency

    public static class MarketMapping {
        public static Market toBisq2Model(MarketDto value) {
            return new Market(value.baseCurrencyCode(), value.quoteCurrencyCode(), value.baseCurrencyName(), value.quoteCurrencyName());
        }

        public static MarketDto fromBisq2Model(Market value) {
            return new MarketDto(value.getBaseCurrencyCode(), value.getQuoteCurrencyCode(), value.getBaseCurrencyName(), value.getQuoteCurrencyName());
        }
    }


    // common.monetary

    public static class CoinMapping {
        public static Coin toBisq2Model(CoinDto value) {
            return new Coin(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }

        public static CoinDto fromBisq2Model(Coin value) {
            return new CoinDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }
    }

    public static class FiatMapping {
        public static Fiat toBisq2Model(FiatDto value) {
            return new Fiat(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }

        public static FiatDto fromBisq2Model(Fiat value) {
            return new FiatDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }
    }


    public static class MonetaryMapping {
        public static Monetary toBisq2Model(MonetaryDto value) {
            if (value instanceof FiatDto) {
                return FiatMapping.toBisq2Model((FiatDto) value);
            } else {
                return CoinMapping.toBisq2Model((CoinDto) value);
            }
        }

        public static MonetaryDto fromBisq2Model(Monetary value) {
            if (value instanceof Fiat) {
                return new FiatDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
            } else {
                return new CoinDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
            }
        }
    }

    public static class PriceQuoteMapping {
        public static PriceQuote toBisq2Model(PriceQuoteDto value) {
            return new PriceQuote(
                    value.value(),
                    MonetaryMapping.toBisq2Model(value.baseSideMonetary()),
                    MonetaryMapping.toBisq2Model(value.quoteSideMonetary())
            );
        }

        public static PriceQuoteDto fromBisq2Model(PriceQuote value) {
            return new PriceQuoteDto(
                    value.getValue(),
                    MonetaryMapping.fromBisq2Model(value.getBaseSideMonetary()),
                    MonetaryMapping.fromBisq2Model(value.getQuoteSideMonetary()),
                    value.getPrecision(),
                    value.getLowPrecision(),
                    MarketMapping.fromBisq2Model(value.getMarket())
            );
        }
    }


    // common.network

    public static class AddressByTransportTypeMapMapping {
        public static AddressByTransportTypeMap toBisq2Model(AddressByTransportTypeMapDto value) {
            return new AddressByTransportTypeMap(value.map().entrySet().stream()
                    .collect(Collectors.toMap(entry -> TransportTypeMapping.toBisq2Model(entry.getKey()),
                            entry -> AddressMapping.toBisq2Model(entry.getValue()))));
        }

        public static AddressByTransportTypeMapDto fromBisq2Model(AddressByTransportTypeMap map) {
            return new AddressByTransportTypeMapDto(map.getMap().entrySet().stream()
                    .collect(Collectors.toMap(entry -> TransportTypeMapping.fromBisq2Model(entry.getKey()),
                            entry -> AddressMapping.fromBisq2Model(entry.getValue()))));
        }
    }

    public static class AddressMapping {
        public static Address toBisq2Model(AddressDto value) {
            return Address.from(value.host(), value.port());
        }

        public static AddressDto fromBisq2Model(Address value) {
            return new AddressDto(value.getHost(), value.getPort());
        }
    }

    public static class TransportTypeMapping {
        public static TransportType toBisq2Model(TransportTypeDto value) {
            if (value == TransportTypeDto.CLEAR) {
                return TransportType.CLEAR;
            } else if (value == TransportTypeDto.TOR) {
                return TransportType.TOR;
            } else if (value == TransportTypeDto.I2P) {
                return TransportType.I2P;
            } else {
                throw new IllegalArgumentException("Unsupported enum " + value);
            }
        }

        public static TransportTypeDto fromBisq2Model(TransportType value) {
            if (value == TransportType.CLEAR) {
                return TransportTypeDto.CLEAR;
            } else if (value == TransportType.TOR) {
                return TransportTypeDto.TOR;
            } else if (value == TransportType.I2P) {
                return TransportTypeDto.I2P;
            } else {
                throw new IllegalArgumentException("Unsupported enum " + value);
            }
        }
    }


    // identity

    public static class IdentityMapping {
        public static Identity toBisq2Model(IdentityDto value) {
            return new Identity(
                    value.tag(),
                    NetworkIdMapping.toBisq2Model(value.networkId()),
                    KeyBundleMapping.toBisq2Model(value.keyBundle())
            );
        }

        public static IdentityDto fromBisq2Model(Identity value) {
            return new IdentityDto(
                    value.getTag(),
                    NetworkIdMapping.fromBisq2Model(value.getNetworkId()),
                    KeyBundleMapping.fromBisq2Model(value.getKeyBundle())
            );
        }
    }


    // network.identity

    public static class NetworkIdMapping {
        public static NetworkId toBisq2Model(NetworkIdDto value) {
            return new NetworkId(AddressByTransportTypeMapMapping.toBisq2Model(value.addressByTransportTypeMap()),
                    PubKeyMapping.toBisq2Model(value.pubKey()));
        }

        public static NetworkIdDto fromBisq2Model(NetworkId value) {
            return new NetworkIdDto(AddressByTransportTypeMapMapping.fromBisq2Model(value.getAddressByTransportTypeMap()),
                    PubKeyMapping.fromBisq2Model(value.getPubKey()));
        }
    }


    // offer

    public static class DirectionMapping {
        public static Direction toBisq2Model(DirectionDto value) {
            if (value == DirectionDto.BUY) {
                return Direction.BUY;
            } else {
                return Direction.SELL;
            }
        }

        public static DirectionDto fromBisq2Model(Direction value) {
            if (value.isBuy()) {
                return DirectionDto.BUY;
            } else {
                return DirectionDto.SELL;
            }
        }
    }


    // offer.amount.spec

    public static class AmountSpecMapping {
        public static AmountSpec toBisq2Model(AmountSpecDto value) {
            if (value instanceof RangeAmountSpecDto) {
                return RangeAmountSpecMapping.toBisq2Model((RangeAmountSpecDto) value);
            } else {
                return FixedAmountSpecMapping.toBisq2Model((FixedAmountSpecDto) value);
            }
        }

        public static AmountSpecDto fromBisq2Model(AmountSpec value) {
            if (value instanceof RangeAmountSpec) {
                return RangeAmountSpecMapping.fromBisq2Model((RangeAmountSpec) value);
            } else {
                return FixedAmountSpecMapping.fromBisq2Model((FixedAmountSpec) value);
            }
        }
    }

    public static class BaseSideFixedAmountSpecMapping {
        public static BaseSideFixedAmountSpec toBisq2Model(BaseSideFixedAmountSpecDto value) {
            return new BaseSideFixedAmountSpec(value.getAmount());
        }

        public static BaseSideFixedAmountSpecDto fromBisq2Model(BaseSideFixedAmountSpec value) {
            return new BaseSideFixedAmountSpecDto(value.getAmount());
        }
    }

    public static class BaseSideRangeAmountSpecMapping {
        public static BaseSideRangeAmountSpec toBisq2Model(BaseSideRangeAmountSpecDto value) {
            return new BaseSideRangeAmountSpec(value.getMinAmount(), value.getMaxAmount());
        }

        public static BaseSideRangeAmountSpecDto fromBisq2Model(BaseSideRangeAmountSpec value) {
            return new BaseSideRangeAmountSpecDto(value.getMinAmount(), value.getMaxAmount());
        }
    }

    public static class FixedAmountSpecMapping {
        public static FixedAmountSpec toBisq2Model(FixedAmountSpecDto value) {
            if (value instanceof BaseSideFixedAmountSpecDto) {
                return BaseSideFixedAmountSpecMapping.toBisq2Model((BaseSideFixedAmountSpecDto) value);
            } else if (value instanceof QuoteSideFixedAmountSpecDto) {
                return QuoteSideFixedAmountSpecMapping.toBisq2Model((QuoteSideFixedAmountSpecDto) value);
            } else {
                throw new IllegalArgumentException("Unsupported FixedAmountSpecDto " + value);
            }
        }

        public static FixedAmountSpecDto fromBisq2Model(FixedAmountSpec value) {
            if (value instanceof BaseSideFixedAmountSpec) {
                return BaseSideFixedAmountSpecMapping.fromBisq2Model((BaseSideFixedAmountSpec) value);
            } else if (value instanceof QuoteSideFixedAmountSpec) {
                return QuoteSideFixedAmountSpecMapping.fromBisq2Model((QuoteSideFixedAmountSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported FixedAmountSpec " + value);
            }
        }
    }

    public static class QuoteSideFixedAmountSpecMapping {
        public static QuoteSideFixedAmountSpec toBisq2Model(QuoteSideFixedAmountSpecDto value) {
            return new QuoteSideFixedAmountSpec(value.getAmount());
        }

        public static QuoteSideFixedAmountSpecDto fromBisq2Model(QuoteSideFixedAmountSpec value) {
            return new QuoteSideFixedAmountSpecDto(value.getAmount());
        }
    }

    public static class QuoteSideRangeAmountSpecMapping {
        public static QuoteSideRangeAmountSpec toBisq2Model(QuoteSideRangeAmountSpecDto value) {
            return new QuoteSideRangeAmountSpec(value.getMinAmount(), value.getMaxAmount());
        }

        public static QuoteSideRangeAmountSpecDto fromBisq2Model(QuoteSideRangeAmountSpec value) {
            return new QuoteSideRangeAmountSpecDto(value.getMinAmount(), value.getMaxAmount());
        }
    }

    public static class RangeAmountSpecMapping {
        public static RangeAmountSpec toBisq2Model(RangeAmountSpecDto value) {
            if (value instanceof BaseSideRangeAmountSpecDto) {
                return BaseSideRangeAmountSpecMapping.toBisq2Model((BaseSideRangeAmountSpecDto) value);
            } else if (value instanceof QuoteSideRangeAmountSpecDto) {
                return QuoteSideRangeAmountSpecMapping.toBisq2Model((QuoteSideRangeAmountSpecDto) value);
            } else {
                throw new IllegalArgumentException("Unsupported RangeAmountSpecDto " + value);
            }
        }

        public static RangeAmountSpecDto fromBisq2Model(RangeAmountSpec value) {
            if (value instanceof BaseSideRangeAmountSpec) {
                return BaseSideRangeAmountSpecMapping.fromBisq2Model((BaseSideRangeAmountSpec) value);
            } else if (value instanceof QuoteSideRangeAmountSpec) {
                return QuoteSideRangeAmountSpecMapping.fromBisq2Model((QuoteSideRangeAmountSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported RangeAmountSpec " + value);
            }
        }
    }


    // offer.bisq_easy

    public static class BisqEasyOfferMapping {
        public static BisqEasyOffer toBisq2Model(BisqEasyOfferDto value) {
            return new BisqEasyOffer(value.id(),
                    value.date(),
                    NetworkIdMapping.toBisq2Model(value.makerNetworkId()),
                    DirectionMapping.toBisq2Model(value.direction()),
                    MarketMapping.toBisq2Model(value.market()),
                    AmountSpecMapping.toBisq2Model(value.amountSpec()),
                    PriceSpecMapping.toBisq2Model(value.priceSpec()),
                    value.protocolTypes().stream().map(TradeProtocolTypeMapping::toBisq2Model).collect(Collectors.toList()),
                    value.baseSidePaymentMethodSpecs().stream().map(BitcoinPaymentMethodSpecMapping::toBisq2Model).collect(Collectors.toList()),
                    value.quoteSidePaymentMethodSpecs().stream().map(FiatPaymentMethodSpecMapping::toBisq2Model).collect(Collectors.toList()),
                    value.offerOptions().stream().map(OfferOptionMapping::toBisq2Model).collect(Collectors.toList()),
                    value.supportedLanguageCodes(),
                    value.version(),
                    value.tradeProtocolVersion(),
                    value.appVersion());
        }

        public static BisqEasyOfferDto fromBisq2Model(BisqEasyOffer value) {
            return new BisqEasyOfferDto(value.getId(),
                    value.getDate(),
                    NetworkIdMapping.fromBisq2Model(value.getMakerNetworkId()),
                    DirectionMapping.fromBisq2Model(value.getDirection()),
                    MarketMapping.fromBisq2Model(value.getMarket()),
                    AmountSpecMapping.fromBisq2Model(value.getAmountSpec()),
                    PriceSpecMapping.fromBisq2Model(value.getPriceSpec()),
                    value.getProtocolTypes().stream().map(TradeProtocolTypeMapping::fromBisq2Model).collect(Collectors.toList()),
                    value.getBaseSidePaymentMethodSpecs().stream().map(BitcoinPaymentMethodSpecMapping::fromBisq2Model).collect(Collectors.toList()),
                    value.getQuoteSidePaymentMethodSpecs().stream().map(FiatPaymentMethodSpecMapping::fromBisq2Model).collect(Collectors.toList()),
                    value.getOfferOptions().stream().map(OfferOptionMapping::fromBisq2Model).collect(Collectors.toList()),
                    value.getSupportedLanguageCodes(),
                    value.getVersion(),
                    value.getTradeProtocolVersion(),
                    value.getAppVersion());
        }
    }


    // offer.options

    public static class OfferOptionMapping {
        public static OfferOption toBisq2Model(OfferOptionDto value) {
            if (value instanceof ReputationOptionDto) {
                return ReputationOptionMapping.toBisq2Model((ReputationOptionDto) value);
            } else if (value instanceof TradeTermsOptionDto) {
                return TradeTermsOptionMapping.toBisq2Model((TradeTermsOptionDto) value);
            } else {
                throw new IllegalArgumentException("Unsupported OfferOptionDto " + value);
            }
        }

        public static OfferOptionDto fromBisq2Model(OfferOption value) {
            if (value instanceof ReputationOption) {
                //noinspection deprecation
                return new ReputationOptionDto(((ReputationOption) value).getRequiredTotalReputationScore());
            } else if (value instanceof TradeTermsOption) {
                return new TradeTermsOptionDto(((TradeTermsOption) value).getMakersTradeTerms());
            } else {
                throw new IllegalArgumentException("Unsupported OfferOption " + value);
            }
        }
    }

    public static class ReputationOptionMapping {
        public static ReputationOption toBisq2Model(ReputationOptionDto value) {
            //noinspection deprecation
            return new ReputationOption(value.getRequiredTotalReputationScore());
        }

        public static ReputationOptionDto fromBisq2Model(ReputationOption value) {
            //noinspection deprecation
            return new ReputationOptionDto(value.getRequiredTotalReputationScore());
        }
    }

    public static class TradeTermsOptionMapping {
        public static TradeTermsOption toBisq2Model(TradeTermsOptionDto value) {
            return new TradeTermsOption(value.getMakersTradeTerms());
        }

        public static TradeTermsOptionDto fromBisq2Model(TradeTermsOption value) {
            return new TradeTermsOptionDto(value.getMakersTradeTerms());
        }
    }


    // offer.payment_method

    @SuppressWarnings("deprecation")
    public static class BitcoinPaymentMethodSpecMapping {
        public static BitcoinPaymentMethodSpec toBisq2Model(BitcoinPaymentMethodSpecDto value) {
            String paymentMethod = value.getPaymentMethod();
            BitcoinPaymentMethod method = PaymentMethodSpecUtil.getBitcoinPaymentMethod(paymentMethod);
            return new BitcoinPaymentMethodSpec(method, value.getSaltedMakerAccountId());
        }

        public static BitcoinPaymentMethodSpecDto fromBisq2Model(BitcoinPaymentMethodSpec value) {
            return new BitcoinPaymentMethodSpecDto(value.getPaymentMethod().getPaymentRailName(), value.getSaltedMakerAccountId());
        }
    }

    public static class FiatPaymentMethodSpecMapping {
        public static FiatPaymentMethodSpec toBisq2Model(FiatPaymentMethodSpecDto value) {
            String paymentMethod = value.getPaymentMethod();
            FiatPaymentMethod method = PaymentMethodSpecUtil.getFiatPaymentMethod(paymentMethod);
            return new FiatPaymentMethodSpec(method, value.getSaltedMakerAccountId());
        }

        public static FiatPaymentMethodSpecDto fromBisq2Model(FiatPaymentMethodSpec value) {
            return new FiatPaymentMethodSpecDto(value.getPaymentMethod().getPaymentRailName(), value.getSaltedMakerAccountId());
        }
    }

    public static class PaymentMethodSpecMapping {
        public static PaymentMethodSpec<?> toBisq2Model(PaymentMethodSpecDto value) {
            if (value instanceof FiatPaymentMethodSpecDto) {
                return FiatPaymentMethodSpecMapping.toBisq2Model((FiatPaymentMethodSpecDto) value);
            } else if (value instanceof BitcoinPaymentMethodSpecDto) {
                return BitcoinPaymentMethodSpecMapping.toBisq2Model((BitcoinPaymentMethodSpecDto) value);
            } else {
                throw new IllegalArgumentException("Unsupported PaymentMethodSpecDto " + value);
            }
        }

        public static PaymentMethodSpecDto fromBisq2Model(PaymentMethodSpec<?> value) {
            if (value instanceof FiatPaymentMethodSpec) {
                return FiatPaymentMethodSpecMapping.fromBisq2Model((FiatPaymentMethodSpec) value);
            } else if (value instanceof BitcoinPaymentMethodSpec) {
                return BitcoinPaymentMethodSpecMapping.fromBisq2Model((BitcoinPaymentMethodSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported PaymentMethodSpec " + value);
            }
        }
    }


    // offer.price.spec

    public static class MarketPriceSpecMapping {
        public static MarketPriceSpec toBisq2Model(MarketPriceSpecDto value) {
            return new MarketPriceSpec();
        }

        public static MarketPriceSpecDto fromBisq2Model(MarketPriceSpec value) {
            return new MarketPriceSpecDto();
        }
    }

    public static class FloatPriceSpecMapping {
        public static FloatPriceSpec toBisq2Model(FloatPriceSpecDto value) {
            return new FloatPriceSpec(value.getPercentage());
        }

        public static FloatPriceSpecDto fromBisq2Model(FloatPriceSpec value) {
            return new FloatPriceSpecDto(value.getPercentage());
        }
    }

    public static class FixPriceSpecMapping {
        public static FixPriceSpec toBisq2Model(FixPriceSpecDto value) {
            return new FixPriceSpec(PriceQuoteMapping.toBisq2Model(value.getPriceQuote()));
        }

        public static FixPriceSpecDto fromBisq2Model(FixPriceSpec value) {
            return new FixPriceSpecDto(PriceQuoteMapping.fromBisq2Model(value.getPriceQuote()));
        }
    }

    public static class PriceSpecMapping {
        public static PriceSpec toBisq2Model(PriceSpecDto value) {
            if (value == null) {
                return null;
            }
            if (value instanceof MarketPriceSpecDto marketPriceSpecDto) {
                return MarketPriceSpecMapping.toBisq2Model(marketPriceSpecDto);
            } else if (value instanceof FixPriceSpecDto fixPriceSpecDto) {
                return FixPriceSpecMapping.toBisq2Model(fixPriceSpecDto);
            } else if (value instanceof FloatPriceSpecDto floatPriceSpecDto) {
                return FloatPriceSpecMapping.toBisq2Model(floatPriceSpecDto);
            } else {
                throw new IllegalArgumentException("Unsupported PriceSpecDto " + value);
            }
        }

        public static PriceSpecDto fromBisq2Model(PriceSpec value) {
            if (value == null) {
                return null;
            }
            if (value instanceof MarketPriceSpec marketPriceSpec) {
                return MarketPriceSpecMapping.fromBisq2Model(marketPriceSpec);
            } else if (value instanceof FixPriceSpec fixPriceSpec) {
                return FixPriceSpecMapping.fromBisq2Model(fixPriceSpec);
            } else if (value instanceof FloatPriceSpec floatPriceSpec) {
                return FloatPriceSpecMapping.fromBisq2Model(floatPriceSpec);
            } else {
                throw new IllegalArgumentException("Unsupported PriceSpecDto " + value);
            }
        }
    }


    // security.keys

    public static class KeyPairMapping {
        public static KeyPair toBisq2Model(KeyPairDto value) {
            PublicKey publicKey = PublicKeyMapping.toBisq2Model(value.publicKey());
            PrivateKey privateKey = PrivateKeyMapping.toBisq2Model(value.privateKey());
            return new KeyPair(publicKey, privateKey);
        }

        public static KeyPairDto fromBisq2Model(KeyPair value) {
            PrivateKeyDto privateKeyDto = PrivateKeyMapping.fromBisq2Model(value.getPrivate());
            PublicKeyDto publicKeyDto = PublicKeyMapping.fromBisq2Model(value.getPublic());
            return new KeyPairDto(publicKeyDto, privateKeyDto);
        }
    }

    public static class PrivateKeyMapping {
        public static PrivateKey toBisq2Model(PrivateKeyDto value) {
            try {
                byte[] decoded = Base64.getDecoder().decode(value.encoded());
                return KeyGeneration.generatePrivate(decoded);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate privateKeyEncoded", e);
            }
        }

        public static PrivateKeyDto fromBisq2Model(PrivateKey value) {
            return new PrivateKeyDto(Base64.getEncoder().encodeToString(value.getEncoded()));
        }
    }

    public static class PubKeyMapping {
        public static PubKey toBisq2Model(PubKeyDto value) {
            return new PubKey(PublicKeyMapping.toBisq2Model(value.publicKey()), value.keyId());
        }

        public static PubKeyDto fromBisq2Model(PubKey value) {
            PublicKey publicKey = value.getPublicKey();
            PublicKeyDto publicKeyDto = PublicKeyMapping.fromBisq2Model(publicKey);
            String keyId = value.getKeyId();
            byte[] hashAsBytes = DigestUtil.hash(publicKey.getEncoded());
            String hash = Base64.getEncoder().encodeToString(hashAsBytes);
            String id = Hex.encode(hashAsBytes);
            return new PubKeyDto(publicKeyDto, keyId, hash, id);
        }
    }

    public static class PublicKeyMapping {
        public static PublicKey toBisq2Model(PublicKeyDto value) {
            try {
                byte[] decoded = Base64.getDecoder().decode(value.encoded());
                return KeyGeneration.generatePublic(decoded);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate publicKeyEncoded", e);
            }
        }

        public static PublicKeyDto fromBisq2Model(PublicKey value) {
            return new PublicKeyDto(Base64.getEncoder().encodeToString(value.getEncoded()));
        }
    }

    public static class TorKeyPairMapping {
        public static TorKeyPair toBisq2Model(TorKeyPairDto value) {
            return new TorKeyPair(
                    Base64.getDecoder().decode(value.privateKeyEncoded()),
                    Base64.getDecoder().decode(value.publicKeyEncoded()),
                    value.onionAddress()
            );
        }

        public static TorKeyPairDto fromBisq2Model(TorKeyPair model) {
            return new TorKeyPairDto(
                    Base64.getEncoder().encodeToString(model.getPrivateKey()),
                    Base64.getEncoder().encodeToString(model.getPublicKey()),
                    model.getOnionAddress()
            );
        }
    }

    public static class I2PKeyPairMapping {
        public static I2PKeyPair toBisq2Model(I2PKeyPairDto dto) {
            return new I2PKeyPair(dto.identityBytes(), dto.destinationBytes());
        }

        public static I2PKeyPairDto fromBisq2Model(I2PKeyPair model) {
            return new I2PKeyPairDto(model.getIdentityBytes(), model.getDestinationBytes());
        }
    }


    public static class KeyBundleMapping {
        public static KeyBundle toBisq2Model(KeyBundleDto value) {
            return new KeyBundle(
                    value.keyId(),
                    KeyPairMapping.toBisq2Model(value.keyPair()),
                    TorKeyPairMapping.toBisq2Model(value.torKeyPair()),
                    I2PKeyPairMapping.toBisq2Model(value.i2pKeyPair()));
        }

        public static KeyBundleDto fromBisq2Model(KeyBundle value) {
            return new KeyBundleDto(
                    value.getKeyId(),
                    KeyPairMapping.fromBisq2Model(value.getKeyPair()),
                    TorKeyPairMapping.fromBisq2Model(value.getTorKeyPair()),
                    I2PKeyPairMapping.fromBisq2Model(value.getI2PKeyPair())
            );
        }
    }


    // security.pow

    public static class ProofOfWorkMapping {
        public static ProofOfWork toBisq2Model(ProofOfWorkDto value) {
            return new ProofOfWork(
                    Base64.getDecoder().decode(value.payloadEncoded()),
                    value.counter(),
                    value.challengeEncoded() != null ? Base64.getDecoder().decode(value.challengeEncoded()) : null,
                    value.difficulty(),
                    Base64.getDecoder().decode(value.solutionEncoded()),
                    value.duration()
            );
        }

        public static ProofOfWorkDto fromBisq2Model(ProofOfWork value) {
            return new ProofOfWorkDto(
                    Base64.getEncoder().encodeToString(value.getPayload()),
                    value.getCounter(),
                    value.getChallenge() != null ? Base64.getEncoder().encodeToString(value.getChallenge()) : null,
                    value.getDifficulty(),
                    Base64.getEncoder().encodeToString(value.getSolution()),
                    value.getDuration()
            );
        }
    }


    // settings
    public static class SettingsMapping {
        // toBisq2Model method not implemented as we do not have a settings value object in the domain

        public static SettingsDto fromBisq2Model(SettingsService settingsService) {
            return new SettingsDto(settingsService.getIsTacAccepted().get(),
                    settingsService.getBisqEasyTradeRulesConfirmed().get(),
                    settingsService.getMuSigTradeRulesConfirmed().get(),
                    settingsService.getCloseMyOfferWhenTaken().get(),
                    settingsService.getLanguageTag().get(),
                    settingsService.getSupportedLanguageTags(),
                    settingsService.getMaxTradePriceDeviation().get(),
                    MarketMapping.fromBisq2Model(settingsService.getSelectedMuSigMarket().get()),
                    settingsService.getNumDaysAfterRedactingTradeData().get(),
                    settingsService.getUseAnimations().get()
            );
        }
    }

    // paymentaccount
    public static class FiatPaymentMethodItemMapping {
        public static FiatPaymentMethodItemDto fromBisq2Model(FiatPaymentMethod paymentMethod) {
            java.util.List<Country> supportedCountries = paymentMethod.getSupportedCountries();
            java.util.List<String> countryCodes = supportedCountries.stream()
                    .map(Country::getCode)
                    .sorted()
                    .toList();
            String countryNames = CountryRepository.matchesAllCountries(countryCodes)
                    ? Res.get("paymentAccounts.allCountries")
                    : supportedCountries.stream()
                    .map(Country::getName)
                    .sorted()
                    .collect(Collectors.joining(", "));

            return new FiatPaymentMethodItemDto(
                    FiatPaymentRailMapping.fromBisq2Model(paymentMethod.getPaymentRail()),
                    paymentMethod.getShortDisplayString(),
                    paymentMethod.getSupportedCurrencyCodesAsDisplayString(),
                    paymentMethod.getSupportedCurrencyDisplayNameAndCodeAsDisplayString(),
                    countryNames,
                    paymentMethod.getPaymentRail().getChargebackRisk().toString()
            );
        }
    }

    /**
     * Mapping for polymorphic fiat accounts.
     * Supports all FiatPaymentRail types through the FiatAccountDto interface.
     */
    public static class FiatPaymentRailMapping {
        public static FiatPaymentRail toBisq2Model(FiatPaymentRailDto value) {
            if (value == null) {
                return null;
            }
            return FiatPaymentRail.valueOf(value.name());
        }

        public static FiatPaymentRailDto fromBisq2Model(FiatPaymentRail value) {
            if (value == null) {
                return null;
            }
            return FiatPaymentRailDto.valueOf(value.name());
        }
    }

    public static class FiatAccountMapping {
        public static Account<? extends PaymentMethod<?>, ?> toBisq2Model(FiatAccountDto dto) {
            if (dto == null) {
                throw new IllegalArgumentException("FiatAccountDto cannot be null");
            }

            return switch (FiatPaymentRailMapping.toBisq2Model(dto.paymentRail())) {
                case ACH_TRANSFER -> {
                    if (dto instanceof AchTransferAccountDto typedDto) {
                        yield AchTransferAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected AchTransferAccountDto for ACH_TRANSFER payment rail");
                }
                case ADVANCED_CASH -> {
                    if (dto instanceof AdvancedCashAccountDto typedDto) {
                        yield AdvancedCashAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected AdvancedCashAccountDto for ADVANCED_CASH payment rail");
                }
                case ALI_PAY -> {
                    if (dto instanceof AliPayAccountDto typedDto) {
                        yield AliPayAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected AliPayAccountDto for ALI_PAY payment rail");
                }
                case AMAZON_GIFT_CARD -> {
                    if (dto instanceof AmazonGiftCardAccountDto typedDto) {
                        yield AmazonGiftCardAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected AmazonGiftCardAccountDto for AMAZON_GIFT_CARD payment rail");
                }
                case BIZUM -> {
                    if (dto instanceof BizumAccountDto typedDto) {
                        yield BizumAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected BizumAccountDto for BIZUM payment rail");
                }
                case CASH_BY_MAIL -> {
                    if (dto instanceof CashByMailAccountDto typedDto) {
                        yield CashByMailAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected CashByMailAccountDto for CASH_BY_MAIL payment rail");
                }
                case CASH_DEPOSIT -> {
                    if (dto instanceof CashDepositAccountDto typedDto) {
                        yield CashDepositAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected CashDepositAccountDto for CASH_DEPOSIT payment rail");
                }
                case CUSTOM -> {
                    if (dto instanceof bisq.api.dto.account.fiat.UserDefinedFiatAccountDto typedDto) {
                        yield UserDefinedFiatAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected UserDefinedFiatAccountDto for CUSTOM payment rail");
                }
                case DOMESTIC_WIRE_TRANSFER -> {
                    if (dto instanceof DomesticWireTransferAccountDto typedDto) {
                        yield DomesticWireTransferAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected DomesticWireTransferAccountDto for DOMESTIC_WIRE_TRANSFER payment rail");
                }
                case F2F -> {
                    if (dto instanceof F2FAccountDto typedDto) {
                        yield F2FAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected F2FAccountDto for F2F payment rail");
                }
                case FASTER_PAYMENTS -> {
                    if (dto instanceof FasterPaymentsAccountDto typedDto) {
                        yield FasterPaymentsAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected FasterPaymentsAccountDto for FASTER_PAYMENTS payment rail");
                }
                case HAL_CASH -> {
                    if (dto instanceof HalCashAccountDto typedDto) {
                        yield HalCashAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected HalCashAccountDto for HAL_CASH payment rail");
                }
                case IMPS -> {
                    if (dto instanceof ImpsAccountDto typedDto) {
                        yield ImpsAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected ImpsAccountDto for IMPS payment rail");
                }
                case INTERAC_E_TRANSFER -> {
                    if (dto instanceof InteracETransferAccountDto typedDto) {
                        yield InteracETransferAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected InteracETransferAccountDto for INTERAC_E_TRANSFER payment rail");
                }
                case MERCADO_PAGO -> {
                    if (dto instanceof MercadoPagoAccountDto typedDto) {
                        yield MercadoPagoAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected MercadoPagoAccountDto for MERCADO_PAGO payment rail");
                }
                case MONESE -> {
                    if (dto instanceof MoneseAccountDto typedDto) {
                        yield MoneseAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected MoneseAccountDto for MONESE payment rail");
                }
                case MONEY_BEAM -> {
                    if (dto instanceof MoneyBeamAccountDto typedDto) {
                        yield MoneyBeamAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected MoneyBeamAccountDto for MONEY_BEAM payment rail");
                }
                case MONEY_GRAM -> {
                    if (dto instanceof MoneyGramAccountDto typedDto) {
                        yield MoneyGramAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected MoneyGramAccountDto for MONEY_GRAM payment rail");
                }
                case NATIONAL_BANK -> {
                    if (dto instanceof NationalBankAccountDto typedDto) {
                        yield NationalBankAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected NationalBankAccountDto for NATIONAL_BANK payment rail");
                }
                case NEFT -> {
                    if (dto instanceof NeftAccountDto typedDto) {
                        yield NeftAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected NeftAccountDto for NEFT payment rail");
                }
                case PAYSERA -> {
                    if (dto instanceof PayseraAccountDto typedDto) {
                        yield PayseraAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected PayseraAccountDto for PAYSERA payment rail");
                }
                case PAY_ID -> {
                    if (dto instanceof PayIdAccountDto typedDto) {
                        yield PayIdAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected PayIdAccountDto for PAY_ID payment rail");
                }
                case PERFECT_MONEY -> {
                    if (dto instanceof PerfectMoneyAccountDto typedDto) {
                        yield PerfectMoneyAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected PerfectMoneyAccountDto for PERFECT_MONEY payment rail");
                }
                case PIN_4 -> {
                    if (dto instanceof Pin4AccountDto typedDto) {
                        yield Pin4AccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected Pin4AccountDto for PIN_4 payment rail");
                }
                case PIX -> {
                    if (dto instanceof PixAccountDto typedDto) {
                        yield PixAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected PixAccountDto for PIX payment rail");
                }
                case PROMPT_PAY -> {
                    if (dto instanceof PromptPayAccountDto typedDto) {
                        yield PromptPayAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected PromptPayAccountDto for PROMPT_PAY payment rail");
                }
                case REVOLUT -> {
                    if (dto instanceof RevolutAccountDto typedDto) {
                        yield RevolutAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected RevolutAccountDto for REVOLUT payment rail");
                }
                case SAME_BANK -> {
                    if (dto instanceof SameBankAccountDto typedDto) {
                        yield SameBankAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SameBankAccountDto for SAME_BANK payment rail");
                }
                case SATISPAY -> {
                    if (dto instanceof SatispayAccountDto typedDto) {
                        yield SatispayAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SatispayAccountDto for SATISPAY payment rail");
                }
                case SBP -> {
                    if (dto instanceof SbpAccountDto typedDto) {
                        yield SbpAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SbpAccountDto for SBP payment rail");
                }
                case SEPA -> {
                    if (dto instanceof SepaAccountDto typedDto) {
                        yield SepaAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SepaAccountDto for SEPA payment rail");
                }
                case SEPA_INSTANT -> {
                    if (dto instanceof SepaInstantAccountDto typedDto) {
                        yield SepaInstantAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SepaInstantAccountDto for SEPA_INSTANT payment rail");
                }
                case STRIKE -> {
                    if (dto instanceof StrikeAccountDto typedDto) {
                        yield StrikeAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected StrikeAccountDto for STRIKE payment rail");
                }
                case SWIFT -> {
                    if (dto instanceof SwiftAccountDto typedDto) {
                        yield SwiftAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SwiftAccountDto for SWIFT payment rail");
                }
                case SWISH -> {
                    if (dto instanceof SwishAccountDto typedDto) {
                        yield SwishAccountDtoMapping.toBisq2Model(typedDto);
                    }
                    throw new IllegalArgumentException("Expected SwishAccountDto for SWISH payment rail");
                }
                case UPHOLD -> {
                    if (dto instanceof UpholdAccountDto typedDto) {
                        UpholdAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.UpholdAccountPayload payload = new bisq.account.accounts.fiat.UpholdAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.selectedCurrencyCodes(),
                                payloadDto.holderName(),
                                payloadDto.accountId()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.UpholdAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected UpholdAccountDto for UPHOLD payment rail");
                }
                case UPI -> {
                    if (dto instanceof UpiAccountDto typedDto) {
                        UpiAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.UpiAccountPayload payload = new bisq.account.accounts.fiat.UpiAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.countryCode(),
                                payloadDto.virtualPaymentAddress()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.UpiAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected UpiAccountDto for UPI payment rail");
                }
                case US_POSTAL_MONEY_ORDER -> {
                    if (dto instanceof USPostalMoneyOrderAccountDto typedDto) {
                        USPostalMoneyOrderAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.USPostalMoneyOrderAccountPayload payload = new bisq.account.accounts.fiat.USPostalMoneyOrderAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.holderName(),
                                payloadDto.postalAddress()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.USPostalMoneyOrderAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected USPostalMoneyOrderAccountDto for US_POSTAL_MONEY_ORDER payment rail");
                }
                case WECHAT_PAY -> {
                    if (dto instanceof WeChatPayAccountDto typedDto) {
                        WeChatPayAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.WeChatPayAccountPayload payload = new bisq.account.accounts.fiat.WeChatPayAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.accountNr()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.WeChatPayAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected WeChatPayAccountDto for WECHAT_PAY payment rail");
                }
                case WISE -> {
                    if (dto instanceof WiseAccountDto typedDto) {
                        WiseAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.WiseAccountPayload payload = new bisq.account.accounts.fiat.WiseAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.selectedCurrencyCodes(),
                                payloadDto.holderName(),
                                payloadDto.email()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.WiseAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected WiseAccountDto for WISE payment rail");
                }
                case WISE_USD -> {
                    if (dto instanceof WiseUsdAccountDto typedDto) {
                        WiseUsdAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.WiseUsdAccountPayload payload = new bisq.account.accounts.fiat.WiseUsdAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.countryCode(),
                                payloadDto.holderName(),
                                payloadDto.email(),
                                payloadDto.beneficiaryAddress()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.WiseUsdAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected WiseUsdAccountDto for WISE_USD payment rail");
                }
                case ZELLE -> {
                    if (dto instanceof ZelleAccountDto typedDto) {
                        ZelleAccountPayloadDto payloadDto = typedDto.accountPayload();
                        bisq.account.accounts.fiat.ZelleAccountPayload payload = new bisq.account.accounts.fiat.ZelleAccountPayload(
                                bisq.common.util.StringUtils.createUid(),
                                payloadDto.holderName(),
                                payloadDto.emailOrMobileNr()
                        );
                        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
                        KeyType keyType = KeyType.EC;
                        yield new bisq.account.accounts.fiat.ZelleAccount(
                                bisq.common.util.StringUtils.createUid(),
                                System.currentTimeMillis(),
                                typedDto.accountName(),
                                payload,
                                keyPair,
                                keyType,
                                AccountOrigin.BISQ2_NEW
                        );
                    }
                    throw new IllegalArgumentException("Expected ZelleAccountDto for ZELLE payment rail");
                }
                default -> throw new IllegalArgumentException("Unsupported payment rail: " + dto.paymentRail());
            };
        }

        public static FiatAccountDto fromBisq2Model(Account<? extends PaymentMethod<?>, ?> account) {
            if (account == null) {
                throw new IllegalArgumentException("Account cannot be null");
            }

            if (account instanceof bisq.account.accounts.fiat.AchTransferAccount typed) {
                return AchTransferAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.AdvancedCashAccount typed) {
                return AdvancedCashAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.AliPayAccount typed) {
                return AliPayAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.AmazonGiftCardAccount typed) {
                return AmazonGiftCardAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.BizumAccount typed) {
                return BizumAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.CashByMailAccount typed) {
                return CashByMailAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.CashDepositAccount typed) {
                return CashDepositAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.UserDefinedFiatAccount typed) {
                return UserDefinedFiatAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.DomesticWireTransferAccount typed) {
                return DomesticWireTransferAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.F2FAccount typed) {
                return F2FAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.FasterPaymentsAccount typed) {
                return FasterPaymentsAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.HalCashAccount typed) {
                return HalCashAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.ImpsAccount typed) {
                return ImpsAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.InteracETransferAccount typed) {
                return InteracETransferAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.MercadoPagoAccount typed) {
                return MercadoPagoAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.MoneseAccount typed) {
                return MoneseAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.MoneyBeamAccount typed) {
                return MoneyBeamAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.MoneyGramAccount typed) {
                return MoneyGramAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.NationalBankAccount typed) {
                return NationalBankAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.NeftAccount typed) {
                return NeftAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.PayseraAccount typed) {
                return PayseraAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.PayIdAccount typed) {
                return PayIdAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.PerfectMoneyAccount typed) {
                return PerfectMoneyAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.Pin4Account typed) {
                return Pin4AccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.PixAccount typed) {
                return PixAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.PromptPayAccount typed) {
                return PromptPayAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.RevolutAccount typed) {
                return RevolutAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SameBankAccount typed) {
                return SameBankAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SatispayAccount typed) {
                return SatispayAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SbpAccount typed) {
                return SbpAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SepaAccount typed) {
                return SepaAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SepaInstantAccount typed) {
                return SepaInstantAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.StrikeAccount typed) {
                return StrikeAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SwiftAccount typed) {
                return SwiftAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.SwishAccount typed) {
                return SwishAccountDtoMapping.fromBisq2Model(typed);
            }

            if (account instanceof bisq.account.accounts.fiat.UpholdAccount typed) {
                return new UpholdAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.UPHOLD),
                        new UpholdAccountPayloadDto(
                                typed.getAccountPayload().getSelectedCurrencyCodes(),
                                typed.getAccountPayload().getHolderName(),
                                typed.getAccountPayload().getAccountId()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.UpiAccount typed) {
                return new UpiAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.UPI),
                        new UpiAccountPayloadDto(
                                typed.getAccountPayload().getCountryCode(),
                                typed.getAccountPayload().getVirtualPaymentAddress()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.USPostalMoneyOrderAccount typed) {
                return new USPostalMoneyOrderAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.US_POSTAL_MONEY_ORDER),
                        new USPostalMoneyOrderAccountPayloadDto(
                                typed.getAccountPayload().getHolderName(),
                                typed.getAccountPayload().getPostalAddress()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.WeChatPayAccount typed) {
                return new WeChatPayAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.WECHAT_PAY),
                        new WeChatPayAccountPayloadDto(
                                typed.getAccountPayload().getAccountNr()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.WiseAccount typed) {
                return new WiseAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.WISE),
                        new WiseAccountPayloadDto(
                                typed.getAccountPayload().getSelectedCurrencyCodes(),
                                typed.getAccountPayload().getHolderName(),
                                typed.getAccountPayload().getEmail()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.WiseUsdAccount typed) {
                return new WiseUsdAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.WISE_USD),
                        new WiseUsdAccountPayloadDto(
                                typed.getAccountPayload().getCountryCode(),
                                typed.getAccountPayload().getHolderName(),
                                typed.getAccountPayload().getEmail(),
                                typed.getAccountPayload().getBeneficiaryAddress()
                        )
                );
            }

            if (account instanceof bisq.account.accounts.fiat.ZelleAccount typed) {
                return new ZelleAccountDto(
                        typed.getAccountName(),
                        FiatPaymentRailMapping.fromBisq2Model(FiatPaymentRail.ZELLE),
                        new ZelleAccountPayloadDto(
                                typed.getAccountPayload().getHolderName(),
                                typed.getAccountPayload().getEmailOrMobileNr()
                        )
                );
            }

            throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getSimpleName());
        }
    }

    // trade

    public static class TradeRoleMapping {
        public static TradeRole toBisq2Model(TradeRoleDto value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BUYER_AS_TAKER -> TradeRole.BUYER_AS_TAKER;
                case BUYER_AS_MAKER -> TradeRole.BUYER_AS_MAKER;
                case SELLER_AS_TAKER -> TradeRole.SELLER_AS_TAKER;
                case SELLER_AS_MAKER -> TradeRole.SELLER_AS_MAKER;
            };
        }

        public static TradeRoleDto fromBisq2Model(TradeRole value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case BUYER_AS_TAKER -> TradeRoleDto.BUYER_AS_TAKER;
                case BUYER_AS_MAKER -> TradeRoleDto.BUYER_AS_MAKER;
                case SELLER_AS_TAKER -> TradeRoleDto.SELLER_AS_TAKER;
                case SELLER_AS_MAKER -> TradeRoleDto.SELLER_AS_MAKER;
            };
        }
    }

    public static class TradeProtocolFailureMapping {
        public static TradeProtocolFailure toBisq2Model(TradeProtocolFailureDto value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case UNKNOWN -> TradeProtocolFailure.UNKNOWN;
                case PRICE_DEVIATION -> TradeProtocolFailure.PRICE_DEVIATION;
                case NO_MATCHING_OFFER_FOUND -> TradeProtocolFailure.NO_MATCHING_OFFER_FOUND;
                case MEDIATORS_NOT_MATCHING -> TradeProtocolFailure.MEDIATORS_NOT_MATCHING;
            };
        }

        public static TradeProtocolFailureDto fromBisq2Model(TradeProtocolFailure value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case UNKNOWN -> TradeProtocolFailureDto.UNKNOWN;
                case PRICE_DEVIATION -> TradeProtocolFailureDto.PRICE_DEVIATION;
                case NO_MATCHING_OFFER_FOUND -> TradeProtocolFailureDto.NO_MATCHING_OFFER_FOUND;
                case MEDIATORS_NOT_MATCHING -> TradeProtocolFailureDto.MEDIATORS_NOT_MATCHING;
            };
        }
    }


    // trade.bisq_easy

    public static class BisqEasyTradePartyMapping {
        public static BisqEasyTradeParty toBisq2Model(BisqEasyTradePartyDto value) {
            return new BisqEasyTradeParty(
                    NetworkIdMapping.toBisq2Model(value.networkId())
            );
        }

        public static BisqEasyTradePartyDto fromBisq2Model(BisqEasyTradeParty model) {
            return new BisqEasyTradePartyDto(
                    NetworkIdMapping.fromBisq2Model(model.getNetworkId())
            );
        }
    }

    public static class BisqEasyTradeMapping {
        //todo we dont have the mutable data in the dto
       /* public static BisqEasyTrade toBisq2Model(BisqEasyTradeDto value) {
            return new BisqEasyTrade(
                    BisqEasyTradeState.INIT,
                    value.id(),
                    TradeRoleMapping.toBisq2Model(value.tradeRole()),
                    IdentityMapping.toBisq2Model(value.myIdentity()),
                    BisqEasyTradePartyMapping.toBisq2Model(value.taker()),
                    BisqEasyTradePartyMapping.toBisq2Model(value.maker())
            );
        }*/

        public static BisqEasyTradeDto fromBisq2Model(BisqEasyTrade value) {
            return new BisqEasyTradeDto(
                    BisqEasyContractMapping.fromBisq2Model(value.getContract()),
                    value.getId(),
                    TradeRoleMapping.fromBisq2Model(value.getTradeRole()),
                    IdentityMapping.fromBisq2Model(value.getMyIdentity()),
                    BisqEasyTradePartyMapping.fromBisq2Model(value.getTaker()),
                    BisqEasyTradePartyMapping.fromBisq2Model(value.getMaker()),
                    BisqEasyTradeStateMapping.fromBisq2Model(value.getTradeState()),
                    value.getPaymentAccountData().get(),
                    value.getBitcoinPaymentData().get(),
                    value.getPaymentProof().get(),
                    RoleMapping.fromBisq2Model(value.getInterruptTradeInitiator().get()),
                    value.getErrorMessage(),
                    value.getErrorStackTrace(),
                    value.getPeersErrorMessage(),
                    value.getPeersErrorStackTrace()
            );
        }
    }


    // trade.bisq_easy.protocol

    public static class BisqEasyTradeStateMapping {
        public static BisqEasyTradeState toBisq2Model(BisqEasyTradeStateDto value) {
            if (value == null) {
                return null;
            }

            return switch (value) {
                case INIT -> BisqEasyTradeState.INIT;
                case TAKER_SENT_TAKE_OFFER_REQUEST -> BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_ ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_ ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_ ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case BUYER_SENT_FIAT_SENT_CONFIRMATION -> BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION;
                case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION ->
                        BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
                case SELLER_CONFIRMED_FIAT_RECEIPT -> BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT;
                case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION ->
                        BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION;
                case SELLER_SENT_BTC_SENT_CONFIRMATION -> BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION;
                case BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION;
                case BTC_CONFIRMED -> BisqEasyTradeState.BTC_CONFIRMED;
                case REJECTED -> BisqEasyTradeState.REJECTED;
                case PEER_REJECTED -> BisqEasyTradeState.PEER_REJECTED;
                case CANCELLED -> BisqEasyTradeState.CANCELLED;
                case PEER_CANCELLED -> BisqEasyTradeState.PEER_CANCELLED;
                case FAILED -> BisqEasyTradeState.FAILED;
                case FAILED_AT_PEER -> BisqEasyTradeState.FAILED_AT_PEER;
            };
        }

        public static BisqEasyTradeStateDto fromBisq2Model(BisqEasyTradeState value) {
            if (value == null) {
                return null;
            }

            return switch (value) {
                case INIT -> BisqEasyTradeStateDto.INIT;
                case TAKER_SENT_TAKE_OFFER_REQUEST -> BisqEasyTradeStateDto.TAKER_SENT_TAKE_OFFER_REQUEST;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_ ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_ ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_ ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_;
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS ->
                        BisqEasyTradeStateDto.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_;
                case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                        BisqEasyTradeStateDto.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
                case BUYER_SENT_FIAT_SENT_CONFIRMATION -> BisqEasyTradeStateDto.BUYER_SENT_FIAT_SENT_CONFIRMATION;
                case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION ->
                        BisqEasyTradeStateDto.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
                case SELLER_CONFIRMED_FIAT_RECEIPT -> BisqEasyTradeStateDto.SELLER_CONFIRMED_FIAT_RECEIPT;
                case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION ->
                        BisqEasyTradeStateDto.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION;
                case SELLER_SENT_BTC_SENT_CONFIRMATION -> BisqEasyTradeStateDto.SELLER_SENT_BTC_SENT_CONFIRMATION;
                case BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> BisqEasyTradeStateDto.BUYER_RECEIVED_BTC_SENT_CONFIRMATION;
                case BTC_CONFIRMED -> BisqEasyTradeStateDto.BTC_CONFIRMED;
                case REJECTED -> BisqEasyTradeStateDto.REJECTED;
                case PEER_REJECTED -> BisqEasyTradeStateDto.PEER_REJECTED;
                case CANCELLED -> BisqEasyTradeStateDto.CANCELLED;
                case PEER_CANCELLED -> BisqEasyTradeStateDto.PEER_CANCELLED;
                case FAILED -> BisqEasyTradeStateDto.FAILED;
                case FAILED_AT_PEER -> BisqEasyTradeStateDto.FAILED_AT_PEER;
            };
        }
    }

    // user.identity

    public static class UserIdentityMapping {
        public static UserIdentity toBisq2Model(UserIdentityDto value) {
            return new UserIdentity(
                    IdentityMapping.toBisq2Model(value.identity()),
                    UserProfileMapping.toBisq2Model(value.userProfile())
            );
        }

        public static UserIdentityDto fromBisq2Model(UserIdentity value) {
            return new UserIdentityDto(
                    IdentityMapping.fromBisq2Model(value.getIdentity()),
                    UserProfileMapping.fromBisq2Model(value.getUserProfile())
            );
        }
    }


    // user.profile

    public static class UserProfileMapping {
        public static UserProfile toBisq2Model(UserProfileDto value) {
            return new UserProfile(value.version(),
                    value.nickName(),
                    ProofOfWorkMapping.toBisq2Model(value.proofOfWork()),
                    value.avatarVersion(),
                    NetworkIdMapping.toBisq2Model(value.networkId()),
                    value.terms(),
                    value.statement(),
                    value.applicationVersion()
            );
        }

        public static UserProfileDto fromBisq2Model(UserProfile value) {
            return new UserProfileDto(
                    value.getVersion(),
                    value.getNickName(),
                    ProofOfWorkMapping.fromBisq2Model(value.getProofOfWork()),
                    value.getAvatarVersion(),
                    NetworkIdMapping.fromBisq2Model(value.getNetworkId()),
                    value.getTerms(),
                    value.getStatement(),
                    value.getApplicationVersion(),
                    value.getNym(),
                    value.getUserName(),
                    value.getPublishDate()
            );
        }
    }


    // user.reputation

    public static class ReputationScoreMapping {
        public static ReputationScore toBisq2Model(ReputationScoreDto value) {
            return new ReputationScore(value.totalScore(), value.fiveSystemScore(), value.ranking());
        }

        public static ReputationScoreDto fromBisq2Model(ReputationScore value) {
            return new ReputationScoreDto(value.getTotalScore(), value.getFiveSystemScore(), value.getRanking());
        }
    }
}
