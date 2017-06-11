package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.subscription.SubscriptionContext;

import java.lang.invoke.MethodHandle;

/**
 * Uses reflection to invoke a message handler for a given message.
 *
 * @author bennidi
 *         Date: 11/23/12
 */
public class ReflectiveHandlerInvocation extends HandlerInvocation {

    public ReflectiveHandlerInvocation(SubscriptionContext context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(final Object listener, final Object message, MessagePublication publication) {

        final MethodHandle handler = getContext().getHandler().getMethodHandle();
        try {
            handler.invoke(listener, message);
        } catch (Throwable e) {
            handlePublicationError(publication, new PublicationError(e, "Error during invocation of message handler. The handler code threw an exception",
                                                                     getContext().getHandler().getMethod(), listener, publication));
        }
    }
}
