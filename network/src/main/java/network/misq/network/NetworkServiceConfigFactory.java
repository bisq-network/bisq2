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

package network.misq.network;

import com.typesafe.config.Config;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
public class NetworkServiceConfigFactory {
    private final NetworkService.Config networkServiceConfig;

    public NetworkService.Config get() {
        return networkServiceConfig;
    }

    public NetworkServiceConfigFactory(String baseDir) {
        this(baseDir, Optional.empty(), Optional.empty());
    }

    public NetworkServiceConfigFactory(String baseDir, Config config, String[] args) {
        this(baseDir, Optional.of(config), Optional.of(args));
    }

    public NetworkServiceConfigFactory(String baseDir, Optional<Config> config, Optional<String[]> args) {
        //todo apply networkConfig
        //todo use option parser lib for args (define priority)

       // Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR, Transport.Type.I2P);
        Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR);

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        Map<Transport.Type, List<Address>> seedAddressesByTransport = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P),
                Transport.Type.CLEAR, getSeedAddresses(Transport.Type.CLEAR)
        );

        Transport.Config transportConfig = new Transport.Config(baseDir);

        PeerGroup.Config peerGroupConfig = new PeerGroup.Config(8, 12, 1);
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config(2, 10, 10);
        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(TimeUnit.SECONDS.toMillis(180), TimeUnit.SECONDS.toMillis(90));
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(10),  // bootstrapTime
                TimeUnit.SECONDS.toMillis(20),  // interval
                TimeUnit.SECONDS.toMillis(60),  // timeout
                TimeUnit.HOURS.toMillis(2),     // maxAge
                100,                        // maxReported
                100,                        // maxPersisted
                4                             // maxSeeds
        );
        PeerGroupService.Config clearNetConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(5),  // bootstrapTime
                TimeUnit.SECONDS.toMillis(10),  // interval
                TimeUnit.SECONDS.toMillis(60),  // timeout
                TimeUnit.HOURS.toMillis(2),     // maxAge
                100,                        // maxReported
                100,                        // maxPersisted
                4                             // maxSeeds
        );
        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );

        networkServiceConfig = new NetworkService.Config(baseDir,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedAddressesByTransport,
                Optional.empty());
    }

    // todo move to a conf file
    public List<Address> getSeedAddresses(Transport.Type transportType) {
        switch (transportType) {
            case TOR -> {
                return Stream.of(
                                new Address("76ewqvsvh5nnuqnlro65nrxu3d4377aw5kv25p2uq7cpvoi4xslq7vyd.onion", 1000),
                                new Address("ucq3qw4qlzstpwtqig6lxll64tarmqi77u6t5iquvi52j66pqrsqcpad.onion", 1001),
                                new Address("fanvmqmbxyklaro3uanyotybrfcflc2ywlr5qd3ttvf2cva2huxrwuyd.onion", 1002),
                                new Address("4i45sndzsxnw24idfr57lzzbcxhfdjp3yikvxwb54p4bjay5j4lqjuqd.onion", 1003),
                                new Address("5wzt2sx53vzozoifkvntrl7rycegmker57aofb6qxgbunbw2p435fnyd.onion", 1004),
                                new Address("5ekgw2qowowt7ncgo2jnnwyehqfvj7gqj75cogt3apuyef32myj7byyd.onion", 1005),
                                new Address("cqvqm4ewvh2zdlrcz4whn6b5ltgg5hbef4ymp2pzk2hlrz4hjkqpmgyd.onion", 1006),
                                new Address("kctohowqiwkgeceag622c56kv5aqbeozm5ifjjldqo2x5hnzb5qkhfqd.onion", 1007),
                                new Address("hqvn6w7phvz6jifm2ak66flrdeieq24f653njkf4e7tsrqbramm3hpyd.onion", 1008),
                                new Address("onuyzrnp4zgnokmptj4krgwfydqh5snzoekhoo4hr3eo4qg7jhhziaad.onion", 1009),
                                new Address("zj6eiccp4irphbs4ihbgwcy3jo6unxap5nhac4r25mswrscuqqs5beqd.onion", 1010),
                                new Address("plj7e5vs67psw2jlryjrbuh4336zwrigcsiiwf4jeennkla5x2ly5qyd.onion", 1011)
                        )
                        .collect(Collectors.toList());
            }
            case I2P -> {
                return Stream.of(
                                new Address("KvQVpgFzxw7jvwdxLAjywlc9Y4hLPT49on2XPYzcRoHmQa-UAkydPKZfRiY40Dh5DHjr6~jeOuLqGXk-~qz3afeiOjEBp2Ev~qdT8Xg55jPLs8ObG-20Fsa1M3PpVF2gCeDPe~lY8UMgxpJuH~yVMz13rtyqM9wIlpCck3YFtIhe97Pm1fkb2~88z6Oo3tZGWixI0-MEPGWh8hRwdVju5Un6NXpterWdTLWkM7A3kHPh0qCJn9WaoH~wX5oiIL7JtP0Sn8F852JdmJJHxupgosLJ1L63uvbvb0pT3RtOoG~drdfbATv~jqQGc2GaEV2v8xbEYhp7usXAukJeQTLiWFxFCHlRlIjmhM-u10J8cKrqAp2OXrDwLzyX7phDEm58N21rQXdvQ8MiSfm4VPlgYxie6oo5Fu8RTAkK-8SKRUA0wx7QiJUVPLm4h1-6lIHUbethGfDpCsW-z2M3qwLKbn~DAkvyxitNylCTR-UNZ4rbuDSH38nLRbDYug2gVRjiBQAEAAcAAA==", 5000),
                                new Address("m~1WfaZNz1x9HCOFdotg~G9m~YSMowWvE3jeqAmc-xsFNJZKNPcOub4yWc4uhoSu6yL0WuRIH7B4skPvlDe1BtEnPVJXyTGQX3wepcL3aekY0Gc3kB5gcMy48pUHNcxdznPNDNFVCqrmOpthGDksukJIlYxfh-M~S~3K-2gxYrDiJsT16o59E3bOEwArVpLg~C4NtaU6~KyUFvPfcD9SKA8PrQ4nu7OjyCrzhnO0BNhNv2t1c~5gLlu3gsRviWBl6hxppystHuDDCE~6ERufsvr0DFSrRetxkY0eHqL9l8--YbDgceTPtoWiEfmgpfLrznHnaWdn9J~CMQ~0dIbi7hPhGh8z5rBp5h2RRBzumNF5~A60Fr4WSIsCbSGeaQo0SZJsGpysJdmws5ExcxQaqTiCDUuef0zbl2Su3THlipNOTkZaA6wQv-TbJjfaJPnVhnpIBsnyK8Dd8GzG3P6eYvrA2QFN2XzxS4rQ~KK5oNqQr4MHRJBBFUM1QmGLU6wmBQAEAAcAAA==", 5001),
                                new Address("fZDdLw9o1eWz69BQSLTYD7TvYnDA9QLFyEykh7OJR7d4qs88H5xFJ11IRQzup56NZVeZEqh-fgaG-JT0LKHgUx3G5qlYM5N1QlidKWDzpmuLwqnXg25n5V3wK0EpomYslsBej1~tYReK5qw6ASw5N~1qVZDKhYktKrCoD7SLmnPejFmOA~Gx2YWoV4eU35Y0PYHq~ysWwEs9BPuuhxbN9PUA2IFsXu5NmXsyuvmHABjMOyj9h5uMN0F0Dj~2slxEqadWBCqzcbmE5EkdjAO1vcdnMRh2nTs71J7MEX7w1sXi6lU0xLLEv~JCBLs8qIRT32v2j7a3FvnGhmZju7XAUbx3LNgFUyCqReUFCZnmNIKdmTmNdg~MLlI50hFsErykxlMIjaf5bn3fE~E839J0fJP-OW~3sBg7t8Z68aOQhZEydIOjmiiGI~Tp1JFvT6Dnwkldk1SLstQMNrOKNNfIbK43aNIWehbiU4nXXvVR-0vxNPAFyTPRqNVpTCZIobrdBQAEAAcAAA==", 5002),
                                new Address("lZrqdVrgGByiwTr5NHjO8PUVjTxjxcpJowKyxfXtfj0wBJIYDTQM88fiUkAYCLGuJSZhiavmysEa6n2lYxn3EwM1jq0hSA0mclYOj6fIqO7dupBKHHRppRcr-E4ir07pIV23edi1wnW0oOhXGNjS7K5RXaQngu0KOkCgvjSaBPYrUkbPgT5zxTkZMly2FUkntGQ-qEBDktUr72a2TCrg2i6geBqo5E~hSN-SrLFFViri2pz9wS52AkBOscu6DDPas2LWpTwXYBg-qkUZtzM38c3UCYUjAvNHooI68Z7nj6Fo0zs3Em6r63o6ztQfAvn~PJ8LmJboFxGzGhgGk4AL4zbgY8ZF0Fyrr3OqCjOWBwaClFPfTsWhYFzQcFw06QEd0N-MQnkCmwI6gcu6BTz-Z6m7HktjJHJHhpRMkXsXUOVBTBFp~QyfijGvJO9sll91rcJ6cNl09R9jsLSnJ~82rWBQagBdW02TNyS4UgVlpMr4QN35S1zRu5rYSDtOnrS4BQAEAAcAAA==", 5003),
                                new Address("eay7qLrWoMp~C2Kid4aOfzTVaYAJIvT-xtSSHw4EDr6R7-K4I2bcRyt2I4XIZaRta-QKgFC2MBfQ9rdv6~BlbpN4EaSAfaTnqvBNb~00eSDdvdlzKDTgo0OsAlSHYx0KriEZk-qc3LA6T-xFmFCsqADxweHSprNG0UP87O0jZUldJjJc-9~Ls76~C1NEpHWp0W8GJ47TkCkqXNoA7nvNGMfIxBznP0HlnlMgnbWtUDeWcuPLaH8Na5Yh3Y8XMwHtwofkPJeAaX51~74muB2t5U9xkD9oPAKZQSIA1Ykt8XAoFRfRrsqKrqeGwtbrtsGFopcUu4Zry3QCbsS76czKxlW7WpSIbWwcaj~XOkUR3WycfsGuMkE7rijuuYTqwR5508pmYA1wIm6ThY8sjfqTKvpVZUFx5BUYKB6KRXUy9tvbBGVjrG~DFhEWeo~mcAkMu-MpnqrqMB0iG2-dwUKpjhsOar0KSycjtURmYBvLchh6L~4W5imlgemFkXWfumDJBQAEAAcAAA==", 5004),
                                new Address("ZKZfVE-s4Qw~My~xVXKSBMot4ms9~wEWruWX8398muZytxV8Hw~cgq410EoHRbq-HsWuzbFI-Acd3t-ja8JffE2KwEmzEc3GwZxyaMXccUbreUeM33ldEJZIWOm~~szhGlAJ4wdFOrjgdsCHM~cyDzPQujerN640Ghkr4rC~gkLL7P~-vTLFUEVuAzMLe3-fs2NJWG9jhfLeynZ845QTKifWs~0OK4zwu~iGwifOw2H1Ra9k~8ZmTW0UM44WowFE7djPtM2XDKfQGWKM2Z1oa2747rdQNa-MDbZcZJvGSO6DiSP7ia2XKSkn0YyvuGqul2q1~EJZYu9~ih1rd35U20YKC~AurXV8RPRc8yUl8Go4WWH-1n5EHkxXp~o8oKeUM2vACzJW0TZHXM6uDsEYy7s-3pRAuM5xQn2s-5HPA63XH47ki8GieiXfberMxdgYj-RIEWp3DxSu7TnSt1cZIRSmwMhUo0ialyr6oIqt83qtD9hdOxbfRI9t734rp8b3BQAEAAcAAA==", 5005),
                                new Address("7mUI1wGvc0SFXe95sSEBGc~FKeLTZ5Rm5DkP8KgM23uFIVyzafsJHrspXmI6nrO-zocRkRlh96pbo-Klj9vnxzCnlwOrcLtevCSH6Mb1i6dqez2OBZJPAWkwMl5rhlBNKZK-gnjHKzvGWi7p~JQVTD4DBXOREzl1Ja9KTh7DKI-LOr1IfmRvvgBJ~tMRwy~DhpPzrKnb3wQ7JOdbPov76LSrzZ4pJ0k0ZJCAwLLn-gLm0f1bCjZWuqDragzp4k1niNKYsDUS6yX3vAlybW-yqbCHIe-P0xojpru1YHaY0RRiz3F9M8h57lgX1bTIkE4XHZNAvTerC-2DP~Y32ZyVSuc14vaZ31WhqTfrXnl3h9RDZ8OfyjwL7zKQMznnMPbWFLv~KyTAcmI7wjGlmEXBjwk0jjVJhvrm4uEn648aNc3oPv4vryXH9u-~54J~gU3-wgii3zpYtpT9AhK2X0OTEvvnG0QZAL0gTwmyvlKSrKsRPfdl9AWWswmj8F3WxjK-BQAEAAcAAA==", 5006),
                                new Address("gFT7xEiKOYTOLOyJgFntwdGXpcGeq0XloyUXAqqQooqwWQC~UqpaFw5GANm-kjbNdkyymL8DJTw6HU~rXxPqVs6YKV~v80Rtrug2jQuIlPwXWEwRYnDl38bMMH4a6Qqq0--vh7JzhXt~fh4hiWPyiPkPZVN6F8PjLJ0myr54plzEz5ueMxW~bjOWshef8yWrvrXZ5QPMSJWwRhw1AClWZUjdjR-7yRZfT0XW4z7l3EE7ib~ZFzdRxcb~M3JFaudpIT5bowgPmByym-M5ZNZXws6~Nlu4OpflSsboAmXz-gdyMElpUOm8p0n1UyV0n20nTVRiuCVL0e~SSYVjNLSBdWfUSwYHG-TjDfFovAFd1Ns7sHZwOXFKZSWY6bUOuOW4rRwGKaXOvXGdvm881w-wIVgk6bah3fVIi-D6vjiKacFKQD6gxybneUci6KXi59cucBxzp3QPMlQLweAvX6cBKvKzleStJhH18MkJ1Q9MPNq8tl2WF4X-YPSnpkHsYtM6BQAEAAcAAA==", 5007)
                        )
                        .collect(Collectors.toList());
            }
            default -> {
                List<Address> seedAddresses = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    seedAddresses.add(Address.localHost(8000 + i));
                }
                return seedAddresses;
            }
        }
    }
}