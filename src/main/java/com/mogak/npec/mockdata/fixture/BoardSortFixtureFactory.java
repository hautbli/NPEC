package com.mogak.npec.mockdata.fixture;

import com.mogak.npec.board.domain.BoardSort;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.function.Predicate;

import static org.jeasy.random.FieldPredicates.*;

@Component
public class BoardSortFixtureFactory {

    public EasyRandom get(Long index) {
        Predicate<Field> idPredicate = named("id")
                .and(ofType(Long.class))
                .and(inClass(BoardSort.class));

        Predicate<Field> likeCountPredicate = named("likeCount")
                .and(ofType(Long.class))
                .and(inClass(BoardSort.class));

        Predicate<Field> viewCountPredicate = named("viewCount")
                .and(ofType(Long.class))
                .and(inClass(BoardSort.class));

        Predicate<Field> commentCountPredicate = named("commentCount")
                .and(ofType(Long.class))
                .and(inClass(BoardSort.class));

        EasyRandomParameters parameters = new EasyRandomParameters()
                .excludeField(idPredicate)
                .randomize(likeCountPredicate, () -> 0L)
                .randomize(viewCountPredicate, new LongRangeRandomizer(0L, 90000L))
                .randomize(commentCountPredicate, () -> 0L)
                .randomize(Long.class, () -> index)
                .dateRange(LocalDate.of(2022, 1, 1),
                        LocalDate.of(2023, 5, 16));

        return new EasyRandom(parameters);
    }
}
