package net.engio.mbassy;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.common.ListenerFactory;
import net.engio.mbassy.common.MessageBusTest;
import net.engio.mbassy.common.TestUtil;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.messages.SubTestMessage;
import net.engio.mbassy.messages.TestMessage;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testing of filter functionality
 *
 * @author bennidi
 *         Date: 11/26/12
 */
public class CancellableTest extends MessageBusTest {

    private static final AtomicInteger HandleAllCounter = new AtomicInteger(0);
    private static final AtomicInteger HandleWithoutCancelled = new AtomicInteger(0);
    private static final AtomicInteger HandleWithoutCancelledLaterConuter = new AtomicInteger(0);

    @Test
    public void testCancellable() throws Exception {
        HandleAllCounter.set(0);
        HandleWithoutCancelled.set(0);
        HandleWithoutCancelledLaterConuter.set(0);

        MBassador bus = createBus(SyncAsync());
        ListenerFactory listenerFactory = new ListenerFactory()
                .create(100, FilteredMessageListener.class);

        List<Object> listeners = listenerFactory.getAll();

        // this will subscribe the listeners concurrently to the bus
        TestUtil.setup(bus, listeners, 10);

        bus.post(new TestMessage()).now();
        bus.post(new SubTestMessage()).now();

        assertEquals(200, HandleAllCounter.get());
        assertEquals(200, HandleWithoutCancelled.get());
        assertEquals(0, HandleWithoutCancelledLaterConuter.get());
    }

    public static class FilteredMessageListener{
        @Handler(ignoreCancelled = true, priority = 100)
        public void handleWithoutCancelled(TestMessage message){
            message.counter.incrementAndGet();
            message.setCancelled(true);
            HandleWithoutCancelled.incrementAndGet();
        }
        @Handler(ignoreCancelled = false, priority = 50)
        public void handleAll(TestMessage message){
            message.counter.incrementAndGet();
            HandleAllCounter.incrementAndGet();
        }
        @Handler(ignoreCancelled = true, priority = 50)
        public void handleWithoutCancelledLater(TestMessage message){
            message.counter.incrementAndGet();
            HandleWithoutCancelledLaterConuter.incrementAndGet();
        }

    }
}
