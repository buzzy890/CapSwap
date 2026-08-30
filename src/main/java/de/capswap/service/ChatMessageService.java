package de.capswap.service;

import de.capswap.entity.ChatMessage;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.repository.ChatMessageRepository;
import de.capswap.socket.NewChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ChatMessage> getDirectMessages(Long companyAId, Long companyBId) {
        return chatMessageRepository.findConversation(companyAId, companyBId);
    }

    public List<ChatMessage> getMessagesByListing(Long listingId) {
        return chatMessageRepository.findByListingId(listingId);
    }

    @Transactional
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        ChatMessage savedMessage = chatMessageRepository.saveAndFlush(chatMessage);
        
        savedMessage = chatMessageRepository.findById(savedMessage.getId()).orElse(savedMessage);
        eventPublisher.publishEvent(new NewChatMessageEvent(this, savedMessage));
        return savedMessage;
    }

    @Transactional
    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            chatMessageRepository.save(msg);
        });
    }
}
