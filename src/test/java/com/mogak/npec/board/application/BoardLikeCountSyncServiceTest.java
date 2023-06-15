package com.mogak.npec.board.application;

import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.domain.BoardLike;
import com.mogak.npec.board.domain.BoardSort;
import com.mogak.npec.board.repository.BoardLikeRepository;
import com.mogak.npec.board.repository.BoardRepository;
import com.mogak.npec.board.repository.BoardSortRepository;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class BoardLikeCountSyncServiceTest {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private BoardSortRepository boardSortRepository;
    @Autowired
    private BoardLikeRepository boardLikeRepository;
    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardLikeCountSyncService boardLikeCountSyncService;

    private Board board;
    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member("kim", "user@npec.om", "1234"));
        board = boardRepository.save(new Board(member, "제목1", "내용1"));
        boardSortRepository.save(new BoardSort(board, 0L, 0L, 0L));
    }

    @AfterEach
    void tearDown() {
        boardSortRepository.deleteAll();
        boardLikeRepository.deleteAll();
        boardRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("주어진 boardId 의 likeCount 값을 업데이트하여 싱크를 맞춘다.")
    @Test
    void updateBoardSort() {
        // given
        List<BoardLikeModifiedMessage> messages = List.of(new BoardLikeModifiedMessage(board.getId()));
        boardLikeRepository.save(new BoardLike(member, board));

        // when
        boardLikeCountSyncService.updateBoardSortsLikeCount(messages);

        // then
        BoardSort boardSort = boardSortRepository.findByBoardId(board.getId()).get();
        Assertions.assertThat(boardSort.getLikeCount()).isEqualTo(1L);
    }
}
