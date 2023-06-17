package com.mogak.npec.comment.repository;

import com.mogak.npec.comment.domain.Comment;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

import static com.mogak.npec.comment.domain.QComment.comment;

public class CommentQueryRepositoryImpl implements CommentQueryRepository {
    private final JPAQueryFactory queryFactory;

    public CommentQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Comment> findParentsByBoardId(Long boardId, Long lastCommentId) {
        return queryFactory
                .selectFrom(comment)
                .where(
                        gtCommentId(lastCommentId),
                        comment.board.id.eq(boardId),
                        comment.parent.isNull()
                )
                .orderBy(comment.id.asc())
                .limit(10)
                .fetch();
    }

    private BooleanExpression gtCommentId(Long commentId) {
        if (commentId == null) {
            return null;
        }
        return comment.id.gt(commentId);
    }
}
