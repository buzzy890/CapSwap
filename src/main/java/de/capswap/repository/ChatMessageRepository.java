package de.capswap.repository;

import de.capswap.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"senderCompany", "recipientCompany", "listing", "listing.company", "listing.category", "listing.photos"})
    List<ChatMessage> findBySenderCompanyIdAndRecipientCompanyId(Long senderCompanyId, Long recipientCompanyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"senderCompany", "recipientCompany", "listing", "listing.company", "listing.category", "listing.photos"})
    @org.springframework.data.jpa.repository.Query("SELECT c FROM ChatMessage c WHERE (c.senderCompany.id = :companyAId AND c.recipientCompany.id = :companyBId) OR (c.senderCompany.id = :companyBId AND c.recipientCompany.id = :companyAId) ORDER BY c.sentAt ASC")
    List<ChatMessage> findConversation(@org.springframework.data.repository.query.Param("companyAId") Long companyAId, @org.springframework.data.repository.query.Param("companyBId") Long companyBId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"senderCompany", "recipientCompany", "listing", "listing.company", "listing.category", "listing.photos"})
    List<ChatMessage> findByListingId(Long listingId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"senderCompany", "recipientCompany", "listing", "listing.company", "listing.category", "listing.photos"})
    java.util.Optional<ChatMessage> findById(Long id);
}
