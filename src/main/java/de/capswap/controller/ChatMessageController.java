package de.capswap.controller;

import de.capswap.dto.ChatMessageResponse;
import de.capswap.entity.ChatMessage;
import de.capswap.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints for Chat Messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final de.capswap.repository.CompanyRepository companyRepository;
    private final de.capswap.repository.ListingRepository listingRepository;

    @GetMapping
    @Operation(summary = "Get chat messages", description = "Fetch conversation history or messages for a listing")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) Long listingId) {

        if (senderId != null && recipientId != null) {
            return ResponseEntity.ok(chatMessageService.getDirectMessages(senderId, recipientId));
        }
        if (listingId != null) {
            return ResponseEntity.ok(chatMessageService.getMessagesByListing(listingId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping
    @Operation(summary = "Send a new chat message", description = "Sends a chat message via REST and broadcasts to Socket")
    public ResponseEntity<ChatMessage> sendMessage(@Valid @RequestBody de.capswap.dto.ChatMessageRequest request) {
        
        de.capswap.entity.Company sender = companyRepository.findById(request.getSenderCompanyId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender not found"));
                
        de.capswap.entity.Company recipient = companyRepository.findById(request.getRecipientCompanyId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient not found"));
        
        ChatMessage chatMessage = ChatMessage.builder()
                .senderCompany(sender)
                .recipientCompany(recipient)
                .message(request.getMessage())
                .build();
                
        if (request.getListingId() != null) {
            de.capswap.entity.Listing listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing not found"));
            chatMessage.setListing(listing);
        }

        ChatMessage created = chatMessageService.sendMessage(chatMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark message as read", description = "Marks a specific message as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        chatMessageService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
