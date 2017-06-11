package net.engio.mbassy.listener;

import net.engio.mbassy.common.IPredicate;
import net.engio.mbassy.common.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The meta data reader is responsible for parsing and validating message handler configurations.
 *
 * @author bennidi
 *         Date: 11/16/12
 */
public class MetadataReader {

    //  This predicate is used to find all message listeners (methods annotated with @Handler)
    private static final IPredicate<Method> AllMessageHandlers = new IPredicate<Method>() {
        @Override
        public boolean apply(Method target) {
            return ReflectionUtils.getAnnotation(target, Handler.class) != null;
        }
    };

    // cache already created filter instances
    private final Map<Class<? extends IMessageFilter>, IMessageFilter> filterCache = new HashMap<Class<? extends IMessageFilter>, IMessageFilter>();

    // retrieve all instances of filters associated with the given subscription
    private IMessageFilter[] getFilter(Handler subscription) {
        if (subscription.filters().length == 0) {
            return null;
        }
        IMessageFilter[] filters = new IMessageFilter[subscription.filters().length];
        int i = 0;
        for (Filter filterDef : subscription.filters()) {
            IMessageFilter filter = filterCache.get(filterDef.value());
            if (filter == null) {
                try {
                    filter = filterDef.value().newInstance();
                    filterCache.put(filterDef.value(), filter);
                } catch (Exception e) {
                    throw new RuntimeException(e);// propagate as runtime exception
                }
            }
            filters[i] = filter;
            i++;
        }
        return filters;
    }

    // get all listeners defined by the given class (includes
    // listeners defined in super classes)
    public MessageListener getMessageListener(Class target) {
        MessageListener listenerMetadata = new MessageListener(target);
        // get all handlers (this will include all (inherited) methods directly annotated using @Handler)
        Method[] allHandlers = ReflectionUtils.getMethods(AllMessageHandlers, target);
        final int length = allHandlers.length;

        Method handler;
        for (int i = 0; i < length; i++) {
            handler = allHandlers[i];

            // retain only those that are at the bottom of their respective class hierarchy (deepest overriding method)
            if (!ReflectionUtils.containsOverridingMethod(allHandlers, handler)) {

                // for each handler there will be no overriding method that specifies @Handler annotation
                // but an overriding method does inherit the listener configuration of the overwritten method

                Handler handlerConfig = ReflectionUtils.getAnnotation( handler, Handler.class);
                if (!handlerConfig.enabled() || !isValidMessageHandler(handler)) {
                    continue; // disabled or invalid listeners are ignored
                }
                Method overriddenHandler = ReflectionUtils.getOverridingMethod(handler, target);
                // if a handler is overwritten it inherits the configuration of its parent method
                Map<String, Object> handlerProperties = MessageHandler.Properties.Create(overriddenHandler == null ? handler : overriddenHandler,
                                                                                         handlerConfig,
                                                                                         getFilter(handlerConfig),
                                                                                         listenerMetadata);
                MessageHandler handlerMetadata = new MessageHandler(handlerProperties);
                listenerMetadata.addHandler(handlerMetadata);
            }
        }

        return listenerMetadata;
    }

    private boolean isValidMessageHandler(Method handler) {
        if (handler == null || ReflectionUtils.getAnnotation( handler, Handler.class) == null) {
            return false;
        }
        if (handler.getParameterTypes().length != 1) {
            // a messageHandler only defines one parameter (the message)
            System.out.println("Found no or more than one parameter in messageHandler [" + handler.getName()
                    + "]. A messageHandler must define exactly one parameter");
            return false;
        }
        return true;
    }
}
