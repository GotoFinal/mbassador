package net.engio.mbassy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.common.MessageBusTest;
import net.engio.mbassy.listener.Filter;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.IMessageFilter;
import net.engio.mbassy.listener.MessageHandler;
import net.engio.mbassy.listener.Synchronized;
import net.engio.mbassy.subscription.SubscriptionContext;

/**
 * Tests a custom handler annotation with a @Handler meta annotation and a default filter.
 */
public class CustomHandlerAnnotationTest extends MessageBusTest
{
	/**
	 * Handler annotation that adds a default filter on the NamedMessage.
	 */
	@Retention(value = RetentionPolicy.RUNTIME)
	@Inherited
	@Handler(filters = { @Filter(NamedMessageFilter.class) })
	@Synchronized
	@Target(value = { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@interface NamedMessageHandler
	{
		/**
		 * @return The message names supported.
		 */
		String[] value();
	}

    /**
     * Handler annotation that adds a default filter on the NamedMessage.
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Inherited
    @NamedMessageHandler("messageThree")
    @interface MessageThree {}



	/**
	 * Test enveloped meta annotation.
	 */
	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Inherited
	@Handler(filters = { @Filter(NamedMessageFilter.class) })
	@interface SomeNamedMessageHandler
	{
		/**
		 * @return The message names supported.
		 */
		String[] value();
	}

	/**
	 * Searches for a NamedMessageHandler annotation on the handler method.
	 * The annotation specifies the supported message names.
	 */
	public static class NamedMessageFilter implements IMessageFilter<NamedMessage>
	{
		@Override
		public boolean accepts( NamedMessage message,  SubscriptionContext context ) {
            MessageHandler handler = context.getHandler();
			NamedMessageHandler namedMessageHandler = handler.getAnnotation(NamedMessageHandler.class);
			if ( namedMessageHandler != null ) {
				return Arrays.asList(namedMessageHandler.value() ).contains(message.getName() );
			}

			SomeNamedMessageHandler envelopedHandler = handler.getAnnotation(SomeNamedMessageHandler.class);
			return envelopedHandler != null && Arrays.asList( envelopedHandler.value() ).contains( message.getName() );

		}
	}

	static class NamedMessage
	{
		private String name;

		NamedMessage( String name ) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	static class NamedMessageListener
	{
		final Set<NamedMessage> handledByOne = new HashSet<NamedMessage>();
		final Set<NamedMessage> handledByTwo = new HashSet<NamedMessage>();
		final Set<NamedMessage> handledByThree = new HashSet<NamedMessage>();

		@NamedMessageHandler({ "messageOne", "messageTwo" })
		void handlerOne( NamedMessage message ) {
			handledByOne.add( message );
		}

		@SomeNamedMessageHandler({ "messageTwo", "messageThree" })
		void handlerTwo( NamedMessage message ) {
			handledByTwo.add(message);
		}

		@MessageThree
		void handlerThree( NamedMessage message ) {
			handledByThree.add( message );
		}
	}

	@Test
	public void testMetaHandlerFiltering() {
		MBassador bus = createBus(SyncAsync());

		NamedMessageListener listener = new NamedMessageListener();
		bus.subscribe( listener );

		NamedMessage messageOne = new NamedMessage( "messageOne" );
		NamedMessage messageTwo = new NamedMessage( "messageTwo" );
		NamedMessage messageThree = new NamedMessage( "messageThree" );

		bus.publish( messageOne );
		bus.publish( messageTwo );
		bus.publish( messageThree );

        assertEquals(2, listener.handledByOne.size());
		assertTrue( listener.handledByOne.contains( messageOne ) );
		assertTrue(listener.handledByOne.contains(messageTwo));

        assertEquals(1, listener.handledByThree.size());
		assertTrue( listener.handledByThree.contains( messageThree ) );
	}
}
