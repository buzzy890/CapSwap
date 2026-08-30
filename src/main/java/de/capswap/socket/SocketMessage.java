package de.capswap.socket;

import lombok.Data;

@Data
public class SocketMessage {
    private String type; // "AUTH" or "MESSAGE"
    private Long companyId; // used for AUTH
    private Long senderId;
    private Long recipientId;
    private Long listingId;
    private Long categoryId;
    private String message;
}
