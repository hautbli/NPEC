package com.mogak.npec.auth.controller;

import com.mogak.npec.auth.EncryptorImpl;
import com.mogak.npec.auth.dto.LoginRequest;
import com.mogak.npec.auth.exception.LoginFailException;
import com.mogak.npec.member.domain.Member;
import com.mogak.npec.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @DisplayName("저장된 이메일과 비밀번호로 요청한 경우 멤버 id 를 리턴한다.")
    @Test
    void login() {
        // given
        String email = "member@m.com";
        memberRepository.save(new Member("a", email, new EncryptorImpl().encrypt("1234")));

        // when
        Long memberId = authService.login(new LoginRequest(email, "1234"));

        // then
        assertThat(memberId).isNotNull();
    }

    @DisplayName("유효하지 않은 이메일과 비밀번호로 요청한 경우 예외를 던진다.")
    @Test
    void loginWithFail() {
        // given
        String email = "member@m.com";
        memberRepository.save(new Member("a", email, new EncryptorImpl().encrypt("1234")));

        // when
        assertThatThrownBy(
                () -> authService.login(new LoginRequest(email, "11"))
        ).isExactlyInstanceOf(LoginFailException.class);
    }
}