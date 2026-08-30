package de.capswap.repository;

import de.capswap.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    @Query("SELECT n FROM Notification n JOIN FETCH n.company LEFT JOIN FETCH n.listing " +
            "WHERE n.company.id = :companyId ORDER BY n.createdAt DESC")
    List<Notification> findByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.company LEFT JOIN FETCH n.listing " +
            "WHERE n.company.id = :companyId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findByCompanyIdAndIsReadFalseOrderByCreatedAtDesc(@Param("companyId") Long companyId);
}
