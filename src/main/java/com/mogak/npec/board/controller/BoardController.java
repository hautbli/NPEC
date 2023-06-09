package com.mogak.npec.board.controller;

import com.mogak.npec.auth.annotation.ValidToken;
import com.mogak.npec.board.application.BoardService;
import com.mogak.npec.board.dto.BoardCreateRequest;
import com.mogak.npec.board.dto.BoardGetResponse;
import com.mogak.npec.board.dto.BoardImageResponse;
import com.mogak.npec.board.dto.BoardListResponse;
import com.mogak.npec.board.dto.BoardUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/boards")
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PostMapping
    public ResponseEntity<Void> createBoard(@ValidToken Long memberId,
                                            @RequestBody @Valid BoardCreateRequest request) {
        Long boardId = boardService.createBoard(memberId, request);
        return ResponseEntity.created(URI.create("/boards/" + boardId)).build();
    }

    @GetMapping
    public ResponseEntity<BoardListResponse> getBoards(@RequestParam("page") int page, @RequestParam("sort") SortType sortType) {
        BoardListResponse response = boardService.getBoards(page, sortType);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{boardId}")
    public ResponseEntity<BoardGetResponse> getBoard(@PathVariable Long boardId) {
        BoardGetResponse response = boardService.getBoard(boardId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<BoardListResponse> searchBoards(@RequestParam("query") String query, @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        BoardListResponse response = boardService.searchBoard(query, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/images")
    public ResponseEntity<BoardImageResponse> uploadBoardImages(@ValidToken Long memberId, @RequestPart("file") List<MultipartFile> files) {
        BoardImageResponse response = boardService.uploadImages(memberId, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{boardId}")
    public ResponseEntity<Void> updateBoard(@PathVariable Long boardId, @ValidToken Long memberId, @RequestBody BoardUpdateRequest request) {
        boardService.updateBoard(boardId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId, @ValidToken Long memberId) {
        boardService.deleteBoard(boardId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{boardId}/like")
    public ResponseEntity<Void> likeBoard(@PathVariable Long boardId, @ValidToken Long memberId) {
        boardService.likeBoard(boardId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(value = "/{boardId}/like")
    public ResponseEntity<Void> cancelLikeBoard(@PathVariable Long boardId, @ValidToken Long memberId) {
        boardService.cancelLikeBoard(boardId, memberId);
        return ResponseEntity.noContent().build();
    }
}
