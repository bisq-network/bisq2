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
# 1) Local clearnet scenario
#
# Launch 2 clearnet seeds and several (n) clearnet desktop clients with:
#
#     $ make start-local-clearnet n=4
#
# When not specified, n will default to 2, so this will start 2 seeds and 2 clients:
#
#     $ make start-local-clearnet
#

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

# Set n to 2, if otherwise not set
n ?= 2
.start-clearnet-clients:
	for i in $$(seq 1 $n); do \
		screen -S localtests -X screen -t client-$$i ./gradlew --console=plain desktop:run \
			-Dbisq.application.appName=bisq_$${i}_test \
			-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
			-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001; \
	done

# Reattach to the screen session
.reconnect-screen-session:
	screen -r localtests


start-local-clearnet: .start-local-clearnet-seeds .start-clearnet-clients .reconnect-screen-session

