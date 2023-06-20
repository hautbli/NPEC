package com.mogak.npec.comment.dto;

public record FindCommentsServiceDto(long boardId, Long lastCommentId, int size) {
}
