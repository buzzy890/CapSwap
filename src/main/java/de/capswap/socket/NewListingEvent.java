package de.capswap.socket;

import de.capswap.entity.Listing;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewListingEvent extends ApplicationEvent {
    private final Listing listing;

    public NewListingEvent(Object source, Listing listing) {
        super(source);
        this.listing = listing;
    }
}
