FROM python:3

# Install Bitcoin Core
WORKDIR /tmp
RUN wget https://bitcoincore.org/bin/bitcoin-core-24.0.1/bitcoin-24.0.1-x86_64-linux-gnu.tar.gz && \
    tar xvf bitcoin-24.0.1-x86_64-linux-gnu.tar.gz && \
    mv bitcoin-24.0.1/bin/* /usr/local/bin/ && \
    rm -rf /tmp/bitcoin*

# Install ElectrumX
WORKDIR /root
RUN git clone https://github.com/spesmilo/electrumx.git && \
    cd electrumx && \
    pip3 install . && \
    mkdir db_directory

# Start Bitcoin Core Daemon and ElectrumX
WORKDIR /root/electrumx
CMD bitcoind -regtest -daemon -server -debug=1 \
    -rpcbind -rpcallowip=127.0.0.1 -rpcuser=bisq -rpcpassword=bisq \
    -zmqpubhashblock=tcp://:7771 -zmqpubrawtx=tcp://:7771 \
    -fallbackfee=0.00000001 -whitelist=127.0.0.1 -txindex=1 && \
    ALLOW_ROOT=yes SERVICES=tcp://:50001,rpc:// COIN=Bitcoin NET=regtest \
    DAEMON_URL=http://bisq:bisq@localhost:18443 \
    DB_DIRECTORY=/root/electrumx/db_directory ./electrumx_server
