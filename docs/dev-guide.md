# Developer guild lines

This is a preliminary collection of description of patterns, developer guild lines and frameworks we use in Bisq 2.
For general guideline see also: https://github.com/bisq-network/bisq/blob/master/CONTRIBUTING.md

## Dependencies
We try to avoid adding dependencies as far as possible to reduce the risk for supply chain attacks. Sticking to plain 
Java libraries is preferred over using 3rd party libraries.

## Asynchronous handling
We use the CompletableFutures framework of Java for dealing with asynchronous code.

## MVC pattern
There are plenty of variations of MVC design patterns, and they vary depending on the feature set of the UI framework and language.
The one use here in Bisq 2 is a pretty straight classical one but adjusted to the JavaFX features (e.g. data binding). 
In Bisq 1 we use the MVVM pattern, but it did not work out that well as the responsibilities have not been that clear and
the view classed tended to become large and complex. To avoid that we try to break up larger views into a composition 
of components as well as stick strictly to the pattern as described below.
An important aspect is also the role of the domain model which we call Service in Bisq 2. The MCV triad carries the pure UI 
related code. Everything which is domain related lives in the service classes from the different domain modules.

### MCV core classes

#### Controller
The controller class is the core of the MCV hierarchy and creates the other 2 classes.
It is responsible for any behaviour/logic and access to other services or sub-components. It is the only class visible to 
other parts of the application.
We pass usually the applicationService as provider for the domain service classes. 
The controller never calls methods on the view but sets properties in the model and the view listens to changes on those 
properties to react on the change.
The controller might listen on changes in services and apply the changes to the model. 

#### Model
The model is holding state and bindable properties or observable collections. It does not contain any logic and 
usually the data are applied by the controller. It does not know about the view or the controller.

#### View
The view gets passed both the controller and the model. It is responsible for the graphical representation.
It does not contain any domain logic. Simple view/layout logic is ok.
It binds the properties of its component (e.g. textProperty of a label) to the property in the model in case it is a 
dynamically changing value or otherwise call a getter at the model. Trivial values like resource strings are applied directly.
It calls handler methods on the controllers for UI events like button clicks or text input. We use the "on" prefix as 
convention for such UI handler methods (e.g. `onClose`). It does not call setter methods on the model but use the model 
only for reading data.  

### View graph
The graph of the views is constructed from the controllers. A controller creates the controller for the child view 
and by setting the child view to the model the listener in the view attaches that child view to its container node.
Usually that happens via navigation controllers (see below).
Popups which carry MVC classes are handled in a similar way. The OverlayController is a singleton which manages 
navigation targets which are defined as overlay (by using OVERLAY as its parent).

Other light-weight popups or popover are not using the MVC pattern.

#### Components
To avoid large complex views we try to break it up into smaller components which are following as well the MVC pattern, 
but they are not using separate classes to avoid too much boilerplate. They use inner classes and use by convention 
`Model`, `View` and `Controller` as the class names. The outer component class creates the controller and acts as 
interface for the client using it. All the inner classes are private and not exposing anything to the clients.
To avoid boilerplate we do not use getter/setters inside those MCV classes but access the properties directly.
In the normal MVC classes we use private fields and Lombok Getter annotation. 

#### Typical use cases
A typical use case could look like following:

Controller registers on a property change event of the selected channel at the `ChatService`. On a change it maps the domain 
data to the model data which is usually adjusted to the needs of the view. Let's assume we want the channel name. So it maps 
the `selectedChannel` to a string and set it in the models `channelName` which is type of `StringProperty`.
The view had created a binding of the channel name label to the models `channelName` property and gets automatically 
updated once the `channelName` gets set. A remove button registers an `onAction` handler and calls the `onCloseChannel` 
method on the controller. The method calls the `removeChannel` method on the `ChatService` and we pass the 
`selectedChannel` as parameter.
So the UI only manages the view related state. Domain state is handled in the service classes.

### Life cycle management
When a view gets added (or removed) to the scene we are calling life cycle methods on the controller and the view class. 
The model does not need it as it does not do anything where resources are allocated/deallocated.
On the controller those methods are: `onActivate` and `onDeactivate`.
At the view they are called: `onViewAttached` and `onViewDetached`.

Any listeners, binding or subscriptions have to be done inside the `onActivate`/`onViewAttached` methods and the removal 
of listeners, unbind or unsubscribing is done at the `onDeactivate`/`onViewDetached` methods.

We handle resource management manually even in most cases it would not lead to memory leaks as the observable where we attach
ourselves as listener gets usually removes as well. But there are edge cases where that does not happen and if we would by 
default not handle it we would run for sure into some memory-leak issues which would be likely very difficult to locate 
in a large and complex application.
The exception when this is not needed are singleton classes which are never removed once created.

### Observer patterns
We use the JavaFX bindings for property bindings. Instead of the standard listeners we prefer to use the `EasyBind` library which has
the benefit that it calls the handler method at registration time, which is with listeners not the case, and it's a 
common source for bugs to forget to call the handler manually when the listeners are registered.
For non-UI code (services) we use our own observer implementations (`FxBindings` and `bisq.common.observable` package). 
See example use cases here:
```
selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile)
                    .to(chatUserService.getSelectedUserProfile());
                        
userProfilesPin = FxBindings.<ChatUserIdentity, ListItem>bind(model.userProfiles)
                    .map(ListItem::new)
                    .to(chatUserService.getUserProfiles());
```

### Navigation
Our navigation framework is based on hierarchical navigation targets. Each target defines its parent and when the 
navigation target is resolved by the framework the potential parents get updated as well.
E.g. If we navigate to the Network tab inside the settings screen the navigation hierarchy is:
`ROOT -> PRIMARY_STAGE -> MAIN-> CONTENT-> SETTINGS -> NETWORK_INFO`
This will trigger an navigation update at each class handling any element in the path. E.g. the ContentController will 
set the SettingsController as its child and the SettingsController will set the NetworkInfoController as its child.
The first targets are static and do not change usually so they will not be affected as they are already in the 
correct state (e.g. PrimaryController has MainController as its child and MainController has ContentController).

A navigation controller class is an extension of the normal controller and supports navigation handling of child views.
It passes the navigation target for which the class is responsible for and creates the sub views when the navigation target 
is called.
E.g. The ContentController passes CONTENT to the NavigationController super class, signalling that it is interested in navigation targets which have CONTENT in their path.
In the `createController` method it handles in the switch cases all its children and creates the controller.
For `ContentController` it looks like that:
```
protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case DASHBOARD -> {
                return Optional.of(new DashboardController(applicationService));
            }
            case DISCUSS -> {
                return Optional.of(new DiscussionsController(applicationService));
            }
            ...
```

Controllers are by default cached, so their constructor is only called ones, but the `onActivate` and `onDeactivate` methods
are called when the views get added or removed. 
Caching behaviour can be overwritten by implementing the `useCaching` method.

There is also a TabNavigationController for supporting tab navigation use cases.

## General code guidelines
- Use Lombok annotations for Getter/Setter/ToString/EqualsAndHashCode. Be cautious with using more advanced Lombok features.
- Use the most narrow visibility scope.
- Use clear variable names, not one letter variables (except in loops). Using the type as variable name is mostly a good choice and helps with refactoring and search.
- Set organize imports and reformat code at the commit screen in IDE. It helps to reduce formatting diffs.
- The style convention is to follow the autoformatting rules provided by IntelliJ's IDEA by default. Hence, there is no need to customize the IDEA's settings in any way, and you can simply use it as-is out of the box.
- Use curly brackets even in one-liners. It's a common source for bugs when later code gets added and, it improves readability.
- Don't use the final keyword in local scope or with arguments, only in class fields
- Try to use final class fields and avoid nullable values.
- Use Optional if a nullable value is a valid case.
- If nullable fields are used, use the @Nullable annotation
- Nullable values in domain code should be avoided as far as possible. In UI code its unfortunately not that easy as JavaFX framework classes often deal with nullable values.
- If you need to write a lot of documentation ask yourself if instead the method name or variable name could be improved. If the method is too complex break it up.
- Don't use trivial and boilerplate java doc. Use java doc only in API level classes. 
- If parameters are getting too long, break it up as single param per line
- When using fluent interface break up in lines at each `.` 
- Use separator lines if classes gets larger to logically group methods
- Use Java records only for simple value objects. Converting them later to normal classes is a bit cumbersome.
- Use @Override when overriding methods