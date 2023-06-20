package com.mogak.npec.comment.controller;

import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.repository.BoardRepository;
import com.mogak.npec.comment.domain.Comment;
import com.mogak.npec.comment.repository.CommentRepository;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.repository.MemberRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommentControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private MemberRepository memberRepository;
    private Board board;
    private Member member;
    private Comment comment1;
    private Comment comment2;


    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        member = memberRepository.save(new Member("kim", "a@a.com", "1234"));
        board = boardRepository.save(new Board(member, "제목", "내용"));
    }

    private void createCommentAndReply(int size) {
        for (int i = 0; i < size; i++) {
            saveCommentAndReply(member, board);
        }
    }

    private Comment saveCommentAndReply(Member member, Board board) {
        Comment parent = Comment.parent(member, board, "댓글내용", false);
        Comment child = Comment.child(member, board, parent, "대댓글내용", false);

        commentRepository.save(parent);
        commentRepository.save(child);
        return parent;
    }

    @DisplayName("요청한 페이징 사이즈만큼 댓글 목록을 리턴한다.")
    @Test
    void getCommentsWithSize() {
        createCommentAndReply(4);

        RestAssured.given().log().all()
                .get("/boards/" + board.getId() + "/comments?size=3")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .assertThat().body("comments.size()", equalTo(3))
        ;
    }

    @DisplayName("요청한 페이징 사이즈를 요청하지 않은 경우 기본값 사이즈로 댓글 목록을 리턴한다.")
    @Test
    void getCommentsWithoutSize() {
        createCommentAndReply(11);

        RestAssured.given().log().all()
                .get("/boards/" + board.getId() + "/comments")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .assertThat().body("comments.size()", equalTo(10));
    }

    @DisplayName("페이징 사이즈가 사이즈 최댓값보다 큰 경우 최대값 사이즈로 댓글들을 리턴한다.")
    @Test
    void getCommentsWithExceedMaximumSize() {
        //given
        createCommentAndReply(31);

        RestAssured.given().log().all()
                .get("/boards/" + board.getId() + "/comments?size=40")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .assertThat().body("comments.size()", equalTo(30));
    }

    @DisplayName("parentId 가 없는 경우 가장 오래된 댓글부터 리턴한다.")
    @Test
    void getCommentsWithoutParentId() {
        createCommentAndReply(3);
        List<Comment> comments = commentRepository.findPagedParentsByBoardId(board.getId(), 0L, 2);

        RestAssured.given()
                .log().all()
                .get("/boards/" + board.getId() + "/comments?size=2")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .assertThat()
                .body("comments.id", contains(comments.get(0).getId().intValue(), comments.get(1).getId().intValue()));
    }
}
