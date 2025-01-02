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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.encoding.Hex;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.dto.account.protocol_type.TradeProtocolTypeDto;
import bisq.dto.common.currency.MarketDto;
import bisq.dto.common.monetary.CoinDto;
import bisq.dto.common.monetary.FiatDto;
import bisq.dto.common.monetary.MonetaryDto;
import bisq.dto.common.monetary.PriceQuoteDto;
import bisq.dto.common.network.AddressByTransportTypeMapDto;
import bisq.dto.common.network.AddressDto;
import bisq.dto.common.network.TransportTypeDto;
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
import bisq.dto.security.keys.KeyPairDto;
import bisq.dto.security.keys.PrivateKeyDto;
import bisq.dto.security.keys.PubKeyDto;
import bisq.dto.security.keys.PublicKeyDto;
import bisq.dto.security.pow.ProofOfWorkDto;
import bisq.dto.settings.SettingsDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.dto.user.reputation.ReputationScoreDto;
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
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
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
        public static TradeProtocolType toPojo(TradeProtocolTypeDto dto) {
            return switch (dto) {
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

        public static TradeProtocolTypeDto from(TradeProtocolType value) {
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


    // common.currency

    public static class MarketMapping {
        public static Market toPojo(MarketDto dto) {
            return new Market(dto.baseCurrencyCode(), dto.quoteCurrencyCode(), dto.baseCurrencyName(), dto.quoteCurrencyName());
        }

        public static MarketDto from(Market value) {
            return new MarketDto(value.getBaseCurrencyCode(), value.getQuoteCurrencyCode(), value.getBaseCurrencyName(), value.getQuoteCurrencyName());
        }
    }


    // common.monetary

    public static class CoinMapping {
        public static Coin toPojo(CoinDto dto) {
            return new Coin(dto.getId(), dto.getValue(), dto.getCode(), dto.getPrecision(), dto.getLowPrecision());
        }

        public static CoinDto from(Coin value) {
            return new CoinDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }
    }

    public static class FiatMapping {
        public static Fiat toPojo(FiatDto dto) {
            return new Fiat(dto.getId(), dto.getValue(), dto.getCode(), dto.getPrecision(), dto.getLowPrecision());
        }

        public static FiatDto from(Fiat value) {
            return new FiatDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
        }
    }


    public static class MonetaryMapping {
        public static Monetary toPojo(MonetaryDto dto) {
            if (dto instanceof FiatDto) {
                return FiatMapping.toPojo((FiatDto) dto);
            } else {
                return CoinMapping.toPojo((CoinDto) dto);
            }
        }

        public static MonetaryDto from(Monetary value) {
            if (value instanceof Fiat) {
                return new FiatDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
            } else {
                return new CoinDto(value.getId(), value.getValue(), value.getCode(), value.getPrecision(), value.getLowPrecision());
            }
        }
    }

    public static class PriceQuoteMapping {
        public static PriceQuote toPojo(PriceQuoteDto dto) {
            String baseCurrencyCode = dto.market().baseCurrencyCode();
            String quoteCurrencyCode = dto.market().quoteCurrencyCode();
            if (baseCurrencyCode.equals("BTC")) {
                Monetary baseSideMonetary = Coin.asBtcFromFaceValue(1);
                Monetary quoteSideMonetary = Fiat.from(dto.value(), quoteCurrencyCode);
                return new PriceQuote(dto.value(), baseSideMonetary, quoteSideMonetary);
            } else {
                throw new UnsupportedOperationException("Altcoin price quote mapping is not supported yet");
            }
        }

        public static PriceQuoteDto from(PriceQuote value) {
            return new PriceQuoteDto(value.getValue(), MarketMapping.from(value.getMarket()));
        }
    }


    // common.network

    public static class AddressByTransportTypeMapMapping {
        public static AddressByTransportTypeMap toPojo(AddressByTransportTypeMapDto dto) {
            return new AddressByTransportTypeMap(dto.map().entrySet().stream().collect(Collectors.toMap(entry -> TransportTypeMapping.toPojo(entry.getKey()), entry -> AddressMapping.toPojo(entry.getValue()))));
        }

        public static AddressByTransportTypeMapDto from(AddressByTransportTypeMap map) {
            return new AddressByTransportTypeMapDto(map.getMap().entrySet().stream().collect(Collectors.toMap(entry -> TransportTypeMapping.from(entry.getKey()), entry -> AddressMapping.from(entry.getValue()))));
        }
    }

    public static class AddressMapping {
        public static Address toPojo(AddressDto dto) {
            return new Address(dto.host(), dto.port());
        }

        public static AddressDto from(Address value) {
            return new AddressDto(value.getHost(), value.getPort());
        }
    }

    public static class TransportTypeMapping {
        public static TransportType toPojo(TransportTypeDto dto) {
            if (dto == TransportTypeDto.CLEAR) {
                return TransportType.CLEAR;
            } else if (dto == TransportTypeDto.TOR) {
                return TransportType.TOR;
            } else if (dto == TransportTypeDto.I2P) {
                return TransportType.I2P;
            } else {
                throw new IllegalArgumentException("Unsupported enum " + dto);
            }
        }

        public static TransportTypeDto from(TransportType value) {
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


    // network.identity

    public static class NetworkIdMapping {
        public static NetworkId toPojo(NetworkIdDto dto) {
            return new NetworkId(AddressByTransportTypeMapMapping.toPojo(dto.addressByTransportTypeMap()), PubKeyMapping.toPojo(dto.pubKey()));
        }

        public static NetworkIdDto from(NetworkId value) {
            return new NetworkIdDto(AddressByTransportTypeMapMapping.from(value.getAddressByTransportTypeMap()), PubKeyMapping.from(value.getPubKey()));
        }
    }


    // offer

    public static class DirectionMapping {
        public static Direction toPojo(DirectionDto dto) {
            if (dto == DirectionDto.BUY) {
                return Direction.BUY;
            } else {
                return Direction.SELL;
            }
        }

        public static DirectionDto from(Direction value) {
            if (value == Direction.BUY) {
                return DirectionDto.BUY;
            } else {
                return DirectionDto.SELL;
            }
        }
    }


    // offer.amount.spec

    public static class AmountSpecMapping {
        public static AmountSpec toPojo(AmountSpecDto dto) {
            if (dto instanceof RangeAmountSpecDto) {
                return RangeAmountSpecMapping.toPojo((RangeAmountSpecDto) dto);
            } else {
                return FixedAmountSpecMapping.toPojo((FixedAmountSpecDto) dto);
            }
        }

        public static AmountSpecDto from(AmountSpec value) {
            if (value instanceof RangeAmountSpec) {
                return RangeAmountSpecMapping.from((RangeAmountSpec) value);
            } else {
                return FixedAmountSpecMapping.from((FixedAmountSpec) value);
            }
        }
    }

    public static class BaseSideFixedAmountSpecMapping {
        public static BaseSideFixedAmountSpec toPojo(BaseSideFixedAmountSpecDto dto) {
            return new BaseSideFixedAmountSpec(dto.getAmount());
        }

        public static BaseSideFixedAmountSpecDto from(BaseSideFixedAmountSpec value) {
            return new BaseSideFixedAmountSpecDto(value.getAmount());
        }
    }

    public static class BaseSideRangeAmountSpecMapping {
        public static BaseSideRangeAmountSpec toPojo(BaseSideRangeAmountSpecDto dto) {
            return new BaseSideRangeAmountSpec(dto.getMinAmount(), dto.getMaxAmount());
        }

        public static BaseSideRangeAmountSpecDto from(BaseSideRangeAmountSpec value) {
            return new BaseSideRangeAmountSpecDto(value.getMinAmount(), value.getMaxAmount());
        }
    }

    public static class FixedAmountSpecMapping {
        public static FixedAmountSpec toPojo(FixedAmountSpecDto dto) {
            if (dto instanceof BaseSideFixedAmountSpecDto) {
                return BaseSideFixedAmountSpecMapping.toPojo((BaseSideFixedAmountSpecDto) dto);
            } else if (dto instanceof QuoteSideFixedAmountSpecDto) {
                return QuoteSideFixedAmountSpecMapping.toPojo((QuoteSideFixedAmountSpecDto) dto);
            } else {
                throw new IllegalArgumentException("Unsupported FixedAmountSpecDto " + dto);
            }
        }

        public static FixedAmountSpecDto from(FixedAmountSpec value) {
            if (value instanceof BaseSideFixedAmountSpec) {
                return BaseSideFixedAmountSpecMapping.from((BaseSideFixedAmountSpec) value);
            } else if (value instanceof QuoteSideFixedAmountSpec) {
                return QuoteSideFixedAmountSpecMapping.from((QuoteSideFixedAmountSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported FixedAmountSpec " + value);
            }
        }
    }

    public static class QuoteSideFixedAmountSpecMapping {
        public static QuoteSideFixedAmountSpec toPojo(QuoteSideFixedAmountSpecDto dto) {
            return new QuoteSideFixedAmountSpec(dto.getAmount());
        }

        public static QuoteSideFixedAmountSpecDto from(QuoteSideFixedAmountSpec value) {
            return new QuoteSideFixedAmountSpecDto(value.getAmount());
        }
    }

    public static class QuoteSideRangeAmountSpecMapping {
        public static QuoteSideRangeAmountSpec toPojo(QuoteSideRangeAmountSpecDto dto) {
            return new QuoteSideRangeAmountSpec(dto.getMinAmount(), dto.getMaxAmount());
        }

        public static QuoteSideRangeAmountSpecDto from(QuoteSideRangeAmountSpec value) {
            return new QuoteSideRangeAmountSpecDto(value.getMinAmount(), value.getMaxAmount());
        }
    }

    public static class RangeAmountSpecMapping {
        public static RangeAmountSpec toPojo(RangeAmountSpecDto dto) {
            if (dto instanceof BaseSideRangeAmountSpecDto) {
                return BaseSideRangeAmountSpecMapping.toPojo((BaseSideRangeAmountSpecDto) dto);
            } else if (dto instanceof QuoteSideRangeAmountSpecDto) {
                return QuoteSideRangeAmountSpecMapping.toPojo((QuoteSideRangeAmountSpecDto) dto);
            } else {
                throw new IllegalArgumentException("Unsupported RangeAmountSpecDto " + dto);
            }
        }

        public static RangeAmountSpecDto from(RangeAmountSpec value) {
            if (value instanceof BaseSideRangeAmountSpec) {
                return BaseSideRangeAmountSpecMapping.from((BaseSideRangeAmountSpec) value);
            } else if (value instanceof QuoteSideRangeAmountSpec) {
                return QuoteSideRangeAmountSpecMapping.from((QuoteSideRangeAmountSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported RangeAmountSpec " + value);
            }
        }
    }


    // offer.bisq_easy

    public static class BisqEasyOfferMapping {
        public static BisqEasyOffer toPojo(BisqEasyOfferDto dto) {
            return new BisqEasyOffer(dto.id(), dto.date(), NetworkIdMapping.toPojo(dto.makerNetworkId()), DirectionMapping.toPojo(dto.direction()), MarketMapping.toPojo(dto.market()), AmountSpecMapping.toPojo(dto.amountSpec()), PriceSpecMapping.toPojo(dto.priceSpec()), dto.protocolTypes().stream().map(TradeProtocolTypeMapping::toPojo).collect(Collectors.toList()), dto.baseSidePaymentMethodSpecs().stream().map(BitcoinPaymentMethodSpecMapping::toPojo).collect(Collectors.toList()), dto.quoteSidePaymentMethodSpecs().stream().map(FiatPaymentMethodSpecMapping::toPojo).collect(Collectors.toList()), dto.offerOptions().stream().map(OfferOptionMapping::toPojo).collect(Collectors.toList()), dto.supportedLanguageCodes());
        }

        public static BisqEasyOfferDto from(BisqEasyOffer value) {
            return new BisqEasyOfferDto(value.getId(), value.getDate(), NetworkIdMapping.from(value.getMakerNetworkId()), DirectionMapping.from(value.getDirection()), MarketMapping.from(value.getMarket()), AmountSpecMapping.from(value.getAmountSpec()), PriceSpecMapping.from(value.getPriceSpec()), value.getProtocolTypes().stream().map(TradeProtocolTypeMapping::from).collect(Collectors.toList()), value.getBaseSidePaymentMethodSpecs().stream().map(BitcoinPaymentMethodSpecMapping::from).collect(Collectors.toList()), value.getQuoteSidePaymentMethodSpecs().stream().map(FiatPaymentMethodSpecMapping::from).collect(Collectors.toList()), value.getOfferOptions().stream().map(OfferOptionMapping::from).collect(Collectors.toList()), value.getSupportedLanguageCodes());
        }
    }


    // offer.options

    public static class OfferOptionMapping {
        public static OfferOption toPojo(OfferOptionDto dto) {
            if (dto instanceof ReputationOptionDto) {
                return ReputationOptionMapping.toPojo((ReputationOptionDto) dto);
            } else if (dto instanceof TradeTermsOptionDto) {
                return TradeTermsOptionMapping.toPojo((TradeTermsOptionDto) dto);
            } else {
                throw new IllegalArgumentException("Unsupported OfferOptionDto " + dto);
            }
        }

        public static OfferOptionDto from(OfferOption value) {
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
        public static ReputationOption toPojo(ReputationOptionDto dto) {
            //noinspection deprecation
            return new ReputationOption(dto.getRequiredTotalReputationScore());
        }

        public static ReputationOptionDto from(ReputationOption value) {
            //noinspection deprecation
            return new ReputationOptionDto(value.getRequiredTotalReputationScore());
        }
    }

    public static class TradeTermsOptionMapping {
        public static TradeTermsOption toPojo(TradeTermsOptionDto dto) {
            return new TradeTermsOption(dto.getMakersTradeTerms());
        }

        public static TradeTermsOptionDto from(TradeTermsOption value) {
            return new TradeTermsOptionDto(value.getMakersTradeTerms());
        }
    }


    // offer.payment_method

    public static class BitcoinPaymentMethodSpecMapping {
        public static BitcoinPaymentMethodSpec toPojo(BitcoinPaymentMethodSpecDto dto) {
            String paymentMethod = dto.getPaymentMethod();
            BitcoinPaymentMethod method = PaymentMethodSpecUtil.getBitcoinPaymentMethod(paymentMethod);
            return new BitcoinPaymentMethodSpec(method, dto.getSaltedMakerAccountId());
        }

        public static BitcoinPaymentMethodSpecDto from(BitcoinPaymentMethodSpec value) {
            return new BitcoinPaymentMethodSpecDto(value.getPaymentMethod().getName(), value.getSaltedMakerAccountId());
        }
    }

    public static class FiatPaymentMethodSpecMapping {
        public static FiatPaymentMethodSpec toPojo(FiatPaymentMethodSpecDto dto) {
            String paymentMethod = dto.getPaymentMethod();
            FiatPaymentMethod method = PaymentMethodSpecUtil.getFiatPaymentMethod(paymentMethod);
            return new FiatPaymentMethodSpec(method, dto.getSaltedMakerAccountId());
        }

        public static FiatPaymentMethodSpecDto from(FiatPaymentMethodSpec value) {
            return new FiatPaymentMethodSpecDto(value.getPaymentMethod().getName(), value.getSaltedMakerAccountId());
        }
    }

    public static class PaymentMethodSpecMapping {
        public static PaymentMethodSpec<?> toPojo(PaymentMethodSpecDto dto) {
            if (dto instanceof FiatPaymentMethodSpecDto) {
                return FiatPaymentMethodSpecMapping.toPojo((FiatPaymentMethodSpecDto) dto);
            } else if (dto instanceof BitcoinPaymentMethodSpecDto) {
                return BitcoinPaymentMethodSpecMapping.toPojo((BitcoinPaymentMethodSpecDto) dto);
            } else {
                throw new IllegalArgumentException("Unsupported PaymentMethodSpecDto " + dto);
            }
        }

        public static PaymentMethodSpecDto from(PaymentMethodSpec<?> value) {
            if (value instanceof FiatPaymentMethodSpec) {
                return FiatPaymentMethodSpecMapping.from((FiatPaymentMethodSpec) value);
            } else if (value instanceof BitcoinPaymentMethodSpec) {
                return BitcoinPaymentMethodSpecMapping.from((BitcoinPaymentMethodSpec) value);
            } else {
                throw new IllegalArgumentException("Unsupported PaymentMethodSpec " + value);
            }
        }
    }


    // offer.price.spec

    public static class MarketPriceSpecMapping {
        public static MarketPriceSpec toPojo(MarketPriceSpecDto dto) {
            return new MarketPriceSpec();
        }

        public static MarketPriceSpecDto from(MarketPriceSpec value) {
            return new MarketPriceSpecDto();
        }
    }

    public static class FloatPriceSpecMapping {
        public static FloatPriceSpec toPojo(FloatPriceSpecDto dto) {
            return new FloatPriceSpec(dto.getPercentage());
        }

        public static FloatPriceSpecDto from(FloatPriceSpec value) {
            return new FloatPriceSpecDto(value.getPercentage());
        }
    }

    public static class FixPriceSpecMapping {
        public static FixPriceSpec toPojo(FixPriceSpecDto dto) {
            return new FixPriceSpec(PriceQuoteMapping.toPojo(dto.getPriceQuote()));
        }

        public static FixPriceSpecDto from(FixPriceSpec value) {
            return new FixPriceSpecDto(PriceQuoteMapping.from(value.getPriceQuote()));
        }
    }

    public static class PriceSpecMapping {
        public static PriceSpec toPojo(PriceSpecDto dto) {
            return switch (dto) {
                case MarketPriceSpecDto marketPriceSpecDto -> MarketPriceSpecMapping.toPojo(marketPriceSpecDto);
                case FixPriceSpecDto fixPriceSpecDto -> FixPriceSpecMapping.toPojo(fixPriceSpecDto);
                case FloatPriceSpecDto floatPriceSpecDto -> FloatPriceSpecMapping.toPojo(floatPriceSpecDto);
                case null, default -> throw new IllegalArgumentException("Unsupported PriceSpecDto " + dto);
            };
        }

        public static PriceSpecDto from(PriceSpec value) {
            return switch (value) {
                case MarketPriceSpec marketPriceSpec -> MarketPriceSpecMapping.from(marketPriceSpec);
                case FixPriceSpec fixPriceSpec -> FixPriceSpecMapping.from(fixPriceSpec);
                case FloatPriceSpec floatPriceSpec -> FloatPriceSpecMapping.from(floatPriceSpec);
                case null, default -> throw new IllegalArgumentException("Unsupported PriceSpec " + value);
            };
        }
    }


    // security.keys

    public static class KeyPairDtoMapping {
        public static KeyPair toPojo(KeyPairDto dto) {
            PublicKey publicKey = PublicKeyMapping.toPojo(dto.publicKey());
            PrivateKey privateKey = PrivateKeyMapping.toPojo(dto.privateKey());
            return new KeyPair(publicKey, privateKey);
        }

        public static KeyPairDto from(KeyPair value) {
            PrivateKeyDto privateKeyDto = PrivateKeyMapping.from(value.getPrivate());
            PublicKeyDto publicKeyDto = PublicKeyMapping.from(value.getPublic());
            return new KeyPairDto(publicKeyDto, privateKeyDto);
        }
    }

    public static class PrivateKeyMapping {
        public static PrivateKey toPojo(PrivateKeyDto dto) {
            try {
                byte[] decoded = Base64.getDecoder().decode(dto.encoded());
                return KeyGeneration.generatePrivate(decoded);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate privateKey", e);
            }
        }

        public static PrivateKeyDto from(PrivateKey value) {
            return new PrivateKeyDto(Base64.getEncoder().encodeToString(value.getEncoded()));
        }
    }

    public static class PubKeyMapping {
        public static PubKey toPojo(PubKeyDto dto) {
            return new PubKey(PublicKeyMapping.toPojo(dto.publicKey()), dto.keyId());
        }

        public static PubKeyDto from(PubKey value) {
            PublicKey publicKey = value.getPublicKey();
            PublicKeyDto publicKeyDto = PublicKeyMapping.from(publicKey);
            String keyId = value.getKeyId();
            byte[] hashAsBytes = DigestUtil.hash(publicKey.getEncoded());
            String hash = Base64.getEncoder().encodeToString(hashAsBytes);
            String id = Hex.encode(hashAsBytes);
            return new PubKeyDto(publicKeyDto, keyId, hash, id);
        }
    }

    public static class PublicKeyMapping {
        public static PublicKey toPojo(PublicKeyDto dto) {
            try {
                byte[] decoded = Base64.getDecoder().decode(dto.encoded());
                return KeyGeneration.generatePublic(decoded);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate publicKey", e);
            }
        }

        public static PublicKeyDto from(PublicKey value) {
            return new PublicKeyDto(Base64.getEncoder().encodeToString(value.getEncoded()));
        }
    }


    // security.pow

    public static class ProofOfWorkDtoMapping {
        public static ProofOfWork toPojo(ProofOfWorkDto dto) {
            return new ProofOfWork(
                    Base64.getDecoder().decode(dto.payload()),
                    dto.counter(),
                    dto.challenge() != null ? Base64.getDecoder().decode(dto.challenge()) : null,
                    dto.difficulty(),
                    Base64.getDecoder().decode(dto.solution()),
                    dto.duration()
            );
        }

        public static ProofOfWorkDto from(ProofOfWork value) {
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
    public static class SettingsDtoMapping {
        // toPojo method not implemented as we do not have a settings value object in the domain

        public static SettingsDto from(SettingsService settingsService) {
            return new SettingsDto(settingsService.getIsTacAccepted().get(),
                    settingsService.getTradeRulesConfirmed().get(),
                    settingsService.getCloseMyOfferWhenTaken().get(),
                    settingsService.getLanguageCode().get(),
                    settingsService.getSupportedLanguageCodes(),
                    settingsService.getMaxTradePriceDeviation().get(),
                    MarketMapping.from(settingsService.getSelectedMarket().get()));
        }
    }


    // user.profile

    public static class UserProfileDtoMapping {
        public static UserProfile toPojo(UserProfileDto dto) {
            return new UserProfile(dto.version(),
                    dto.nickName(),
                    ProofOfWorkDtoMapping.toPojo(dto.proofOfWork()),
                    dto.avatarVersion(),
                    NetworkIdMapping.toPojo(dto.networkId()),
                    dto.terms(),
                    dto.statement(),
                    dto.applicationVersion()
            );
        }

        public static UserProfileDto from(UserProfile value) {
            return new UserProfileDto(
                    value.getVersion(),
                    value.getNickName(),
                    ProofOfWorkDtoMapping.from(value.getProofOfWork()),
                    value.getAvatarVersion(),
                    NetworkIdMapping.from(value.getNetworkId()),
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
        public static ReputationScore toPojo(ReputationScoreDto dto) {
            return new ReputationScore(dto.totalScore(), dto.fiveSystemScore(), dto.ranking());
        }

        public static ReputationScoreDto from(ReputationScore value) {
            return new ReputationScoreDto(value.getTotalScore(), value.getFiveSystemScore(), value.getRanking());
        }
    }
}