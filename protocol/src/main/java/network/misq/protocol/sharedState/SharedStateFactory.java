package network.misq.protocol.sharedState;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import lombok.Getter;
import network.misq.protocol.SecurityProvider;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO: Need to decide rules concerning mutual incompatibility of @Supplied, @DependsOn, @Access, @Action and @Event annotations.
@Getter
public class SharedStateFactory<T extends SecurityProvider.SharedState> {
    private final Class<T> clazz;
    private final Set<String> parties;
    private final Set<String> propertyNames;
    private final ListMultimap<String, Set<String>> dependencyMultimap;
    private final Map<String, String> actorMap;
    private final Map<String, String> supplierMap;
    private final Map<String, String> eventObserverMap;
    private final Map<String, AccessCondition> declaredAccessConditionMap;
    private final Map<String, AccessCondition.DPF> accessConditionMap;

    public SharedStateFactory(Class<T> clazz) {
        Preconditions.checkArgument(clazz.isInterface(), "Not an interface: %s", clazz);
        this.clazz = clazz;
        parties = processSharedStateAnnotation();
        propertyNames = Arrays.stream(clazz.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .map(Method::getName)
                .collect(ImmutableSet.toImmutableSet());
        dependencyMultimap = processDependsOnAnnotations();
        actorMap = processActionAnnotations();
        supplierMap = processSuppliedAnnotations();
        eventObserverMap = processEventAnnotations();
        declaredAccessConditionMap = processAccessAnnotations();
        accessConditionMap = computeDerivedAccessConditions();
    }

    private Set<String> processSharedStateAnnotation() {
        State annotation = clazz.getAnnotation(State.class);
        Preconditions.checkNotNull(annotation, "Missing @SharedState annotation on %s", clazz);
        Preconditions.checkArgument(annotation.parties().length != 0, "Empty party list on %s", clazz);
        for (String party : annotation.parties()) {
            Preconditions.checkArgument(Character.isJavaIdentifierStart(party.charAt(0)) &&
                            party.chars().allMatch(Character::isJavaIdentifierPart),
                    "Expected: valid Java identifier for party - got: %s", party);
        }
        return ImmutableSet.copyOf(annotation.parties());
    }

    private ListMultimap<String, Set<String>> processDependsOnAnnotations() {
        var multimapBuilder = ImmutableListMultimap.<String, Set<String>>builder();

        for (Method method : clazz.getMethods()) {
            var dependsOnAnnotations = method.getAnnotationsByType(DependsOn.class);
            var name = method.getName();
            Preconditions.checkArgument(dependsOnAnnotations.length == 0 || propertyNames.contains(name),
                    "Illegal @DependsOn annotation on non-property: %s", method);
            Preconditions.checkArgument(dependsOnAnnotations.length == 0 || method.isDefault(),
                    "Missing default method body for property '%s' annotated with @DependsOn", name);

            for (DependsOn annotation : dependsOnAnnotations) {
                Preconditions.checkArgument(annotation.value().length != 0,
                        "Empty dependency list on property '%s'", name);
                for (String dependencyName : annotation.value()) {
                    Preconditions.checkArgument(propertyNames.contains(dependencyName),
                            "Unrecognized dependency '%s' of property '%s'", dependencyName, name);
                }
                multimapBuilder.put(name, ImmutableSet.copyOf(annotation.value()));
            }
        }
        return multimapBuilder.build();
    }

    // TODO: Should we require void return type for actions?
    private Map<String, String> processActionAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var actionAnnotation = method.getAnnotation(Action.class);
            var name = method.getName();

            if (actionAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Action annotation on non-property: %s", method);
                Preconditions.checkArgument(parties.contains(actionAnnotation.by()),
                        "Unrecognized actor '%s' for action '%s'", actionAnnotation.by(), name);
                mapBuilder.put(name, actionAnnotation.by());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, String> processSuppliedAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var suppliedAnnotation = method.getAnnotation(Supplied.class);
            var name = method.getName();

            if (suppliedAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Supplied annotation on non-property: %s", method);
                Preconditions.checkArgument(parties.contains(suppliedAnnotation.by()),
                        "Unrecognized supplier '%s' for property '%s'", suppliedAnnotation.by(), name);
                mapBuilder.put(name, suppliedAnnotation.by());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, String> processEventAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var eventAnnotation = method.getAnnotation(Event.class);
            var name = method.getName();

            if (eventAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Event annotation on non-property: %s", method);
                Preconditions.checkArgument(!dependencyMultimap.containsKey(name),
                        "Illegal simultaneous annotation of property '%s' with @Event and @DependsOn", name);
                Preconditions.checkArgument(parties.contains(eventAnnotation.seenBy()),
                        "Unrecognized observer '%s' for event '%s'", eventAnnotation.seenBy(), name);
                mapBuilder.put(name, eventAnnotation.seenBy());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, AccessCondition> processAccessAnnotations() {
        var mapBuilder = ImmutableMap.<String, AccessCondition>builder();
        var allowedNames = Sets.union(parties, eventObserverMap.keySet());

        for (Method method : clazz.getMethods()) {
            var accessAnnotation = method.getAnnotation(Access.class);
            var name = method.getName();

            if (accessAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Access annotation on non-property: %s", method);
                mapBuilder.put(name, AccessCondition.parse(accessAnnotation.value(), allowedNames));
            }
        }
        return mapBuilder.build();
    }

    private Map<String, AccessCondition.DPF> computeDerivedAccessConditions() {
        var accessConditionMap = new HashMap<>(Maps.asMap(propertyNames, Functions.constant(AccessCondition.none())));
        declaredAccessConditionMap.forEach((id, cond) -> accessConditionMap.merge(id, cond.toDPF(), AccessCondition.DPF::or));
        supplierMap.forEach((id, party) -> accessConditionMap.merge(id, AccessCondition.atom(party), AccessCondition.DPF::or));
        eventObserverMap.forEach((id, party) -> accessConditionMap.merge(id, AccessCondition.atom(id).and(AccessCondition.atom(party)), AccessCondition.DPF::or));

        var dirtyProperties = new HashSet<>(propertyNames);
        var firstRound = true;
        while (true) {
            for (String id : propertyNames) {
                if (dirtyProperties.isEmpty()) {
                    return ImmutableMap.copyOf(accessConditionMap);
                }
                if (!firstRound) {
                    dirtyProperties.remove(id);
                }
                var oldCond = accessConditionMap.get(id);
                for (var dependencies : dependencyMultimap.get(id)) {
                    if (dependencies.stream().anyMatch(dirtyProperties::contains)) {
                        var inheritedCond = dependencies.stream()
                                .map(accessConditionMap::get)
                                .reduce(AccessCondition.DPF::and).orElseThrow();

                        accessConditionMap.merge(id, inheritedCond, AccessCondition.DPF::or);
                    }
                }
                if (!oldCond.equals(accessConditionMap.get(id))) {
                    dirtyProperties.add(id);
                }
            }
            firstRound = false;
        }
    }

    @SafeVarargs
    public static <T> T oneOf(Supplier<T>... alternativeDerivations) {
        return alternativeDerivations[IndexInjector.getIndex()].get();
    }

    private static final Object configurationMonitor = new Object();
    private static volatile boolean eventFiring;
    private static volatile SharedStateFactory<?>.StateInvocationHandler handlerUnderConfiguration;
    private static List<String> getterInvocations;

    public static <T> When<T> when(T result) {
        return new When<>();
    }

    public static class When<T> {
        void thenReturn(T value) {
            synchronized (configurationMonitor) {
                var name = getterInvocations.get(getterInvocations.size() - 1);
                var source = new FieldVersions.ExternalSource(handlerUnderConfiguration.owner);
                handlerUnderConfiguration.putFieldIfAbsent(name, source, value);
            }
        }
    }

    static <T> void fireEvent(Supplier<T> methodReference, T value) {
        SharedStateFactory<?>.StateInvocationHandler handler;
        String event;
        synchronized (configurationMonitor) {
            try {
                eventFiring = true;
                getterInvocations = new ArrayList<>();
                methodReference.get();
                Preconditions.checkArgument(getterInvocations.size() == 1,
                        "Event getter should be invoked exactly once but got invocations: %s", getterInvocations);
                (handler = handlerUnderConfiguration).setEventValue((event = getterInvocations.get(0)), value);
            } finally {
                eventFiring = false;
                handlerUnderConfiguration = null;
                getterInvocations = null;
            }
        }
        handler.updateDependentFields(Set.of(event));
    }

    public T create(String owner, Consumer<T> initializationBlock) {
        Preconditions.checkArgument(parties.contains(owner), "Unknown party: %s", owner);

        var handler = new StateInvocationHandler(owner);
        var state = handler.proxy;
        var fieldsToInitialize = Maps.filterValues(supplierMap, owner::equals).keySet();

        synchronized (configurationMonitor) {
            try {
                handlerUnderConfiguration = handler;
                getterInvocations = new ArrayList<>();
                initializationBlock.accept(state);
                Preconditions.checkArgument(handler.fields.keySet().equals(fieldsToInitialize),
                        "Wrong fields initialized for '%s'. Expected '%s' but got '%s'",
                        owner, fieldsToInitialize, handler.fields.keySet());
                Preconditions.checkArgument(getterInvocations.size() == fieldsToInitialize.size(),
                        "Expected %s getter invocations in initialization block but got %s",
                        fieldsToInitialize.size(), getterInvocations.size());
            } finally {
                handlerUnderConfiguration = null;
                getterInvocations = null;
            }
        }

        handler.updateDependentFields(fieldsToInitialize);
        return state;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static final TypeToken<String> PARTY_TYPE = TypeToken.of(String.class);
    @SuppressWarnings("UnstableApiUsage")
    private static final TypeToken<Map<String, ?>> MESSAGE_TYPE = new TypeToken<>() {
    };

    @SuppressWarnings("UnstableApiUsage")
    private class StateInvocationHandler extends AbstractInvocationHandler {
        private final String owner;
        private final T proxy;
        private final Map<String, FieldVersions<Object>> fields = new ConcurrentHashMap<>();
        private final Set<String> pendingActions = ConcurrentHashMap.newKeySet();
        private final SetMultimap<String, String> recipientMultimap = MultimapBuilder.hashKeys().hashSetValues().build();

        private StateInvocationHandler(String owner) {
            this.owner = owner;
            proxy = Reflection.newProxy(clazz, this);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            Preconditions.checkArgument(this.proxy == proxy);
            var name = method.getName();
            if (args.length == 0) {
                if (this == handlerUnderConfiguration || eventObserverMap.containsKey(name) && eventFiring) {
                    synchronized (configurationMonitor) {
                        if (this == handlerUnderConfiguration || eventObserverMap.containsKey(name) && eventFiring) {
                            handlerUnderConfiguration = this;
                            getterInvocations.add(name);
                            return null;
                        }
                    }
                }
                if (fields.containsKey(name)) {
                    return getField(name);
                }
            }
            if (name.equals("sendMessage") && args.length == 1 &&
                    Invokable.from(method).getParameters().get(0).getType().equals(PARTY_TYPE)) {
                return sendMessage((String) args[0]);
            }
            if (name.equals("receiveMessage") && args.length == 2 &&
                    Invokable.from(method).getParameters().get(0).getType().equals(PARTY_TYPE) &&
                    Invokable.from(method).getParameters().get(1).getType().equals(MESSAGE_TYPE)) {
                return receiveMessage((String) args[0], (Map<String, ?>) args[1]);
            }
            return invokeDefaultMethod(method, args);
        }

        private Object getField(String name) {
            return fields.get(name).getMainVersion();
        }

        private void putFieldIfAbsent(String name, FieldVersions.Source source, Object value) {
            var versions = fields.computeIfAbsent(name, k -> new FieldVersions<>(source, value));
            versions.getAlternativeVersionMap().putIfAbsent(source, value);
        }

        private void computeFieldIfAbsent(String name, FieldVersions.Source source, Supplier<Object> valueSupplier) {
            var versions = fields.computeIfAbsent(name, k -> new FieldVersions<>(source, valueSupplier.get()));
            versions.getAlternativeVersionMap().computeIfAbsent(source, k -> valueSupplier.get());
        }

        private void updateDependentFields(Set<String> newFields) {
            while (!newFields.isEmpty()) {
                var addedFields = new HashSet<String>();
                var notAddedFields = Sets.difference(fields.keySet(), addedFields);
                for (var name : dependencyMultimap.keySet()) {
                    var dependenciesList = dependencyMultimap.get(name);
                    for (int i = 0; i < dependenciesList.size(); i++) {
                        var dependencies = dependenciesList.get(i);
                        if (notAddedFields.containsAll(dependencies) && dependencies.stream().anyMatch(newFields::contains)) {
                            if (actorMap.containsKey(name)) {
                                if (actorMap.get(name).equals(owner)) {
                                    pendingActions.add(name);
                                }
                            } else if (updateDependentField(name, i)) {
                                addedFields.add(name);
                            }
                        }
                    }
                }
                newFields = addedFields;
            }
        }

        private boolean updateDependentField(String name, int index) {
            var source = new FieldVersions.DependencySource(index, dependencyMultimap.get(name).get(index));
            var isUpdated = new AtomicBoolean();
            computeFieldIfAbsent(name, source, () -> {
                try {
                    isUpdated.set(true);
                    var method = clazz.getMethod(name);
                    return IndexInjector.call(() -> invokeDefaultMethod(method), index);
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
            return isUpdated.get() && fields.get(name).getMainVersionSource().equals(source);
        }

        private Object invokeDefaultMethod(Method method, Object... args) throws Throwable {
            var lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
            var methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            var handle = Modifier.isStatic(method.getModifiers())
                    ? lookup.findStatic(method.getDeclaringClass(), method.getName(), methodType)
                    : lookup.findSpecial(method.getDeclaringClass(), method.getName(), methodType, method.getDeclaringClass());
            return handle.bindTo(proxy).invokeWithArguments(args);
        }

        private Map<String, ?> sendMessage(String recipient) {
            var set = Sets.union(fields.keySet(), Set.of(recipient));
            var recipientSource = new FieldVersions.ExternalSource(recipient);
            synchronized (recipientMultimap) {
                var result = fields.keySet().stream()
                        .filter(name -> !recipientMultimap.containsEntry(name, recipient))
                        .filter(name -> !fields.get(name).getAllVersionMap().containsKey(recipientSource))
                        .filter(name -> accessConditionMap.get(name).test(set))
                        .collect(ImmutableMap.toImmutableMap(name -> name, this::getField));

                result.keySet().forEach(id -> recipientMultimap.put(id, recipient));
                return result;
            }
        }

        private Void receiveMessage(String sender, Map<String, ?> message) {
            var source = new FieldVersions.ExternalSource(sender);
            message.forEach((name, value) -> putFieldIfAbsent(name, source, value));
            updateDependentFields(ImmutableSet.copyOf(message.keySet()));
            return null;
        }

        private void setEventValue(String name, Object value) {
            Preconditions.checkArgument(owner.equals(eventObserverMap.get(name)),
                    "Event '%s' not observable by the state owner '%s'", name, owner);
            Preconditions.checkArgument(!fields.containsKey(name), "Event '%s' has already fired", name);
            var source = new FieldVersions.ExternalSource(owner);
            putFieldIfAbsent(name, source, value);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder(clazz.getSimpleName()).append(' ');
            sb.append("{\n  owner = ").append(owner);
            sb.append(",\n  pendingActions = ").append(pendingActions);
            sb.append(",\n  fields = {");
            var len = sb.length();
            fields.forEach((name, versions) ->
                    sb.append(len == sb.length() ? "" : ",").append("\n    ").append(name).append(" = ").append(versions)
                            .append(" (sent to ").append(recipientMultimap.get(name)).append(")")
            );
            return sb.append("\n  }\n}").toString();
        }
    }
}
