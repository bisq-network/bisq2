package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAddressInfoResponse {
    private String address;
    private String scriptPubKey;
    private boolean ismine;
    private boolean iswatchonly;
    private boolean solvable;
    private String desc;
    private boolean isscript;
    private boolean ischange;
    private boolean iswitness;
    private int witness_version;
    private String witness_program;
    private String script;
    private String hex;
    private String[] pubkeys;
    private int sigsrequired;
    private String pubkey;
    private Object embedded;
    private boolean iscompressed;
    private int timestamp;
    private String hdkeypath;
    private String hdseedid;
    private String hdmasterfingerprint;
    private String[] labels;
}
