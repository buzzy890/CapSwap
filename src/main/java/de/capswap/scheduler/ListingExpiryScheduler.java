package de.capswap.scheduler;

import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.repository.ListingRepository;
import de.capswap.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingExpiryScheduler { // Scheduler-Klasse, die regelmäßig ablaufende Listings überprüft und entsprechende Aktionen ausführt (Vorab-Benachrichtigung und automatische Löschung).

    private static final int WARNING_WINDOW_DAYS = 30;

    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void warnAboutExpiringListings() {
        Instant warningThreshold = Instant.now().plus(WARNING_WINDOW_DAYS, ChronoUnit.DAYS);
        List<Listing> expiringSoon = listingRepository
                .findByStatusAndExpiresAtBeforeAndExpiryWarningSentFalse(ListingStatus.ACTIVE, warningThreshold);

        for (Listing listing : expiringSoon) {
            notificationService.createNotification(listing.getCompany(), listing,
                    "Ihr Angebot \"" + listing.getTitle() + "\" läuft am " + listing.getExpiresAt()
                            + " ab und wird danach automatisch gelöscht (FA 12).");
            listing.setExpiryWarningSent(true);
            listingRepository.save(listing);
        }

        if (!expiringSoon.isEmpty()) {
            log.info("{} Angebote stehen vor dem Ablauf, Vorab-Benachrichtigung versendet (FA 12, Thread={}).",
                    expiringSoon.size(), Thread.currentThread().getName());
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredListings() {
        List<Listing> expired = listingRepository
                .findByStatusNotAndExpiresAtBefore(ListingStatus.DELETED, Instant.now());

        for (Listing listing : expired) {
            listing.setStatus(ListingStatus.DELETED);
            listingRepository.save(listing);
        }

        if (!expired.isEmpty()) {
            log.info("{} abgelaufene Angebote automatisch gelöscht (FA 11, Thread={}).",
                    expired.size(), Thread.currentThread().getName());
        }
    }
}
