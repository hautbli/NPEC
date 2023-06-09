package com.mogak.npec.comment.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentModifyResponse {
    private Long id;
    private String writer;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentModifyResponse(Long id, String writer, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.writer = writer;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
