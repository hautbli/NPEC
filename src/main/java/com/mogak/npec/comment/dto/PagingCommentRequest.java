package com.mogak.npec.comment.dto;


import lombok.Getter;

@Getter
public class PagingCommentRequest {
    private static final int DEFAULT_PAGING_SIZE = 10;
    private static final int MAX_PAGING_SIZE = 30;

    private Long lastCommentId;
    private Integer size;

    public PagingCommentRequest(Long lastCommentId, Integer size) {
        this.lastCommentId = lastCommentId;
        this.size = size;
    }

    public int getSize() {
        if (size == null) {
            return DEFAULT_PAGING_SIZE;
        }
        if (size > MAX_PAGING_SIZE) {
            return MAX_PAGING_SIZE;
        }
        if (size <= 0) {
            return DEFAULT_PAGING_SIZE;
        }
        return size;
    }
}
