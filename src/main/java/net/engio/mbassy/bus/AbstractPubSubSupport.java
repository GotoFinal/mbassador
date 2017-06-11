package net.engio.mbassy.bus;

import net.engio.mbassy.bus.common.DeadMessage;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.config.ConfigurationError;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.subscription.Subscription;
import net.engio.mbassy.subscription.SubscriptionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.engio.mbassy.bus.config.IBusConfiguration.Properties.BusId;
import static net.engio.mbassy.bus.config.IBusConfiguration.Properties.PublicationErrorHandlers;

/**
 * The base class for all message bus implementations.
 *
 * @param <T>
 */
public abstract class AbstractPubSubSupport<T> implements PubSubSupport<T> {


    // this handler will receive all errors that occur during message dispatch or message handling
    private final List<IPublicationErrorHandler> errorHandlers = new ArrayList<IPublicationErrorHandler>();

    // Cache already processed events that do not have any subscribers
    private final Set<Class> unusedEvents = Collections.newSetFromMap(new ConcurrentHashMap<>(200));

    public void clearUnusedEventsCache() {
        unusedEvents.clear();
    }

    public boolean isUnusedEventType(Class<?> type) {
        return unusedEvents.contains(type);
    }

    private final boolean useUnusedEvent;

    private final MessagePublication.Factory publicationFactory;

    private final SubscriptionManager subscriptionManager;

    private final BusRuntime runtime;

    public static final String ERROR_HANDLER_MSG = "INFO: No error handler has been configured to handle exceptions during publication.\n" +
            "Publication error handlers can be added by IBusConfiguration.addPublicationErrorHandler()\n" +
            "Falling back to console logger.";

    public AbstractPubSubSupport(IBusConfiguration configuration) {
        //transfer publication error handlers from the config object
        this.errorHandlers.addAll(configuration.getRegisteredPublicationErrorHandlers());
        if (errorHandlers.isEmpty()) {
            errorHandlers.add(new IPublicationErrorHandler.ConsoleLogger());
            System.out.println(ERROR_HANDLER_MSG);
        }
        this.useUnusedEvent = configuration.getProperty("useUnusedEvent", true);
        this.runtime = new BusRuntime(this)
                .add(PublicationErrorHandlers, configuration.getRegisteredPublicationErrorHandlers())
                .add(BusId, configuration.getProperty(BusId, UUID.randomUUID().toString()));
        // configure the pub sub feature
        Feature.SyncPubSub pubSubFeature = configuration.getFeature(Feature.SyncPubSub.class);
        if(pubSubFeature == null){
            throw ConfigurationError.MissingFeature(Feature.SyncPubSub.class);
        }
        this.subscriptionManager = pubSubFeature.getSubscriptionManagerProvider()
                .createManager(pubSubFeature.getMetadataReader(), pubSubFeature.getSubscriptionFactory(), runtime);
        this.publicationFactory = pubSubFeature.getPublicationFactory();
    }

    protected MessagePublication.Factory getPublicationFactory() {
        return publicationFactory;
    }


    public Collection<IPublicationErrorHandler> getRegisteredErrorHandlers() {
        return Collections.unmodifiableCollection(errorHandlers);
    }

    public boolean unsubscribe(Object listener) {
        if (subscriptionManager.unsubscribe(listener)) {
            clearUnusedEventsCache();
            return true;
        }
        return false;
    }

    public void subscribe(Object listener) {
        if (subscriptionManager.subscribe(listener)) {
            clearUnusedEventsCache();
        }
    }

    @Override
    public BusRuntime getRuntime() {
        return runtime;
    }

    protected MessagePublication createMessagePublication(T message) {
        Class<?> messageClass = message.getClass();
        if (isUnusedEventType(messageClass))
            return null;
        Collection<Subscription> subscriptions = this.getSubscriptionsByMessageType(messageClass);
        if ((subscriptions == null) || subscriptions.isEmpty()) {
            if (this.useUnusedEvent) {
                if (message.getClass().equals(DeadMessage.class)) {
                    return null;
                }
                subscriptions = getSubscriptionsByMessageType(DeadMessage.class);
                return getPublicationFactory().createPublication(runtime, subscriptions, new DeadMessage(message));
            }
            // unused event
            unusedEvents.add(messageClass);
            return null;
        } else {
            return this.getPublicationFactory().createPublication(this.getRuntime(), subscriptions, message);
        }
    }

    // obtain the set of subscriptions for the given message type
    // Note: never returns null!
    protected Collection<Subscription> getSubscriptionsByMessageType(Class messageType) {
        return subscriptionManager.getSubscriptionsByMessageType(messageType);
    }


    protected void handlePublicationError(PublicationError error) {
        for (IPublicationErrorHandler errorHandler : errorHandlers) {
            try
            {
                errorHandler.handleError(error);
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + runtime.get(IBusConfiguration.Properties.BusId) + ")";
    }
}
