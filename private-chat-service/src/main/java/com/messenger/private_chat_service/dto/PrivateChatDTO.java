package com.messenger.private_chat_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateChatDTO {
    private int id;
    private int senderId;
    private String senderUsername;
    private String senderNickname;
    private int receiverId;
    private String receiverUsername;
    private String receiverNickname;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
