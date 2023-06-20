package com.mogak.npec.comment.application;

import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.domain.BoardSort;
import com.mogak.npec.board.exceptions.BoardCanNotModifyException;
import com.mogak.npec.board.exceptions.BoardNotFoundException;
import com.mogak.npec.board.repository.BoardRepository;
import com.mogak.npec.board.repository.BoardSortRepository;
import com.mogak.npec.comment.domain.Comment;
import com.mogak.npec.comment.dto.CommentModifyResponse;
import com.mogak.npec.comment.dto.CommentsResponse;
import com.mogak.npec.comment.dto.CreateCommentServiceDto;
import com.mogak.npec.comment.dto.CreateReplyServiceDto;
import com.mogak.npec.comment.dto.DeleteCommentServiceDto;
import com.mogak.npec.comment.dto.FindCommentsServiceDto;
import com.mogak.npec.comment.dto.ModifyCommentServiceDto;
import com.mogak.npec.comment.exception.CommentCanNotModifyException;
import com.mogak.npec.comment.exception.CommentDepthExceedException;
import com.mogak.npec.comment.exception.InvalidCommentWriterException;
import com.mogak.npec.comment.repository.CommentRepository;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.exception.MemberNotFoundException;
import com.mogak.npec.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@ActiveProfiles("test")
public class CommentServiceTest {
    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardSortRepository boardSortRepository;

    private Member member;
    private Board board;

    @BeforeEach
    void setUp() {
        member = new Member("tester", "test@example.com", "1234ab1!");
        board = new Board(member, "제목", "내용", false);
        BoardSort boardSort = new BoardSort(board, 0L, 0L, 0L);

        memberRepository.save(member);
        boardRepository.save(board);
        boardSortRepository.save(boardSort);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        boardSortRepository.deleteAll();
        boardRepository.deleteAll();
        memberRepository.deleteAll();
    }


    @DisplayName("게시물에 댓글이 생성되며, 게시물 댓글수가 1 증가한다.")
    @Test
    void createCommentSuccess() {
        // given
        String content = "댓글내용";
        CreateCommentServiceDto dto = new CreateCommentServiceDto(member.getId(), board.getId(), content);

        // when
        commentService.createComment(dto);

        // then
        List<Comment> comments = commentRepository.findAll();
        BoardSort boardSort = boardSortRepository.findByBoardId(board.getId()).get();

        assertAll(
                () -> assertThat(comments.size()).isEqualTo(1),
                () -> assertThat(comments.get(0).getBoard().getId()).isEqualTo(dto.boardId()),
                () -> assertThat(comments.get(0).getMember().getId()).isEqualTo(dto.memberId()),
                () -> assertThat(boardSort.getCommentCount()).isEqualTo(1)
        );
    }

    @DisplayName("사용자가 존재하지 않으면 댓글을 작성할 수 없다.")
    @Test
    void createCommentFail1() {
        // given
        String content = "댓글내용";
        CreateCommentServiceDto dto = new CreateCommentServiceDto(999L, board.getId(), content);

        // when then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @DisplayName("게시물이 존재하지 않으면 댓글을 작성할 수 없다.")
    @Test
    void createCommentFail2() {
        // given
        String content = "댓글내용";
        CreateCommentServiceDto dto = new CreateCommentServiceDto(member.getId(), 999L, content);

        // when then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(BoardNotFoundException.class);
    }

    @DisplayName("삭제된 게시물엔 댓글을 작성할 수 없다.")
    @Test
    void createCommentFail3() {
        // given
        String content = "댓글내용";
        Board deletedBoard = new Board(member, "제목", "내용", true);
        boardRepository.save(deletedBoard);

        CreateCommentServiceDto dto = new CreateCommentServiceDto(member.getId(), deletedBoard.getId(), content);

        // when then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(BoardCanNotModifyException.class);
    }

    @DisplayName("댓글에 대댓글이 생성되며, 해당 게시물의 댓글 수가 1 증가한다.")
    @Test
    void createReplySuccess() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", false);
        commentRepository.save(comment);
        CreateReplyServiceDto dto = new CreateReplyServiceDto(member.getId(), comment.getId(), "대댓글내용");

        // when
        commentService.createReply(dto);

        // then
        List<Comment> comments = commentRepository.findAll();
        BoardSort boardSort = boardSortRepository.findByBoardId(board.getId()).get();

        assertAll(
                () -> assertThat(comments.size()).isEqualTo(2),
                () -> assertThat(comments.get(0).isParent()).isTrue(),
                () -> assertThat(comments.get(1).getParent().getId()).isEqualTo(comments.get(0).getId()),
                () -> assertThat(boardSort.getCommentCount()).isEqualTo(1)
        );
    }

    @DisplayName("대댓글엔 다시 대댓글을 생성할 수 없다. (depth: 1)")
    @Test
    void createReplyFail1() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", false);
        commentRepository.save(comment);

        CreateReplyServiceDto dto = new CreateReplyServiceDto(member.getId(), comment.getId(), "대댓글내용");
        commentService.createReply(dto);

        List<Comment> comments = commentRepository.findAll();
        Comment child = comments.get(1);

        // when then
        CreateReplyServiceDto dto2 = new CreateReplyServiceDto(member.getId(), child.getId(), "대댓글내용2");
        assertThatThrownBy(() -> commentService.createReply(dto2))
                .isInstanceOf(CommentDepthExceedException.class);

    }

    @DisplayName("삭제된 댓글엔 대댓글을 생성할 수 없다.")
    @Test
    void createReplyFail2() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", true);
        commentRepository.save(comment);
        CreateReplyServiceDto dto = new CreateReplyServiceDto(member.getId(), comment.getId(), "대댓글내용");

        // when then
        assertThatThrownBy(() -> commentService.createReply(dto))
                .isInstanceOf(CommentCanNotModifyException.class);

    }

    @DisplayName("댓글을 가져오면 대댓글도 같이 가져온다.")
    @Test
    void findCommentsSuccess() {
        // given
        Comment parent = Comment.parent(member, board, "댓글내용", false);
        Comment child = Comment.child(member, board, parent, "대댓글내용", false);

        commentRepository.save(parent);
        commentRepository.save(child);
        FindCommentsServiceDto dto = new FindCommentsServiceDto(board.getId(), 0L, 10);

        // when
        CommentsResponse comments = commentService.findComments(dto);

        // then
        assertAll(
                () -> assertThat(comments.getCount()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(0).getWriter()).isEqualTo(member.getNickname()),
                () -> assertThat(comments.getComments().get(0).getContent()).isEqualTo(parent.getContent()),
                () -> assertThat(comments.getComments().get(0).getReplies().size()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(0).getReplies().get(0).getContent()).isEqualTo(child.getContent())
        );
    }

    @DisplayName("삭제된 댓글 및 대댓글은 내용을 담지 않는다.")
    @Test
    void findCommentsWithoutDeletedContent() {
        // given
        Comment parent = Comment.parent(member, board, "댓글내용", true);
        Comment child = Comment.child(member, board, parent, "대댓글내용", true);

        commentRepository.save(parent);
        commentRepository.save(child);
        FindCommentsServiceDto dto = new FindCommentsServiceDto(board.getId(), 0L, 10);

        // when
        CommentsResponse comments = commentService.findComments(dto);

        // then
        assertAll(
                () -> assertThat(comments.getCount()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(0).getWriter()).isNull(),
                () -> assertThat(comments.getComments().get(0).getContent()).isNull(),
                () -> assertThat(comments.getComments().get(0).getReplies().size()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(0).getReplies().get(0).getWriter()).isNull(),
                () -> assertThat(comments.getComments().get(0).getReplies().get(0).getContent()).isNull()
        );
    }

    @DisplayName("댓글이 없을 땐 response 값이 비어있고, size는 0이다.")
    @Test
    void responseWhenNoComments() {
        // given
        FindCommentsServiceDto dto = new FindCommentsServiceDto(board.getId(), 0L, 10);

        // when
        CommentsResponse comments = commentService.findComments(dto);

        // then
        assertThat(comments.getCount()).isEqualTo(0);
        assertThat(comments.getComments()).isEmpty();
    }

    @DisplayName("lastCommentId 가 주어지면 그 다음 id 댓글들을 리턴한다.")
    @Test
    void findCommentWithLastParentId() {
        // given
        saveCommentAndReply(member, board);
        saveCommentAndReply(member, board);
        Long lastId = saveCommentAndReply(member, board).getId();

        Comment comment1 = saveCommentAndReply(member, board);
        Comment comment2 = saveCommentAndReply(member, board);
        Comment comment3 = saveCommentAndReply(member, board);

        FindCommentsServiceDto dto = new FindCommentsServiceDto(board.getId(), lastId, 10);

        // when
        CommentsResponse comments = commentService.findComments(dto);

        // then
        assertAll(
                () -> assertThat(comments.getCount()).isEqualTo(3),
                () -> assertThat(comments.getComments().get(0).getId()).isEqualTo(comment1.getId()),
                () -> assertThat(comments.getComments().get(0).getReplies().size()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(1).getId()).isEqualTo(comment2.getId()),
                () -> assertThat(comments.getComments().get(1).getReplies().size()).isEqualTo(1),
                () -> assertThat(comments.getComments().get(2).getId()).isEqualTo(comment3.getId()),
                () -> assertThat(comments.getComments().get(2).getReplies().size()).isEqualTo(1)
        );
    }

    private Comment saveCommentAndReply(Member member, Board board) {
        Comment parent = Comment.parent(member, board, "댓글내용", false);
        Comment child = Comment.child(member, board, parent, "대댓글내용", false);

        commentRepository.save(parent);
        commentRepository.save(child);
        return parent;
    }

    @DisplayName("작성자의 수정 요청일 경우 댓글이 수정된다.")
    @Test
    void modifyCommentSuccess() {
        // given
        String modifyContent = "댓글수정내용";
        Comment comment = Comment.parent(member, board, "댓글내용", false);
        commentRepository.save(comment);

        ModifyCommentServiceDto dto = new ModifyCommentServiceDto(member.getId(), comment.getId(), modifyContent);
        // when
        CommentModifyResponse response = commentService.modifyComment(dto);

        // then
        assertThat(response.getContent()).isEqualTo(modifyContent);
        assertThat(response.getWriter()).isEqualTo(member.getNickname());
    }

    @DisplayName("작성자가 아닌 경우 수정 요청이 실패한다.")
    @Test
    void modifyCommentFail1() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", false);
        commentRepository.save(comment);

        Member member2 = new Member("tester2", "test2@example.com", "1234ab1!");
        memberRepository.save(member2);

        ModifyCommentServiceDto dto = new ModifyCommentServiceDto(member2.getId(), comment.getId(), "댓글수정내용");

        // when then
        assertThatThrownBy(() -> commentService.modifyComment(dto))
                .isInstanceOf(InvalidCommentWriterException.class);
    }

    @DisplayName("작성자의 요청인 경우 댓글이 삭제 상태가 된다.")
    @Test
    void softDeleteCommentSuccess() {
        // given
        Comment parent = Comment.parent(member, board, "댓글내용", false);

        commentRepository.save(parent);

        DeleteCommentServiceDto dto = new DeleteCommentServiceDto(member.getId(), parent.getId());
        // when
        commentService.deleteComment(dto);

        // then
        List<Comment> comments = commentRepository.findAll();
        assertThat(comments.size()).isEqualTo(1);
        assertThat(comments.get(0).isDeleted()).isTrue();
    }

    @DisplayName("작성자가 아니면 삭제 요청이 실패한다.")
    @Test
    void deleteCommentFail1() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", false);
        commentRepository.save(comment);

        Member member2 = new Member("tester2", "test2@example.com", "1234ab1!");
        memberRepository.save(member2);

        DeleteCommentServiceDto dto = new DeleteCommentServiceDto(member2.getId(), comment.getId());
        // when then
        assertThatThrownBy(() -> commentService.deleteComment(dto))
                .isInstanceOf(InvalidCommentWriterException.class);
    }

    @DisplayName("이미 삭제된 댓글에 삭제 요청이 온 경우 실패한다.")
    @Test
    void deleteCommentFail2() {
        // given
        Comment comment = Comment.parent(member, board, "댓글내용", true);
        commentRepository.save(comment);

        DeleteCommentServiceDto dto = new DeleteCommentServiceDto(member.getId(), comment.getId());

        // when then
        assertThatThrownBy(() -> commentService.deleteComment(dto))
                .isInstanceOf(CommentCanNotModifyException.class);
    }
}
