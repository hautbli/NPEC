package com.mogak.npec.comment.controller;

import com.mogak.npec.auth.annotation.ValidToken;
import com.mogak.npec.comment.application.CommentService;
import com.mogak.npec.comment.dto.CommentCreateRequest;
import com.mogak.npec.comment.dto.CommentModifyRequest;
import com.mogak.npec.comment.dto.CommentModifyResponse;
import com.mogak.npec.comment.dto.CommentsResponse;
import com.mogak.npec.comment.dto.CreateCommentServiceDto;
import com.mogak.npec.comment.dto.CreateReplyServiceDto;
import com.mogak.npec.comment.dto.DeleteCommentServiceDto;
import com.mogak.npec.comment.dto.FindCommentsServiceDto;
import com.mogak.npec.comment.dto.ModifyCommentServiceDto;
import com.mogak.npec.comment.dto.PagingCommentRequest;
import com.mogak.npec.comment.dto.ReplyCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/boards/{boardId}/comments")
    public ResponseEntity<Void> createComment(@PathVariable Long boardId, @ValidToken Long memberId,
                                              @Valid @RequestBody CommentCreateRequest request) {

        commentService.createComment(new CreateCommentServiceDto(memberId, boardId, request.getContent()));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<Void> createReply(@PathVariable Long commentId, @ValidToken Long memberId,
                                            @Valid @RequestBody ReplyCreateRequest request) {
        commentService.createReply(new CreateReplyServiceDto(memberId, commentId, request.getContent()));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/boards/{boardId}/comments")
    public ResponseEntity<CommentsResponse> findComments(@PathVariable long boardId,
                                                         PagingCommentRequest request) {
        FindCommentsServiceDto dto = new FindCommentsServiceDto(boardId, request.getLastCommentId(), request.getSize());
        CommentsResponse response = commentService.findComments(dto);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentModifyResponse> modifyComment(@PathVariable Long commentId, @ValidToken Long memberId,
                                                               @RequestBody CommentModifyRequest request) {
        CommentModifyResponse response = commentService.modifyComment(new ModifyCommentServiceDto(memberId, commentId,
                request.getContent()));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, @ValidToken Long memberId) {

        commentService.deleteComment(new DeleteCommentServiceDto(memberId, commentId));

        return ResponseEntity.noContent().build();
    }
}
