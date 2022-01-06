package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class GetAddressInfoResponse {
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String scriptPubKey;
    @Getter
    @Setter
    private boolean ismine;
    @Getter
    @Setter
    private boolean iswatchonly;
    @Getter
    @Setter
    private boolean solvable;
    @Getter
    @Setter
    private String desc;
    @Getter
    @Setter
    private boolean isscript;
    @Getter
    @Setter
    private boolean ischange;
    @Getter
    @Setter
    private boolean iswitness;
    @Getter
    @Setter
    private int witness_version;
    @Getter
    @Setter
    private String witness_program;
    @Getter
    @Setter
    private String script;
    @Getter
    @Setter
    private String hex;
    @Getter
    @Setter
    private String[] pubkeys;
    @Getter
    @Setter
    private int sigsrequired;
    @Getter
    @Setter
    private String pubkey;
    @Getter
    @Setter
    private Object embedded;
    @Getter
    @Setter
    private boolean iscompressed;
    @Getter
    @Setter
    private int timestamp;
    @Getter
    @Setter
    private String hdkeypath;
    @Getter
    @Setter
    private String hdseedid;
    @Getter
    @Setter
    private String hdmasterfingerprint;
    @Getter
    @Setter
    private String[] labels;
}
