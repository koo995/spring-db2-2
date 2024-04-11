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
     * dataSource 는 스프링이 주입해 주는 것인데 HikariDataSource 이다.
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

    /**
     * 로그를 보면 tx1 과 tx2 가 같은 conn0 을 사용중이다. 이것은 중간에 커넥션 풀 때문에 그런 것이다. tx1 이 끝나면 conn0 커넥션 풀에 반납까지 완료했다.
     * 이후에 tx2가 시작되면 conn0 을 다시 획득한 것이다. 따라서 둘은 완전히 다른 커넥션으로 인지하는 것이 맞다.(물리적인 커넥션은 같지만...)
     * 그렇다면 둘을 구분할 수 있는 다른 방법은 없을까?
     * 히카리 커넥션 풀에서 커넥션을 획득하면 실제 커넥션을 그대로 반납하는 것이 아니라 내부 관리를 위해 히카리 "프록시" 커넥션이라는 객체를 생성해서 반환한다.
     * 물론 내부에는 실제 커넥션이 포함되어 있다. 이 객체의 주소를 확인하면 커넥션 풀에서 획득한 커넥션을 구분할 수 있다.
     */
    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }

    /**
     * 트랜잭션을 각각 사용하는 것이 아니라, 트랜잭션이 이미 진행중인데, 여기에 추가로 트랜잭션을 수행하면 어떻게 될까?
     * 기존 트랜잭션과 별도의 트랜잭션을 진행해야 할까? 아니면 기존 트랜잭션을 그대로 이어 받아서 트랜잭션을 수행해야 할까?
     * 기존의 트랜잭션이 커밋을 하지 않았는데 또 트랜잭션을 획득한다면 애매하다. 중첩되어서 시작되었다.
     * 이런 경우 어떻게 동작할지 결정하는 것을 트랜잭션 전파라 한다.
     * isNewTransaction 이것은 이게 처음 시작된 트랜잭션인지 아닌지를 판단하는 것이다.
     * "내부 트랜잭션 커밋" 후 아무런 로그가 없이 "외부 트랜잭션 커밋" 이 나타난다.
     * 그 후 "외부 트랜잭션 커밋" 이후에 "Initiating transaction commit" 이 나타난다.
     * 스프링은 이렇게 여러 트랜잭션이 함께 사용되는 경우, 처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리 하도록 한다.
     * txManager.commit(outer); 이후에 닫히는 로그가 나타나고 그 다음
     * txManager.commit(inner); 을 하면 이미 tx는 끝나서 아무것도 안나타난다.
     */
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);


    }

    /**
     * 전체가 롤백될 것이다.
     */
    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }
}
