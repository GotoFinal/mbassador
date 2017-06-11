package net.engio.mbassy.listener;

import net.engio.mbassy.common.ReflectionUtils;
import net.engio.mbassy.dispatch.HandlerInvocation;
import net.engio.mbassy.dispatch.groovy.GroovyFilter;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Any method in any class annotated with the @Handler annotation represents a message handler. The class that contains
 * the handler is called a  message listener and more generally, any class containing a message handler in its class hierarchy
 * defines such a message listener.
 *
 * @author bennidi
 *         Date: 11/14/12
 */
public class MessageHandler {

    public static final class Properties{

        public static final String MethodHandle = "methodHandle";
        public static final String HandlerMethod = "handler";
        public static final String InvocationMode = "invocationMode";
        public static final String Filter = "filter";
        public static final String Condition = "condition";
        public static final String HandledMessage = "message";
        public static final String IsSynchronized = "synchronized";
        public static final String Listener = "listener";
        public static final String AcceptSubtypes = "subtypes";
        public static final String Priority = "priority";
        public static final String IgnoreCancelled = "ignoreCancelled";
        public static final String Invocation = "invocation";

        /**
         * Create the property map for the {@link MessageHandler} constructor using the default objects.
         *
         * @param handler  The handler annotated method of the listener
         * @param handlerConfig The annotation that configures the handler
         * @param filter   The set of preconfigured filters if any
         * @param listenerConfig The listener metadata
         * @return  A map of properties initialized from the given parameters that will conform to the requirements of the
         *         {@link MessageHandler} constructor.
         */
        public static final Map<String, Object> Create(Method handler,
                                                       Handler handlerConfig,
                                                       IMessageFilter[] filter,
                                                       MessageListener listenerConfig){
            if(handler == null){
                throw new IllegalArgumentException("The message handler configuration may not be null");
            }
            if(handler.getParameterCount() != 1) {
                throw new IllegalArgumentException("The message handler must have only singe parameter");
            }
            if(filter == null){
                filter = new IMessageFilter[]{};
            }
            Map<String, Object> properties = new HashMap<String, Object>();
            Class<?> handledMessage = handler.getParameterTypes()[0];
            handler.setAccessible(true);
            try {
                MethodHandle methodHandle = MethodHandles.lookup().unreflect(handler);
                properties.put(MethodHandle, methodHandle);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            properties.put(HandlerMethod, handler);
            if (handlerConfig.ignoreCancelled() && Cancellable.class.isAssignableFrom(handledMessage)) {

                IMessageFilter[] expandedFilter = new IMessageFilter[filter.length + 1];
                System.arraycopy(filter, 0, expandedFilter, 0, filter.length);
                expandedFilter[filter.length] = (IMessageFilter<Cancellable>) (msg, c) -> ! msg.isCancelled();
                filter = expandedFilter;
            }
            // add Groovy filter if a condition is present
            if(! handlerConfig.condition().isEmpty()){
                if (! GroovyFilter.isGroovyAvailable()) {
                    throw new IllegalStateException("A handler uses an groovy filter but no groovy implementation is available.");
                }

                IMessageFilter[] expandedFilter = new IMessageFilter[filter.length + 1];
                System.arraycopy(filter, 0, expandedFilter, 0, filter.length);
                expandedFilter[filter.length] = GroovyFilter.create(handlerConfig.condition(), handler.getParameterTypes()[0]);
                filter = expandedFilter;
            }
            properties.put(Filter, filter);
            properties.put(Condition, handlerConfig.condition());
            properties.put(Priority, handlerConfig.priority());
            properties.put(IgnoreCancelled, handlerConfig.ignoreCancelled());
            properties.put(Invocation, handlerConfig.invocation());
            properties.put(InvocationMode, handlerConfig.delivery());
            properties.put(AcceptSubtypes, !handlerConfig.rejectSubtypes());
            properties.put(Listener, listenerConfig);
            properties.put(IsSynchronized, ReflectionUtils.getAnnotation( handler, Synchronized.class) != null);
            properties.put(HandledMessage, handledMessage);
            return properties;
        }
    }

    private final MethodHandle methodHandle;

    private final Method handler;

    private final IMessageFilter[] filter;

	private final String condition;

    private final int priority;

    private final boolean ignoreCancelled;

    private final Class<? extends HandlerInvocation> invocation;

    private final Invoke invocationMode;

    private final Class<?> handledMessage;

    private final boolean acceptsSubtypes;

    private final MessageListener listenerConfig;

    private final boolean isSynchronized;


    public MessageHandler(Map<String, Object> properties){
        super();
        validate(properties);
        this.methodHandle = (MethodHandle) properties.get(Properties.MethodHandle);
        this.handler = (Method)properties.get(Properties.HandlerMethod);
        this.filter = (IMessageFilter[])properties.get(Properties.Filter);
        this.condition = (String)properties.get(Properties.Condition);
        this.priority = (Integer)properties.get(Properties.Priority);
        this.ignoreCancelled = (Boolean) properties.get(Properties.IgnoreCancelled);
        this.invocation = (Class<? extends HandlerInvocation>)properties.get(Properties.Invocation);
        this.invocationMode = (Invoke)properties.get(Properties.InvocationMode);
        this.acceptsSubtypes = (Boolean)properties.get(Properties.AcceptSubtypes);
        this.listenerConfig = (MessageListener)properties.get(Properties.Listener);
        this.isSynchronized = (Boolean)properties.get(Properties.IsSynchronized);
        this.handledMessage = (Class<?>)properties.get(Properties.HandledMessage);
    }

    private void validate(Map<String, Object> properties){
        // define expected types of known properties
        Object[][] expectedProperties = new Object[][]{
                new Object[]{Properties.MethodHandle, MethodHandle.class },
                new Object[]{Properties.HandlerMethod, Method.class },
                new Object[]{Properties.Priority, Integer.class },
                new Object[]{Properties.IgnoreCancelled, Boolean.class },
                new Object[]{Properties.Invocation, Class.class },
                new Object[]{Properties.Filter, IMessageFilter[].class },
                new Object[]{Properties.Condition, String.class },
                new Object[]{Properties.HandledMessage, Class.class },
                new Object[]{Properties.IsSynchronized, Boolean.class },
                new Object[]{Properties.Listener, MessageListener.class },
                new Object[]{Properties.AcceptSubtypes, Boolean.class }
        };
        // ensure types match
        for(Object[] property : expectedProperties){
            if (properties.get(property[0]) == null || !((Class)property[1]).isAssignableFrom(properties.get(property[0]).getClass()))
                throw new IllegalArgumentException("Property " + property[0] + " was expected to be not null and of type " + property[1]
                        + " but was: " + properties.get(property[0]));
        }
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType){
        return ReflectionUtils.getAnnotation(handler,annotationType);
    }

    public boolean isSynchronized(){
        return isSynchronized;
    }

    public boolean useStrongReferences(){
        return listenerConfig.useStrongReferences();
    }

    public boolean isFromListener(Class listener){
        return listenerConfig.isFromListener(listener);
    }

    public boolean isAsynchronous() {
        return invocationMode.equals(Invoke.Asynchronously);
    }

    public boolean isFiltered() {
        return filter.length > 0 || (condition != null && condition.trim().length() > 0);
    }

    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    public int getPriority() {
        return priority;
    }

    public MethodHandle getMethodHandle() {
        return methodHandle;
    }

    public Method getMethod() {
        return handler;
    }

    public IMessageFilter[] getFilter() {
        return filter;
    }

    public String getCondition() {
    	return this.condition;
    }

    public Class<?> getHandledMessage() {
        return handledMessage;
    }

    public Class<? extends HandlerInvocation> getHandlerInvocation(){
        return invocation;
    }

    public boolean handlesMessage(Class<?> messageType) {
        return handledMessage.equals(messageType) || (acceptsSubtypes() && handledMessage.isAssignableFrom(messageType));
    }

    public boolean acceptsSubtypes() {
        return acceptsSubtypes;
    }

}
