package com.mogak.npec.mockdata.fixture;

import com.mogak.npec.board.domain.Board;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.function.Predicate;

import static org.jeasy.random.FieldPredicates.*;

@Component
public class BoardFixtureFactory {
    public EasyRandom get() {
        Predicate<Field> idPredicate = named("id")
                .and(ofType(Long.class))
                .and(inClass(Board.class));

        EasyRandomParameters parameters = new EasyRandomParameters()
                .excludeField(idPredicate)
                .stringLengthRange(10, 200)
                .randomize(Long.class, new LongRangeRandomizer(1L, 100_000L))
                .dateRange(LocalDate.of(2022, 1, 1),
                        LocalDate.of(2023, 5, 16));

        return new EasyRandom(parameters);
    }
}
