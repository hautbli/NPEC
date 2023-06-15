package com.mogak.npec.comment.application;

import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.domain.BoardSort;
import com.mogak.npec.board.dto.BoardSortNotFoundException;
import com.mogak.npec.board.exceptions.BoardCanNotModifyException;
import com.mogak.npec.board.exceptions.BoardNotFoundException;
import com.mogak.npec.board.repository.BoardRepository;
import com.mogak.npec.board.repository.BoardSortRepository;
import com.mogak.npec.comment.domain.Comment;
import com.mogak.npec.comment.dto.*;
import com.mogak.npec.comment.exception.CommentCanNotModifyException;
import com.mogak.npec.comment.exception.CommentDepthExceedException;
import com.mogak.npec.comment.exception.CommentNotFoundException;
import com.mogak.npec.comment.exception.InvalidCommentWriterException;
import com.mogak.npec.comment.repository.CommentRepository;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.exception.MemberNotFoundException;
import com.mogak.npec.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BoardSortRepository boardSortRepository;

    public CommentService(CommentRepository commentRepository, BoardRepository boardRepository, MemberRepository memberRepository, BoardSortRepository boardSortRepository) {
        this.commentRepository = commentRepository;
        this.boardRepository = boardRepository;
        this.memberRepository = memberRepository;
        this.boardSortRepository = boardSortRepository;
    }

    @Transactional
    public void createComment(CreateCommentServiceDto dto) {
        Member member = findMember(dto.memberId());
        Board board = findBoard(dto.boardId());

        verifyBoard(board);

        Comment comment = Comment.parent(member, board, dto.content(), false);
        commentRepository.save(comment);

        boardSortRepository.updateCommentCount(board.getId());

    }

    @Transactional
    public void createReply(CreateReplyServiceDto dto) {
        Member member = findMember(dto.memberId());
        Comment parent = findComment(dto.commentId());
        Board board = parent.getBoard();

        verifyComment(parent);
        verifyParent(parent);
        verifyBoard(board);

        Comment child = Comment.child(member, board, parent, dto.content(), false);
        commentRepository.save(child);

        boardSortRepository.updateCommentCount(board.getId());
    }

    private void verifyParent(Comment parent) {
        if (!parent.isParent()) {
            throw new CommentDepthExceedException("댓글의 Depth를 초과하였습니다.");
        }
    }

    private void verifyBoard(Board findBoard) {
        if (findBoard.getIsDeleted()) {
            throw new BoardCanNotModifyException("삭제된 게시물 입니다.");
        }
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("게시글을 찾을 수 없습니다."));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));
    }

    private BoardSort findBoardSort(Long boardId) {
        return boardSortRepository.findByBoardId(boardId)
                .orElseThrow(() -> new BoardSortNotFoundException("board sort 가 저장되어 있지 않습니다."));
    }

    private void verifyComment(Comment findComment) {
        if (findComment.isDeleted()) {
            throw new CommentCanNotModifyException("삭제된 댓글 입니다.");
        }
    }

    @Transactional(readOnly = true)
    public CommentsResponse findComments(FindCommentsServiceDto dto) {
        Board board = findBoard(dto.boardId());

        List<Comment> parents = commentRepository.findParentsByBoardId(board.getId());

        List<CommentResponse> commentResponses = parents.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return new CommentsResponse(commentResponses);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        List<ReplyResponse> replies = getReplyResponses(comment);

        return CommentResponse.of(comment, replies);
    }

    private List<ReplyResponse> getReplyResponses(Comment comment) {
        List<Comment> children = comment.getChildren();
        if (children.isEmpty()) {
            return new ArrayList<>();
        }
        return children.stream()
                .map(this::toReplyResponse)
                .collect(Collectors.toList());
    }

    private ReplyResponse toReplyResponse(Comment reply) {
        return ReplyResponse.of(reply);
    }

    @Transactional
    public CommentModifyResponse modifyComment(ModifyCommentServiceDto dto) {
        Comment comment = findComment(dto.commentId());
        Member writer = comment.getMember();

        verifyComment(comment);
        verifyWriter(writer, dto.memberId());

        comment.modifyContent(dto.content());

        return new CommentModifyResponse(comment.getId(), writer.getNickname(), dto.content(),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }

    private void verifyWriter(Member writer, Long memberId) {
        if (!writer.getId().equals(memberId)) {
            throw new InvalidCommentWriterException("댓글 작성자가 아닙니다.");
        }
    }

    @Transactional
    public void deleteComment(DeleteCommentServiceDto dto) {
        Comment comment = findComment(dto.commentId());
        Member writer = comment.getMember();

        verifyComment(comment);
        verifyWriter(writer, dto.memberId());

        comment.deleteComment();
    }
}
