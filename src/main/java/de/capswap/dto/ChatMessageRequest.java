package de.capswap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ChatMessageRequest {
    
    @NotNull(message = "Sender ID is required")
    @Schema(description = "ID of the company sending the message", example = "1")
    private Long senderCompanyId;

    @NotNull(message = "Recipient ID is required")
    @Schema(description = "ID of the company receiving the message", example = "2")
    private Long recipientCompanyId;

    @Schema(description = "Optional ID of a related listing", example = "5", nullable = true)
    private Long listingId;

    @NotBlank(message = "Message cannot be empty")
    @Schema(description = "The chat message text", example = "Hello! I am interested in your listing.")
    private String message;
}
