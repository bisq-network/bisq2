# Overview about the distributed data storage of the P2P network

This document should give a high level overview about the distributed data store concept, the different use cases 
and data types.

## Use cases
We have several use cases for distributing data to the P2P network.
- **AuthenticatedData**: Offers, public chat messages
- **MailboxData**: Mailbox message (if peer is not online we store message in encrypted form in the p2p network, similar to a mailbox)
- **AuthorizedData**: DaoBridge data (proofs for bonded roles,...), Filter, alert, arbitrator/mediator registrations. Those data can only be published by users who have received an authorization (private) key.
- **AppendOnlyData**: Account age witness data, trade statistics in Bisq 1 (not intended to be used in Bisq 2)

## Data types
### AuthenticatedData
Authenticated data require a pubKey when getting added, and we use the hash of the data as key in the storage map. The publisher 
of the data is the owner, and only they can remove the data later. For removing it requires a signature which will be checked 
against the pubKey associated with the previously added data.
We use sequence numbers to ensure the order of the add/remove requests. We store the add or remove request data and keep the remove request stored as well. The remove request do not contain the data payload only the hash, so we are not keeping too much data.
At the get inventory request (at startup nodes request from other nodes their storage content) we provide those add and 
remove requests to ensure that remove requests don't get lost (such an issue happened in Bisq 1 as we only kept add 
requests). Additionally, it comes with the benefit that client code do not need to care how the data arrived, either 
via initial getInventory requests or later as gossip broadcast messages. They always get the add or remove requests. 
This makes the initial inventory request process less problematic (as it is in Bisq 1).
There are also refresh requests which are lightweight requests to increase the sequence number of existing data.

### MailboxData
MailboxData are inheriting the features of AuthenticatedData but handle the remove differently. 
The sender is the data owner but only the receiver is allowed to remove the data. 
So we have 2 pubKeys, one of the sender and one of the receiver.

### AuthorizedData
AuthorizedData provide a signature and pubKey and a method to receive the (hardcoded) list of authorized pubKeys.
At receipt the node verifies if the signature matches the pubKey and if the pubKey is contained in the list.
It is important that the list is not part of the network payload, but it delivered from the users local app as 
hard coded list. Otherwise, it would be insecure as the sender could add their own list and use any self created keypair.

### AppendOnlyData
Those are data which never get deleted. They also do not have a time to life.
To avoid abuse we will require some additional proofs like open-timestamp in the case of account age witness to be sure 
the provided account creation date is correct (in some tolerance range).
We do not plan to use trade statistics as they grow to large data sets, could leak potentially some privacy and cannot be verified 
anyway. Not 100% clear yet if there will be an alternative to get market data but at least it would be another data 
storage solution like a specialized node which holds those data in a database.

## Data stores
The data are held in a hashmap using the hash of the data as key.
We use time to life to purge expired data. All data type has its own dedicated data store (e.g. a file for Offers, a file for mailbox messages,...). The MetaData attached to the data is used to derive properties for the store, like store name, max store size,...
That way, we can limit potential attacks to one type of data and can fine tune the stores according to the expected data volume.
Once the store reaches its limit it would reject newly added data, thus protects itself to run out of memory.
All data is held in memory and is persisted at change (using a lazy persistence approach to reduce disk IO). Only at 
startup we read data from the persisted file. We apply pruning at initial read. Having stored data (also offers) reduces 
potentially data load at the initial data requests in case the user have just restarted. If the TTL have rendered the 
persisted data already invalid the persisted data is pruned, so only costs are disk storage size but that's cheap. 

Data which are provided by the P2P network like offers or public chat messages are stored only at those data stores.
The domain only holds a reference and does not duplicate data in the domain models.

## Data distribution
Data gets broadcast to some of a nodes peers. Each node receiving data will check if they have the data already 
received, if not, they verify the data and if valid they store it and broadcast it to their peers.
This flood fill / gossip distribution strategy ensures that data gets distributed very fast and has some level of redundancy.
We need to find the right balance of resilience/reliability and low redundancy/overhead by adjusting the % of peers we
use or broadcasting (about 70% seems ok). The benefit of such gossip networks (Bitcoin uses it as well) is a high level of 
censorship resistance/resilience and fast propagation. Downside is that it has scaling limitations and dos 
vulnerabilities. To deal with dos vulnerabilities we will use proof of work for each message and the required works will
be adjusted to the type of message/data as well to the load of the receiver node.
Scaling limitations are addressed by storing only relative small data (no images, videos) and by using a TTL for 
pruning data. In case we run into issues we could start with partitioning data (e.g. offers are grouped by markets and 
nodes subscribe and support only markets they are interested in).
Shipping as resource files append only data as done in Bisq 1 might be an option as well once needed. Another option 
would be to use dedicated nodes for providing larger data set if needed. 
