package com.mogak.npec.mockdata;

import com.mogak.npec.board.application.BoardService;
import com.mogak.npec.board.domain.Board;
import com.mogak.npec.board.domain.BoardSort;
import com.mogak.npec.board.dto.BoardCreateRequest;
import com.mogak.npec.board.repository.BoardBulkRepository;
import com.mogak.npec.board.repository.BoardSortBulkRepository;
import com.mogak.npec.comment.application.CommentService;
import com.mogak.npec.comment.dto.CommentResponse;
import com.mogak.npec.comment.dto.CommentsResponse;
import com.mogak.npec.comment.dto.CreateCommentServiceDto;
import com.mogak.npec.comment.dto.CreateReplyServiceDto;
import com.mogak.npec.comment.dto.FindCommentsServiceDto;
import com.mogak.npec.member.application.MemberService;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.dto.MemberCreateRequest;
import com.mogak.npec.member.repository.MemberBulkRepository;
import com.mogak.npec.member.repository.MemberRepository;
import com.mogak.npec.mockdata.fixture.BoardFixtureFactory;
import com.mogak.npec.mockdata.fixture.BoardSortFixtureFactory;
import com.mogak.npec.mockdata.fixture.MemberFixtureFactory;
import lombok.RequiredArgsConstructor;
import org.jeasy.random.EasyRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RequiredArgsConstructor
@RestController
@RequestMapping("/testdata")
@Profile("dummy")
public class MockDataController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final BoardService boardService;
    private final CommentService commentService;
    private final MemberBulkRepository memberBulkRepository;
    private final BoardBulkRepository boardBulkRepository;
    private final BoardSortBulkRepository boardSortBulkRepository;
    private final BoardSortFixtureFactory boardSortFixtureFactory;
    private final BoardFixtureFactory boardFixtureFactory;
    private final MemberFixtureFactory memberFixtureFactory;

    @PostMapping("/big/members")
    @Transactional
    public void bulkCreateMembers() {

        StopWatch stopWatch = new StopWatch();

        System.out.println("Bulk Insert Start!!");
        stopWatch.start();

        IntStream.range(0, 1).forEach(i -> memberBulkRepository.bulkInsert(createMembers(10_000 * 10)));

        stopWatch.stop();
        System.out.println("쿼리 실행 시간: " + stopWatch.getTotalTimeSeconds() + "초");
    }

    @PostMapping("/big/boards")
    @Transactional
    public void bulkCreateBoards() {
        StopWatch stopWatch = new StopWatch();

        System.out.println("Bulk Insert Start!!");
        stopWatch.start();

        IntStream.range(0, 50).forEach(i -> boardBulkRepository.bulkInsert(createBoards(10_000 * 10)));

        stopWatch.stop();
        System.out.println("쿼리 실행 시간: " + stopWatch.getTotalTimeSeconds() + "초");
    }

    @PostMapping("/big/boardSorts")
    @Transactional
    public void bulkCreateBoardSorts() {
        StopWatch stopWatch = new StopWatch();

        System.out.println("Bulk Insert Start!!");
        stopWatch.start();

        Long startIdx = 1L;
        Long endIdx = startIdx + 10_000L;
        for (int i = 0; i < 300; i++) {
            boardSortBulkRepository.bulkInsert(createBoardSorts(startIdx, endIdx));
        }

        stopWatch.stop();
        System.out.println("쿼리 실행 시간: " + stopWatch.getTotalTimeSeconds() + "초");
    }

    private List<BoardSort> createBoardSorts(Long startIdIndex, Long endIdIndex) {

        return Stream
                .iterate(startIdIndex, i -> i < endIdIndex, i -> i + 1)
                .parallel()
                .map(i -> boardSortFixtureFactory.get(i))
                .map(easyRandom -> easyRandom.nextObject(BoardSort.class))
                .collect(Collectors.toList());
    }

    private List<Board> createBoards(int countPerQuery) {
        EasyRandom easyRandom = boardFixtureFactory.get();

        return IntStream.range(0, countPerQuery)
                .parallel()
                .mapToObj(member -> easyRandom.nextObject(Board.class))
                .toList();
    }

    private List<Member> createMembers(int countPerQuery) {
        EasyRandom easyRandom = memberFixtureFactory.get();

        return IntStream.range(0, countPerQuery)
                .parallel()
                .mapToObj(member -> easyRandom.nextObject(Member.class))
                .toList();
    }


    @PostMapping("/simpleComments")
    @Transactional
    public void createSimpleMockData() {
        // member
        List<MemberCreateRequest> memberCreateRequests = List.of(
                new MemberCreateRequest("1@mail.com", "1", "123456"),
                new MemberCreateRequest("2@mail.com", "2", "123456"),
                new MemberCreateRequest("3@mail.com", "3", "123456"),
                new MemberCreateRequest("4@mail.com", "4", "123456"),
                new MemberCreateRequest("5@mail.com", "5", "123456"),
                new MemberCreateRequest("6@mail.com", "6", "123456"),
                new MemberCreateRequest("7@mail.com", "7", "123456"),
                new MemberCreateRequest("8@mail.com", "8", "123456"),
                new MemberCreateRequest("9@mail.com", "9", "123456"),
                new MemberCreateRequest("10@mail.com", "10", "123456")
        );

        for (MemberCreateRequest memberCreateRequest : memberCreateRequests) {
            memberService.createMember(memberCreateRequest);
        }

        // board
        List<Member> members = memberRepository.findAll();
        Long boardId = boardService.createBoard(members.get(0).getId(),
                new BoardCreateRequest("title", "content", List.of("aa", "bb")));

        // comment
        for (Member member : members) {
            commentService.createComment(new CreateCommentServiceDto(member.getId(), boardId, "comment: " + member.getNickname()));
        }

        // reply
        List<MemberCreateRequest> memberCreateRequests2 = List.of(
                new MemberCreateRequest("11@mail.com", "11", "123456"),
                new MemberCreateRequest("12@mail.com", "12", "123456"),
                new MemberCreateRequest("13@mail.com", "13", "123456"),
                new MemberCreateRequest("14@mail.com", "14", "123456"),
                new MemberCreateRequest("15@mail.com", "15", "123456"),
                new MemberCreateRequest("16@mail.com", "16", "123456"),
                new MemberCreateRequest("17@mail.com", "17", "123456"),
                new MemberCreateRequest("18@mail.com", "18", "123456"),
                new MemberCreateRequest("19@mail.com", "19", "123456"),
                new MemberCreateRequest("20@mail.com", "20", "123456"),
                new MemberCreateRequest("21@mail.com", "21", "123456"),
                new MemberCreateRequest("22@mail.com", "22", "123456"),
                new MemberCreateRequest("23@mail.com", "23", "123456"),
                new MemberCreateRequest("24@mail.com", "24", "123456"),
                new MemberCreateRequest("25@mail.com", "25", "123456"),
                new MemberCreateRequest("26@mail.com", "26", "123456"),
                new MemberCreateRequest("27@mail.com", "27", "123456"),
                new MemberCreateRequest("28@mail.com", "28", "123456"),
                new MemberCreateRequest("29@mail.com", "29", "123456"),
                new MemberCreateRequest("30@mail.com", "30", "123456")
        );

        for (MemberCreateRequest memberCreateRequest : memberCreateRequests2) {
            memberService.createMember(memberCreateRequest);
        }

        List<Member> allMembers = memberRepository.findAll();
        List<Member> members2 = allMembers.subList(10, allMembers.size());
        System.out.println("members2 = " + members2.size());

        CommentsResponse comments = commentService.findComments(new FindCommentsServiceDto(1L, boardId));

        for (int i = 0; i < comments.getComments().size(); i++) {
            List<Member> replayMembers = members2.subList(0, 2);
            CommentResponse comment = comments.getComments().get(i);

            for (Member replayMember : replayMembers) {
                commentService.createReply(new CreateReplyServiceDto(replayMember.getId(), comment.getId(), "replay: " + replayMember.getId()));
            }
            replayMembers.clear();
        }
    }
}
