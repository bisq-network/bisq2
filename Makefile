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
# to the previous/next `screen` tab. Ctrl+C or Ctrl+D kills the gradle process running
# in the current `screen` tab and closes the tab.
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
# 3) Local I2P scenario
#
#    Launches 2 I2P seeds and several (n) I2P desktop clients. This needs 2 steps:
#
#    First step: have the I2P seeds generate their private key and destinations with:
#
#     $ make start-i2p-seeds
#
#    That will start both I2P seeds. Let them both run until you both of them as green in the
#    local I2P console at http://127.0.0.1:7657/home . Once two new green entries appear in
#    the I2P console, go to the terminal and stop both seeds with Ctrl+C.
#
#    Second step: start a combination of 2 I2P seeds and several (n) I2P desktop clients with:
#
#     $ make start-i2p-full-env n=4
#
#    As in the previous scenario, n is optional. If not specified, it will default to 2.
#
#    Notes: this scenario will create 2 new files in the current directory. They are created
#    in the first step and are needed in the second step. They can be cleaned up with:
#
#     $ make clean
#

########
# COMMON
########

# n = number of clients
# Set n to 2, if otherwise not given as arg
n ?= 2

# Seed variables. Used for tab title and appName
seed1-prefix = seed-1-
seed2-prefix = seed-2-
seed-postfix = -make-test
seed1-title   = $(seed1-prefix)$(seed-type)
seed2-title   = $(seed2-prefix)$(seed-type)
seed1-appName = $(seed1-prefix)$(seed-type)$(seed-postfix)
seed2-appName = $(seed2-prefix)$(seed-type)$(seed-postfix)

# Create the session in the background
# Future commands will add new tabs to it, each running their own gradle instance (seed or client)
# Finally, to "view" the session and all its tabs, the last command has to be to re-attach
.init-screen-session:
	screen -c .screenrc-make -dmS localtests

# Reattach to the screen session
.reconnect-screen-session:
	screen -r localtests

clean:
	@rm -fv seed1-tor-hostname seed2-tor-hostname seed1-i2p-destination seed2-i2p-destination


##########
# CLEARNET
##########

.start-local-clearnet-seeds: seed-type=clear
.start-local-clearnet-seeds:
	# Seed 1
	screen -S localtests -X screen -t ${seed1-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed1-appName} \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
	# Seed 2
	screen -S localtests -X screen -t ${seed2-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed2-appName} \
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

start-local-clearnet: .init-screen-session .start-local-clearnet-seeds .start-clearnet-clients .reconnect-screen-session


#####
# TOR
#####

# Copy seed 1 hostname to a local file, which is read when starting the clients
seed1-tor-hostname: seed-type=tor
seed1-tor-hostname:
	@cp -v ~/.local/share/${seed1-appName}/tor/hiddenservice/default/hostname seed1-tor-hostname

# Copy seed 2 hostname to a local file, which is read when starting the clients
seed2-tor-hostname: seed-type=tor
seed2-tor-hostname:
	@cp -v ~/.local/share/${seed2-appName}/tor/hiddenservice/default/hostname seed2-tor-hostname

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

.start-tor-seeds: seed-type=tor
.start-tor-seeds:
	# Seed 1
	screen -S localtests -X screen -t ${seed1-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed1-appName} \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1000 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=TOR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
	# Seed 2
	screen -S localtests -X screen -t ${seed2-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed2-appName} \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1001 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=TOR \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001

start-tor-seeds: .init-screen-session .start-tor-seeds .reconnect-screen-session

start-tor-full-env: .init-screen-session .start-tor-seeds .start-tor-clients .reconnect-screen-session


#####
# I2P
#####

# Copy seed 1 destination to a local file, which is read when starting the clients
seed1-i2p-destination: seed-type=i2p
seed1-i2p-destination:
	@cp -v ~/.local/share/${seed1-appName}/i2p/default*.destination seed1-i2p-destination

# Copy seed 2 destination to a local file, which is read when starting the clients
seed2-i2p-destination: seed-type=i2p
seed2-i2p-destination:
	@cp -v ~/.local/share/${seed2-appName}/i2p/default*.destination seed2-i2p-destination

.start-i2p-seeds: seed-type=i2p
.start-i2p-seeds:
	# Seed 1
	screen -S localtests -X screen -t ${seed1-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed1-appName} \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5000 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=I2P \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
	# Seed 2
	screen -S localtests -X screen -t ${seed2-title} ./gradlew --console=plain seed:run \
		-Dbisq.application.appName=${seed2-appName} \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001 \
		-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5001 \
		-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
		-Dbisq.networkServiceConfig.supportedTransportTypes.1=I2P \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
		-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001

# Requires both seed destinations to be known
# If any is not known, this will fail
.start-i2p-clients: seed1-i2p-destination seed2-i2p-destination
	for i in $$(seq 1 $n); do \
  		# Due to very large command size (because of I2P destination args) ;\
  		# the resulting cmd had to be built line by line ;\
  		# See .demo-send-large-command-to-screen for details ;\
  		screen -S localtests -X screen -t client-$${i}-i2p ;\
  		screen -S localtests -X stuff './gradlew --console=plain desktop:run ' ;\
		screen -S localtests -X stuff "-Dbisq.application.appName=bisq_client$${i}_i2p_test " ;\
		screen -S localtests -X stuff '-Dbisq.networkServiceConfig.supportedTransportTypes.0=I2P ' ;\
		screen -S localtests -X stuff '-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.0=$(file < seed1-i2p-destination):5000 ' ;\
		screen -S localtests -X stuff '-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.1=$(file < seed2-i2p-destination):5001 ' ;\
		screen -S localtests -X stuff '\n' ;\
	done

start-i2p-seeds: .init-screen-session .start-i2p-seeds .reconnect-screen-session

start-i2p-clients: .init-screen-session .start-i2p-clients .reconnect-screen-session

start-i2p-full-env: .init-screen-session .start-i2p-seeds .start-i2p-clients .reconnect-screen-session

# Executing commands via the -X flag may fail if the commands are too long
# The alternative then is to "type" the commands in the screen session via `stuff`, then send a '\n' which triggers execution
# See https://unix.stackexchange.com/a/542498
.demo-send-large-command-to-screen:
	screen -c .screenrc-make -dmS new_screen
	screen -S new_screen -X screen -t test-title
	screen -S new_screen -X stuff 'ls \n'
	screen -r new_screen