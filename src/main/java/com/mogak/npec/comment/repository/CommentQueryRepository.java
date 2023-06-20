package com.mogak.npec.comment.repository;

import com.mogak.npec.comment.domain.Comment;

import java.util.List;

public interface CommentQueryRepository {
    List<Comment> findPagedParentsByBoardId(long boardId, Long lastParentId, int size);
}
