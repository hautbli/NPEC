package com.mogak.npec.comment.repository;

import com.mogak.npec.comment.domain.Comment;

import java.util.List;

public interface CommentQueryRepository {
    List<Comment> findParentsByBoardId(Long boardId, Long lastParentId);
}
