# Bisq v2 Motivations

Bisq v1 achieved a lot since it went live on mainnet in April 2016, but it's far from perfect. This doc covers its advances and shortcomings in order to put the [aims for Bisq v2](./aims.md) in context.

Contents:
- [Market Positioning](#market-positioning)
- [Security](#security)
- [Traction](#traction)
- [Operation](#operation)

## Market Positioning

### True P2P fiat-cryptocurrency exchange network

_ACHIEVEMENT_

As of the start of 2022, Bisq v1 remains the [only proper peer-to-peer solution](https://bisq.wiki/Introduction) to trade cryptocurrencies for fiat currencies. 

Most peer-to-peer exchange mechanisms (THORChain, Uniswap, Chia, etc) don’t handle legacy fiat—they tend to use stablecoins to provide ‘fiat’ trading pairs. Bisq’s unique trade protocol handles legacy fiat exchange through bank transfers, payment services, cash transfers, money orders, in-person meetups, etc. 

[Handling legacy fiat](https://bisq.wiki/Trading_rules) is complicated, messy, and imperfect but Bisq’s trade protocol has been refined to handle it fairly well over the past 5.5 years (~95% of trades complete without dispute). 

### Rigidity

_WEAKNESS_

A significant downside of Bisq v1 is rigidity. 

Some examples:
- all peers must use a single specific on-chain trade protocol
- all peers must use a specific security mechanism (2-of-2 multisig + delayed payout tx)
- all peers must use a single specific interface (desktop GUI)
- all peers must a single dispute resolution mechanism (mediation, arbitration, DAO)
- all trades be for a limited set of assets (only those developers decide to include in the software)

Another notable technical weakness is its on-chain trade protocol, which works well when block space is available but will make smaller trades increasingly expensive as block space becomes more scarce.

## Security

### Strong security record

_ACHIEVEMENT_

Bisq’s no-honeypot P2P model and constant updates have resulted in a strong security record with only [1 notable incident](https://bisq.network/statement-security-vulnerability-april-2020), an exploit that affected 7 users (full USD value repayments were made to those users from trading fee revenues).

### Liabilities

_WEAKNESS_

Although Bisq has done fairly well with security over time, there are potential outstanding liabilities:

- having a wallet integrated in the software is a big liability that may not be strictly necessary
- current P2P network design puts a lot of trust in seed nodes (ideally seed nodes have reduced roles so that exploits and/or downtime don’t have an outsized impact on the network)
- the whole Bisq network depends on Tor, and when Tor was attacked in the past, the Bisq network suffered
- current network setup does not protect well against denial-of-service attacks

## Traction

### Notable growth

_ACHIEVEMENT_

Over the past several years, Bisq has become a cornerstone project in the Bitcoin space. Its reputation is solid, word-of-mouth mentions are strong, and network activity continues to climb gradually. 

As of December 2021, there were over [130,000 total trades](https://twitter.com/bisq_network/status/1482004388382076930) moving [200-300 BTC per month](https://docs.google.com/spreadsheets/d/1o-I5fAx7DJRVqYjW8fPbo0ztlGIhIZ1EM2iLc5aEHnA/edit#gid=498306346) on the network. For such a technically-advanced peer-to-peer network, this is a relatively high level of activity that few other P2P networks have ever attained.

### Limited API usage

_WEAKNESS_

Lack of more accessible interfaces makes it harder to expand into a mainstream user base and emerging markets. An API has been out for a while that should enable friendlier interfaces, mobile apps, trading bots, presence on node-in-a-box solutions, and integrations with other software/apps, but these tools have not emerged yet.

## Operation


### Novel mechanism to fund and govern

_ACHIEVEMENT_

Bisq's [Bitcoin-based DAO](https://bisq.wiki/Introduction_to_the_DAO) (launched in April 2019) funds Bisq development by routing trade fee revenues to contributors without any legal entities or centrally-owned wallets. Most claimed ‘DAOs’ are Ethereum-based, rely strongly on centrally-managed resources, and seem to function primarily as crowdfunding mechanisms. On the other hand, the Bisq DAO is a fully Bitcoin-based P2P governance structure that enables fully non-custodial dispute resolution (2-of-2 multisig), self-sustaining project funding, and a smooth authoritative decision-making mechanism.

The Bisq DAO has operated virtually problem-free since it was launched on mainnet [over 30 months ago](https://bisq.network/dashboard/). There are no conventional investors, street addresses, or bank accounts involved in making the Bisq DAO possible—just code. And there was no scammy ICO-like funding event needed to make it possible either.

### Limited resources

_WEAKNESS_

While the Bisq DAO is a remarkable achievement, people use it and understand it less than expected. This results in workarounds like the [burningman](https://github.com/bisq-network/roles/issues/80), as well as (perhaps) less community involvement since potential contributors may be hesitant to participate in something unfamiliar like the DAO.  

---

**These factors form the motivations for Bisq v2. See [this doc](./aims.md) for a summary of Bisq v2's aims.**