package de.capswap.socket;

import de.capswap.entity.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewChatMessageEvent extends ApplicationEvent {
    private final ChatMessage chatMessage;

    public NewChatMessageEvent(Object source, ChatMessage chatMessage) {
        super(source);
        this.chatMessage = chatMessage;
    }
}
