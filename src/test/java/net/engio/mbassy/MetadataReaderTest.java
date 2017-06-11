package net.engio.mbassy;

import net.engio.mbassy.common.AssertSupport;
import net.engio.mbassy.listener.MessageListener;
import org.junit.Test;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.MetadataReader;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import static net.engio.mbassy.listener.MessageListener.ForMessage;

/**
 *
 * @author bennidi
 *         Date: 12/16/12
 */
public class MetadataReaderTest extends AssertSupport {

    private MetadataReader reader = new MetadataReader();

    @Test
    public void testListenerWithoutInheritance() {
        MessageListener<MessageListener1> listener = reader.getMessageListener(MessageListener1.class);
        ListenerValidator validator = new ListenerValidator()
                .expectHandlers(2, String.class)
                .expectHandlers(2, Object.class)
                .expectHandlers(1, BufferedReader.class);
        validator.check(listener);
    }

    /*
    public void testInterfaced() {
        MessageListener listener = reader.getMessageListener(InterfacedListener.class);
        ListenerValidator validator = new ListenerValidator()
                .expectHandlers(1, Object.class);
        validator.check(listener);
    }  WIP */


    @Test
    public void testListenerWithInheritance() {
        MessageListener<MessageListener2> listener = reader.getMessageListener(MessageListener2.class);
        ListenerValidator validator = new ListenerValidator()
                .expectHandlers(2, String.class)
                .expectHandlers(2, Object.class)
                .expectHandlers(1, BufferedReader.class);
        validator.check(listener);
    }

    @Test
    public void testListenerWithInheritanceOverriding() {
        MessageListener<MessageListener3> listener = reader.getMessageListener(MessageListener3.class);

        ListenerValidator validator = new ListenerValidator()
                .expectHandlers(0, String.class)
                .expectHandlers(2, Object.class)
                .expectHandlers(0, BufferedReader.class);
        validator.check(listener);
    }

    // Define and assert expectations on handlers in a listener
    private class ListenerValidator {

        private Map<Class<?>, Integer> handlers = new HashMap<Class<?>, Integer>();

        public ListenerValidator expectHandlers(Integer count, Class<?> messageType){
            handlers.put(messageType, count);
            return this;
        }

        public void check(MessageListener listener){
            for(Map.Entry<Class<?>, Integer> expectedHandler: handlers.entrySet()){
                if(expectedHandler.getValue() > 0){
                    assertTrue(listener.handles(expectedHandler.getKey()));
                }
                else{
                    assertFalse(listener.handles(expectedHandler.getKey()));
                }
                assertEquals(expectedHandler.getValue(), listener.getHandlers(ForMessage(expectedHandler.getKey())).size());
            }
        }

    }


    // a simple event listener
    public class MessageListener1 {

        @Handler(rejectSubtypes = true)
        public void handleObject(Object o) {

        }

        @Handler
        public void handleAny(Object o) {

        }


        @Handler
        public void handleString(String s) {

        }

    }

    // the same handlers as its super class
    public class MessageListener2 extends MessageListener1 {

        // redefine handler implementation (not configuration)
        public void handleString(String s) {

        }

    }

    public class MessageListener3 extends MessageListener2 {

        // narrow the handler
        @Handler(rejectSubtypes = true)
        public void handleAny(Object o) {

        }

        @Handler(enabled = false)
        public void handleString(String s) {

        }

    }
}
