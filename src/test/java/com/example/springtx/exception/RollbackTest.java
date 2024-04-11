package com.example.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class RollbackTest {

    @Autowired RollbackService service;

    /**
     * 결과적으로 롤백이 호출된다.
     */
    @Test
    void runtimeExceptionTest() {
        Assertions.assertThatThrownBy(() -> service.runtimeExceptionMethod())
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * 커밋되어야 한다.
     */
    @Test
    void checkedExceptionTest() {
        Assertions.assertThatThrownBy(() -> service.checkedExceptionMethod())
                .isInstanceOf(MyException.class);
    }

    /**
     * 롤백될 것이다.
     */
    @Test
    void rollbackForTest() {
        Assertions.assertThatThrownBy(() -> service.rollbackForMethod())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackTestConfig {
        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    /**
     * 3가지 경우를 살펴보자.
     */
    @Slf4j
    static class RollbackService {

        //런타임 예외 발생: 롤백
        @Transactional
        public void runtimeExceptionMethod() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        /**
         * 체크 예외 발생: 커밋
         * 예외가 터졌는데 왜 커밋을 할까? 가 중요하다.
         * @throws MyException
         */
        @Transactional
        public void checkedExceptionMethod() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        /**
         * 체크 예외 rollbackFor 지정: 롤백
         * 체크 예외지만 롤백할꺼야.
         */
        @Transactional(rollbackFor = MyException.class)
        public void rollbackForMethod() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }
    }

    /**
     * 체크 예외를 하나 만들어준다.
     */
    static class MyException extends Exception {
    }

}
