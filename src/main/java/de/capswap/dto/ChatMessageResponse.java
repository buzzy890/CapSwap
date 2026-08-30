package de.capswap.dto;

import de.capswap.entity.ChatMessage;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long senderCompanyId,
        String senderCompanyName,
        Long recipientCompanyId,
        String recipientCompanyName,
        Long listingId,
        String message,
        Boolean isRead,
        Instant sentAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        boolean hasListing = chatMessage.getListing() != null;
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getSenderCompany().getId(),
                chatMessage.getSenderCompany().getName(),
                chatMessage.getRecipientCompany().getId(),
                chatMessage.getRecipientCompany().getName(),
                hasListing ? chatMessage.getListing().getId() : null,
                chatMessage.getMessage(),
                chatMessage.getIsRead(),
                chatMessage.getSentAt()
        );
    }
}
