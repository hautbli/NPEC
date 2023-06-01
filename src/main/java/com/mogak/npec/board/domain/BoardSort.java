package com.mogak.npec.board.domain;

import com.mogak.npec.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "board_sorts", indexes = @Index(name = "idx_boards_id", columnList = "boards_id"))
public class BoardSort extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boards_id")
    private Board board;

    @JoinColumn(name = "like_count")
    private Long likeCount;
    @JoinColumn(name = "view_count")
    private Long viewCount;
    @JoinColumn(name = "comment_count")
    private Long commentCount;

    public BoardSort(Board board, Long likeCount, Long viewCount, Long commentCount) {
        this.board = board;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
    }

    public BoardSort() {
    }

    public static BoardSort of(Board savedBoard) {
        return new BoardSort(savedBoard, 0L, 0L, 0L);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

}
