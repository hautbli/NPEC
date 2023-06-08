package com.mogak.npec.board.application;

import com.mogak.npec.board.controller.SortType;
import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.domain.BoardLike;
import com.mogak.npec.board.domain.BoardSort;
import com.mogak.npec.board.dto.BoardCreateRequest;
import com.mogak.npec.board.dto.BoardGetResponse;
import com.mogak.npec.board.dto.BoardImageResponse;
import com.mogak.npec.board.dto.BoardListResponse;
import com.mogak.npec.board.dto.BoardSortNotFoundException;
import com.mogak.npec.board.dto.BoardUpdateRequest;
import com.mogak.npec.board.exceptions.BoardCanNotModifyException;
import com.mogak.npec.board.exceptions.BoardNotFoundException;
import com.mogak.npec.board.exceptions.MemberAlreadyLikeBoardException;
import com.mogak.npec.board.exceptions.MemberNotLikeBoardException;
import com.mogak.npec.board.repository.BoardLikeRepository;
import com.mogak.npec.board.repository.BoardRepository;
import com.mogak.npec.board.repository.BoardSortRepository;
import com.mogak.npec.common.aws.S3Helper;
import com.mogak.npec.hashtag.application.HashTagService;
import com.mogak.npec.hashtag.domain.HashTag;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.exception.MemberNotFoundException;
import com.mogak.npec.member.repository.MemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private static final String BOARD_PATH = "boards";
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final S3Helper s3Helper;
    private final BoardLikeRepository boardLikeRepository;
    private final HashTagService hashTagService;
    private final BoardSortRepository boardSortRepository;
    private final ApplicationEventPublisher eventPublisher;


    public BoardService(BoardRepository boardRepository, MemberRepository memberRepository, S3Helper s3Helper, BoardLikeRepository boardLikeRepository, HashTagService hashTagService, BoardSortRepository boardSortRepository, ApplicationEventPublisher eventPublisher) {
        this.boardRepository = boardRepository;
        this.memberRepository = memberRepository;
        this.s3Helper = s3Helper;
        this.boardLikeRepository = boardLikeRepository;
        this.hashTagService = hashTagService;
        this.boardSortRepository = boardSortRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Long createBoard(Long memberId, BoardCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));

        Board board = new Board(member, request.getTitle(), request.getContent());
        Board savedBoard = boardRepository.save(board);

        hashTagService.createHashTags(savedBoard, request.getHashTags());

        boardSortRepository.save(BoardSort.of(savedBoard));

        return savedBoard.getId();
    }

    @Transactional(readOnly = true)
    public BoardListResponse getBoards(int page, SortType sortType) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by(sortType.getSortField()).descending());

        // todo 없애버릴까? member n+1 발생
        if (sortType.equals(SortType.LATEST)) {
            Page<Board> boards = boardRepository.findAll(pageable);
            List<Long> boardIds = boards.stream()
                    .map(Board::getId)
                    .toList();

            List<BoardSort> boardSorts = boardSortRepository.findByBoardIdIn(boardIds);
            Map<Long, BoardSort> boardSortsByBoardId = boardSorts.stream().collect(Collectors.toMap(boardSort -> boardSort.getBoard().getId(), boardSort -> boardSort));

            Map<Long, List<HashTag>> hashTagsByBoardId = hashTagService.getHashTags(boardIds);

            return BoardListResponse.of(boards.getContent(), boardSortsByBoardId, hashTagsByBoardId, boards.getTotalPages());
        }

        // boardSort 에서 정렬이 된 순서대로 가져온다.
        Page<BoardSort> boardSorts = boardSortRepository.findAll(pageable);
        // 정렬된 boardId 리스트를 만든다.
        List<Long> sortedBoardIds = boardSorts.getContent().stream()
                .map(boardSort -> boardSort.getBoard().getId())
                .toList();

        // boardId 리스트로 board 리스트를 조회한다.
        List<Board> boards = boardRepository.findAllByIdIn(sortedBoardIds);
        // boardId:board 맵을 만든다.
        Map<Long, Board> boardByBoardId = boards.stream()
                .collect(Collectors.toMap(Board::getId, board -> board));

        // 정렬된 boardId를 순회하면서 board map 을 조회하여 정렬된 board list 를 만든다.
        List<Board> sortedBoard = sortedBoardIds.stream()
                .map(boardByBoardId::get)
                .toList();

        Map<Long, BoardSort> boardSortsByBoardId = boardSorts.getContent().stream()
                .collect(Collectors.toMap(boardSort -> boardSort.getBoard().getId(), boardSort -> boardSort));
        Map<Long, List<HashTag>> hashTagsByBoardId = hashTagService.getHashTags(sortedBoardIds);

        return BoardListResponse.of(sortedBoard, boardSortsByBoardId, hashTagsByBoardId, boardSorts.getTotalPages());
    }

    @Transactional
    public BoardGetResponse getBoard(Long boardId) {
        Board findBoard = findBoard(boardId);

        BoardSort boardSort = findBoardSort(boardId);
        boardSort.increaseViewCount();

        List<HashTag> hashTags = hashTagService.getHashTags(findBoard.getId());

        return BoardGetResponse.of(findBoard, boardSort, hashTags);
    }

    @Transactional
    public void updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Member findMember = findMember(memberId);
        Board findBoard = findBoard(boardId);

        verifyBoard(findBoard);
        verifyMember(findMember, findBoard.getMember());

        findBoard.update(request.getTitle(), request.getContent());
        hashTagService.updateHashTags(findBoard, request.getHashTags());
    }

    @Transactional
    public void deleteBoard(Long boardId, Long memberId) {
        Member findMember = findMember(memberId);
        Board findBoard = findBoard(boardId);

        verifyBoard(findBoard);
        verifyMember(findMember, findBoard.getMember());

        findBoard.delete();
    }

    @Transactional
    public BoardImageResponse uploadImages(Long memberId, List<MultipartFile> files) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
        List<String> paths = new ArrayList<>();


        files.forEach(image -> {
            String extension = Objects.requireNonNull(image.getContentType()).split("/")[1];
            String imageName = UUID.randomUUID() + "." + extension;
            String path = member.getEmail() + "/" + BOARD_PATH;

            if (s3Helper.uploadImage(imageName, path, extension, image)) {
                paths.add(s3Helper.getPath(path, imageName));
            }
        });
        return new BoardImageResponse(paths);
    }

    private void verifyBoard(Board findBoard) {
        if (findBoard.getIsDeleted()) {
            throw new BoardCanNotModifyException("삭제된 게시물 입니다.");
        }
    }

    private void verifyMember(Member findMember, Member targetMember) {
        if (!findMember.match(targetMember)) {
            throw new BoardCanNotModifyException("게시글의 작성자와 요청한 작성자가 다릅니다.");
        }
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new BoardNotFoundException("게시글을 찾을 수 없습니다.")
        );
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("사용자를 찾을 수 없습니다."));
    }

    // TODO boardlike 서비스 분리하기
    @Transactional
    public void likeBoard(Long boardId, Long memberId) {
        Member findMember = findMember(memberId);
        Board findBoard = findBoard(boardId);

        if (boardLikeRepository.existsByMemberIdAndBoardId(findMember.getId(), findBoard.getId())) {
            throw new MemberAlreadyLikeBoardException("이미 추천한 게시물 입니다.");
        }
        boardLikeRepository.save(new BoardLike(findMember, findBoard));

        eventPublisher.publishEvent(
                BoardLikeCreatedEvent.of(boardId)
        );
    }

    @Transactional
    public void cancelLikeBoard(Long boardId, Long memberId) {
        Member findMember = findMember(memberId);
        Board findBoard = findBoard(boardId);

        BoardLike boardLike = boardLikeRepository.findByMemberIdAndBoardId(findMember.getId(), findBoard.getId())
                .orElseThrow(() -> new MemberNotLikeBoardException("사용자가 추천한 게시물이 아닙니다."));

        boardLikeRepository.delete(boardLike);

        eventPublisher.publishEvent(
                BoardLikeCreatedEvent.of(boardId)
        );
    }

    private BoardSort findBoardSort(Long boardId) {
        return boardSortRepository.findByBoardId(boardId).orElseThrow(
                () -> new BoardSortNotFoundException("board sort 가 저장되어 있지 않습니다.")
        );
    }

    @Transactional(readOnly = true)
    public BoardListResponse searchBoard(String query, Pageable pageable) {
        Page<Board> boards = boardRepository.searchBoard(query, pageable);

        List<Long> boardIds = boards.stream().map(Board::getId).collect(Collectors.toList());
        Map<Long, List<HashTag>> hashTagsByBoardId = hashTagService.getHashTags(boardIds);

//        return BoardListResponse.of(boards, hashTagsByBoardId, boards.getTotalPages());
        return null;
    }
}
