package bisq.network.p2p.services.peergroup;

import bisq.persistence.PersistableStore;
import lombok.Getter;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PersistedPeersStore implements PersistableStore<PersistedPeersStore> {

    @Getter
    private final Set<Peer> persistedPeers = new CopyOnWriteArraySet<>();

    public PersistedPeersStore() {}

    public PersistedPeersStore(Set<Peer> persistedPeers) {
        this.persistedPeers.addAll(persistedPeers);
    }

    @Override
    public PersistedPeersStore getClone() {return new PersistedPeersStore(persistedPeers); }

    public void add(Peer peer) {
        persistedPeers.add(peer);
    }

    public void addAll(Collection<Peer> peers) { persistedPeers.addAll(peers); }

    public void remove(Peer peer) { persistedPeers.remove(peer); }

    public void removeAll(Collection<Peer> peers) { persistedPeers.removeAll(peers); }

    @Override
    public void applyPersisted(PersistedPeersStore persisted) {
        persistedPeers.clear();
        persistedPeers.addAll(persisted.getPersistedPeers());
    }
}