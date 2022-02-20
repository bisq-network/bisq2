#
# INTRODUCTION
#
# This makefile is designed to help Bisq contributors test the behavior of seeds and clients
# under different network setups.
#
#
# REQUIREMENTS
#
# `make`, `screen`
#
#
# USAGE
#
# The tests will launch different seeds and clients using the gradle wrapper. Each
# instance's console output will be tracked in a separate `screen` tab. F6/F7 navigates
# to the previous/next `screen` tab. Ctrl+C kills the gradle process running in the current
# `screen` tab and closes the tab.
#
#
# SCENARIOS
#
# 1) Local clearnet scenario
#
#    Launch 2 clearnet seeds and several (n) clearnet desktop clients with:
#
#     $ make start-local-clearnet n=4
#
#    When not specified, n will default to 2, so this will start 2 seeds and 2 clients:
#
#     $ make start-local-clearnet
#
# 2) Local tor scenario
#
#    Launches 2 tor seeds and several (n) tor desktop clients. This needs 2 steps:
#
#    First step: have the tor seeds generate their tor addresses with:
#
#     $ make start-tor-seeds
#
#    That will start both tor seeds. Let them both run until you see a log statement like
#
#     [NetworkService.network-IO-pool] b.n.p.n.t.TorTransport: Tor initialized after ...ms
#
#    That line indicates the seed has its tor identity. Once both seed outputs have that line,
#    stop both seeds with Ctrl+C.
#
#    Second step: start a combination of 2 tor seeds and several (n) tor desktop clients with:
#
#     $ make start-tor-full-env n=4
#
#    As in the previous scenario, n is optional. If not specified, it will default to 2.
#
#    Notes: this scenario will create 2 new files in the current directory. They are created
#    in the first step and are needed in the second step. They can be cleaned up with:
#
#     $ make clean
#

# n = number of clients
# Set n to 2, if otherwise not given as arg
n ?= 2

.start-local-clearnet-seeds:
	# First screen command uses custom config, creates the session, and is detached
	# All subsequent screen calls will create a new tab in the same session
	# Seed 1
	screen -c .screenrc-make -dmS localtests -t seed-1 ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=bisq2_seed1_test \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
	# Seed 2
	screen -S localtests -X screen -t seed-2 ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=bisq2_seed2_test \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001

.start-clearnet-clients:
	for i in $$(seq 1 $n); do \
		screen -S localtests -X screen -t client-$$i ./gradlew --console=plain desktop:run \
			-Dbisq.application.appName=bisq_client$${i}_test \
			-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001; \
	done

# Reattach to the screen session
.reconnect-screen-session:
	screen -r localtests


start-local-clearnet: .start-local-clearnet-seeds .start-clearnet-clients .reconnect-screen-session

# Copy seed 1 hostname to a local file, which is read when starting the clients
seed1-tor-hostname:
	@echo "Reading seed 1 tor hostname"
	cp ~/.local/share/bisq2_seed1_tor_test/tor/hiddenservice/default/hostname seed1-tor-hostname

# Copy seed 2 hostname to a local file, which is read when starting the clients
seed2-tor-hostname:
	@echo "Reading seed 2 tor hostname"
	cp ~/.local/share/bisq2_seed2_tor_test/tor/hiddenservice/default/hostname seed2-tor-hostname

# Requires both seed hostnames to be known
# If any is not known, this will fail
.start-tor-clients: seed1-tor-hostname seed2-tor-hostname
	for i in $$(seq 1 $n); do \
		screen -S localtests -X screen -t client-$${i}-tor ./gradlew --console=plain desktop:run \
			-Dbisq.application.appName=bisq_client$${i}_tor_test \
			-Dbisq.networkServiceConfig.supportedTransportTypes.0=TOR \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.0=$(file < seed1-tor-hostname):1000 \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.1=$(file < seed2-tor-hostname):1001; \
	done

.start-tor-seeds:
	# First screen command uses custom config, creates the session, and is detached
	# All subsequent screen calls will create a new tab in the same session
	# Seed 1
	screen -c .screenrc-make -dmS localtests -t seed-1 ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=bisq2_seed1_tor_test \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1000 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=TOR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
	# Seed 2
	screen -S localtests -X screen -t seed-2 ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=bisq2_seed2_tor_test \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1001 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=TOR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001


start-tor-seeds: .start-tor-seeds .reconnect-screen-session

start-tor-full-env: .start-tor-seeds .start-tor-clients .reconnect-screen-session

clean:
	rm seed1-tor-hostname seed2-tor-hostname