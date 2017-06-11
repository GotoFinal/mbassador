package net.engio.mbassy.listener;

/**
 * Represent something that can be cancelled, like some events.
 */
public interface Cancellable {
    /**
     * Returns true if this is already cancelled.
     * @return true if this is already cancelled.
     */
    boolean isCancelled();

    /**
     * Set if this should be cancelled.
     * @param cancelled new cancelled status.
     */
    void setCancelled(boolean cancelled);

    /**
     * Change status to cancelled, this same as setCancelled(true)
     */
    default void cancel()
    {
        this.setCancelled(true);
    }
}
