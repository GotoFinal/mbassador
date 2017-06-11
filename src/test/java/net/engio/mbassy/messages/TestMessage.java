package net.engio.mbassy.messages;

import net.engio.mbassy.listener.Cancellable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
* @author bennidi
* Date: 11/22/12
*/
public class TestMessage implements Cancellable {

    public AtomicInteger counter = new AtomicInteger();

    private volatile boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
