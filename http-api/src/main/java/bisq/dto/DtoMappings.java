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

package bisq.dto;

import bisq.account.AccountService;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.Market;
import bisq.common.encoding.Hex;
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
import bisq.dto.account.UserDefinedFiatAccountDto;
import bisq.dto.account.UserDefinedFiatAccountPayloadDto;
import bisq.dto.account.protocol_type.TradeProtocolTypeDto;
import bisq.dto.chat.ChatMessageTypeDto;
import bisq.dto.chat.CitationDto;
import bisq.dto.chat.bisq_easy.offerbook.BisqEasyOfferbookMessageDto;
import bisq.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto;
import bisq.dto.common.currency.MarketDto;
import bisq.dto.common.monetary.CoinDto;
import bisq.dto.common.monetary.FiatDto;
import bisq.dto.common.monetary.MonetaryDto;
import bisq.dto.common.monetary.PriceQuoteDto;
import bisq.dto.common.network.AddressByTransportTypeMapDto;
import bisq.dto.common.network.AddressDto;
import bisq.dto.common.network.TransportTypeDto;
import bisq.dto.contract.ContractSignatureDataDto;
import bisq.dto.contract.PartyDto;
import bisq.dto.contract.RoleDto;
import bisq.dto.contract.bisq_easy.BisqEasyContractDto;
import bisq.dto.identity.IdentityDto;
import bisq.dto.network.identity.NetworkIdDto;
import bisq.dto.offer.DirectionDto;
import bisq.dto.offer.amount.spec.*;
import bisq.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.dto.offer.options.OfferOptionDto;
import bisq.dto.offer.options.ReputationOptionDto;
import bisq.dto.offer.options.TradeTermsOptionDto;
import bisq.dto.offer.payment_method.BitcoinPaymentMethodSpecDto;
import bisq.dto.offer.payment_method.FiatPaymentMethodSpecDto;
import bisq.dto.offer.payment_method.PaymentMethodSpecDto;
import bisq.dto.offer.price.spec.FixPriceSpecDto;
import bisq.dto.offer.price.spec.FloatPriceSpecDto;
import bisq.dto.offer.price.spec.MarketPriceSpecDto;
import bisq.dto.offer.price.spec.PriceSpecDto;
import bisq.dto.security.keys.*;
import bisq.dto.security.pow.ProofOfWorkDto;
import bisq.dto.settings.SettingsDto;
import bisq.dto.trade.TradeRoleDto;
import bisq.dto.trade.bisq_easy.BisqEasyTradeDto;
import bisq.dto.trade.bisq_easy.BisqEasyTradePartyDto;
import bisq.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.dto.user.identity.UserIdentityDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.*;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.options.ReputationOption;
import bisq.offer.options.TradeTermsOption;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.DigestUtil;
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
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
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
                case BISQ_MU_SIG -> TradeProtocolType.BISQ_MU_SIG;
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
                case BISQ_MU_SIG -> TradeProtocolTypeDto.BISQ_MU_SIG;
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
        //todo we dont have the mutable data in the dto
        /*public static BisqEasyOpenTradeChannel toBisq2Model(BisqEasyOpenTradeChannelDto value) {
            return new BisqEasyOpenTradeChannel(
                    value.id(),
                    value.tradeId(),
                    BisqEasyOfferMapping.toBisq2Model(value.bisqEasyOffer()),
                    UserIdentityMapping.toBisq2Model(value.myUserIdentity()),
                    value.traders().stream()
                            .map(UserProfileMapping::toBisq2Model)
                            .collect(Collectors.toSet()),
                    value.mediator().map(UserProfileMapping::toBisq2Model),
                    new HashSet<>(),
                    false,
                    ChatChannelNotificationType.GLOBAL_DEFAULT
            );
        }*/

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
            String baseCurrencyCode = value.market().baseCurrencyCode();
            String quoteCurrencyCode = value.market().quoteCurrencyCode();
            if (baseCurrencyCode.equals("BTC")) {
                Monetary baseSideMonetary = Coin.asBtcFromFaceValue(1);
                Monetary quoteSideMonetary = Fiat.from(value.value(), quoteCurrencyCode);
                return new PriceQuote(value.value(), baseSideMonetary, quoteSideMonetary);
            } else {
                throw new UnsupportedOperationException("Altcoin price quote mapping is not supported yet");
            }
        }

        public static PriceQuoteDto fromBisq2Model(PriceQuote value) {
            return new PriceQuoteDto(value.getValue(), MarketMapping.fromBisq2Model(value.getMarket()));
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
            return new Address(value.host(), value.port());
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
            if (value == Direction.BUY) {
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
                    value.supportedLanguageCodes());
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
                    value.getSupportedLanguageCodes());
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

    public static class BitcoinPaymentMethodSpecMapping {
        public static BitcoinPaymentMethodSpec toBisq2Model(BitcoinPaymentMethodSpecDto value) {
            String paymentMethod = value.getPaymentMethod();
            BitcoinPaymentMethod method = PaymentMethodSpecUtil.getBitcoinPaymentMethod(paymentMethod);
            return new BitcoinPaymentMethodSpec(method, value.getSaltedMakerAccountId());
        }

        public static BitcoinPaymentMethodSpecDto fromBisq2Model(BitcoinPaymentMethodSpec value) {
            return new BitcoinPaymentMethodSpecDto(value.getPaymentMethod().getName(), value.getSaltedMakerAccountId());
        }
    }

    public static class FiatPaymentMethodSpecMapping {
        public static FiatPaymentMethodSpec toBisq2Model(FiatPaymentMethodSpecDto value) {
            String paymentMethod = value.getPaymentMethod();
            FiatPaymentMethod method = PaymentMethodSpecUtil.getFiatPaymentMethod(paymentMethod);
            return new FiatPaymentMethodSpec(method, value.getSaltedMakerAccountId());
        }

        public static FiatPaymentMethodSpecDto fromBisq2Model(FiatPaymentMethodSpec value) {
            return new FiatPaymentMethodSpecDto(value.getPaymentMethod().getName(), value.getSaltedMakerAccountId());
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


    public static class KeyBundleMapping {
        public static KeyBundle toBisq2Model(KeyBundleDto value) {
            return new KeyBundle(
                    value.keyId(),
                    KeyPairMapping.toBisq2Model(value.keyPair()),
                    TorKeyPairMapping.toBisq2Model(value.torKeyPair())
            );
        }

        public static KeyBundleDto fromBisq2Model(KeyBundle value) {
            return new KeyBundleDto(
                    value.getKeyId(),
                    KeyPairMapping.fromBisq2Model(value.getKeyPair()),
                    TorKeyPairMapping.fromBisq2Model(value.getTorKeyPair())
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
                    settingsService.getTradeRulesConfirmed().get(),
                    settingsService.getCloseMyOfferWhenTaken().get(),
                    settingsService.getLanguageCode().get(),
                    settingsService.getSupportedLanguageCodes(),
                    settingsService.getMaxTradePriceDeviation().get(),
                    MarketMapping.fromBisq2Model(settingsService.getSelectedMarket().get()),
                    settingsService.getNumDaysAfterRedactingTradeData().get(),
                    settingsService.getUseAnimations().get()
            );
        }
    }

    // paymentaccount
    public static class UserDefinedFiatAccountMapping {
        // toBisq2Model method not implemented, as we get accountName, accountData props for account creation calls

        public static UserDefinedFiatAccountDto fromBisq2Model(UserDefinedFiatAccount account) {
            return new UserDefinedFiatAccountDto(
                    account.getAccountName(),
                    new UserDefinedFiatAccountPayloadDto(
                            account.getAccountPayload().getAccountData()
                    )
            );
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