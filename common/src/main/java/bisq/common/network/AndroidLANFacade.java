package bisq.common.network;

import bisq.common.network.clear_net_address_types.LANAddressTypeFacade;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.NetworkInterface;
import java.util.Optional;

@Slf4j
public class AndroidLANFacade extends LANAddressTypeFacade {
    // This class extends LANAddressTypeFacade to provide Android-specific LAN address functionality
    // The parent class already implements all the necessary methods
}