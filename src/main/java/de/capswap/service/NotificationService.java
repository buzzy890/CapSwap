package de.capswap.service;

import de.capswap.dto.NotificationResponse;
import de.capswap.entity.CategorySubscription;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.Notification;
import de.capswap.repository.CategorySubscriptionRepository;
import de.capswap.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CategorySubscriptionRepository subscriptionRepository;

    public List<NotificationResponse> getNotificationsForCompany(Long companyId) {
        return notificationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(Long companyId) {
        return notificationRepository.findByCompanyIdAndIsReadFalseOrderByCreatedAtDesc(companyId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public Notification createNotification(Company company, Listing listing, String message) {
        Notification notification = Notification.builder()
                .company(company)
                .listing(listing)
                .message(message)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Async
    @Transactional
    public void notifySubscribersOfNewListing(Listing listing) {
        List<CategorySubscription> subscriptions =
                subscriptionRepository.findByCategoryId(listing.getCategory().getId());

        int notified = 0;
        for (CategorySubscription subscription : subscriptions) {
            Company subscriber = subscription.getCompany();
            if (subscriber.getId().equals(listing.getCompany().getId())) {
                continue;
            }
            Notification notification = Notification.builder()
                    .company(subscriber)
                    .listing(listing)
                    .message("Neues Angebot in Ihrer abonnierten Kategorie \"" + subscription.getCategory().getName()
                            + "\": " + listing.getTitle())
                    .build();
            notificationRepository.save(notification);
            notified++;
        }
        log.info("Angebot {} mit {} Abonnenten abgeglichen, {} benachrichtigt (Thread={}, NFA 4).",
                listing.getId(), subscriptions.size(), notified, Thread.currentThread().getName());
    }
}
