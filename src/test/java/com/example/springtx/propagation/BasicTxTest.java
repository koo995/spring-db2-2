package com.example.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    /**
     * 자동으로 등록해주지만 이렇게 수동으로 등록해줄 수도 있다.
     * 원래라면 아래와 같이 나타나지만...
     * Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
     * Opened new EntityManager [SessionImpl(1418986727<open>)] for JPA transaction
     * Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@16b2d182]
     *
     * jdbc 커넥션으로 변경이 되었다?
     * Acquired Connection [HikariProxyConnection@24331479 wrapping conn0: url=jdbc:h2:mem:f10cea15-54f1-49c3-a199-98cb81481dc7 user=SA] for JDBC transaction
     * Switching JDBC Connection [HikariProxyConnection@24331479 wrapping conn0: url=jdbc:h2:mem:f10cea15-54f1-49c3-a199-98cb81481dc7 user=SA] to manual commit
     *
     */
    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }
}
