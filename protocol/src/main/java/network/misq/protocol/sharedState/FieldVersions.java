package network.misq.protocol.sharedState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FieldVersions<T> {
    private final Source mainVersionSource;
    private final T mainVersion;
    // TODO: Improve naming:
    private final Map<Source, T> alternativeVersionMap = new ConcurrentHashMap<>();
    private final Map<Source, T> allVersionMap;

    public FieldVersions(Source mainVersionSource, T mainVersion) {
        this.mainVersionSource = Preconditions.checkNotNull(mainVersionSource);
        this.mainVersion = Preconditions.checkNotNull(mainVersion);
        alternativeVersionMap.put(mainVersionSource, mainVersion);
        allVersionMap = Maps.asMap(
                Sets.union(ImmutableSet.of(mainVersionSource), alternativeVersionMap.keySet()),
                source -> mainVersionSource.equals(source) ? mainVersion : alternativeVersionMap.get(source)
        );
    }

//    public T putIfAbsent(Source source, T newVersion) {
//        if (!mainVersionSource.equals(source)) {
//        return alternativeVersionMap.putIfAbsent(source, newVersion);
//        }
//    }

    public static abstract class Source {
        private Source() {
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DependencySource extends Source {
        private final int index;
        private final Set<String> dependencies;

        public DependencySource(int index, Set<String> dependencies) {
            this.index = index;
            this.dependencies = ImmutableSet.copyOf(dependencies);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class ExternalSource extends Source {
        private final String party;
    }

    @Override
    public String toString() {
        return allVersionMap.toString();
    }
}
