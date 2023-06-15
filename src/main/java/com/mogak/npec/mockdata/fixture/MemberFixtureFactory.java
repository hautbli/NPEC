package com.mogak.npec.mockdata.fixture;

import com.mogak.npec.member.domain.Member;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Random;
import java.util.function.Predicate;

import static org.jeasy.random.FieldPredicates.*;

@Component
public class MemberFixtureFactory {
    private Random random;

    public MemberFixtureFactory() {
        this.random = new Random();
    }

    public EasyRandom get() {
        Predicate<Field> idPredicate = named("id")
                .and(ofType(Long.class))
                .and(inClass(Member.class));

        Predicate<Field> emailPredicate = named("email")
                .and(ofType(String.class))
                .and(inClass(Member.class));

        Predicate<Field> passwordPredicate = named("password")
                .and(ofType(String.class))
                .and(inClass(Member.class));

        Predicate<Field> imagePredicate = named("imageUrl")
                .and(ofType(String.class))
                .and(inClass(Member.class));

        EasyRandomParameters parameters = new EasyRandomParameters()
                .excludeField(idPredicate)
                .stringLengthRange(5, 20)
                .randomize(emailPredicate, () -> getRandomString(10) + "@example.com")
                .randomize(passwordPredicate, ()-> "1234")
                .randomize(imagePredicate, ()-> "https://npecbucket.s3.ap-northeast-2.amazonaws.com/test%40example.com/boards/873df9bd-6489-409e-bfa4-cb24b43173e3.png")
                .dateRange(LocalDate.of(2022, 1,1 ),
                        LocalDate.of(2023, 5,16 ));

        return new EasyRandom(parameters);

    }

    private String getRandomString(int lengthLimit) {
        int aToInt = 97;
        int zToInt = 122;

        return random.ints(aToInt, zToInt + 1)
                .limit(lengthLimit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
