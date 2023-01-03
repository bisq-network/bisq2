FROM python:3

# Install ElectrumX
WORKDIR /root
RUN git clone https://github.com/spesmilo/electrumx.git && \
    cd electrumx && \
    pip3 install . && \
    mkdir db_directory

# Start ElectrumX
WORKDIR /root/electrumx
CMD ALLOW_ROOT=yes SERVICES=tcp://:50001,rpc:// COIN=Bitcoin NET=regtest \
    DAEMON_URL=http://bisq:bisq@host.docker.internal:18443 \
    DB_DIRECTORY=/root/electrumx/db_directory ./electrumx_server
