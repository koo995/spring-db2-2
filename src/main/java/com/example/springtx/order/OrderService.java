package com.example.springtx.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * JPA는 트랜잭션 커밋 시점에 Order 데이터를 DB에 반영한다.
     * 테스트 도중 하나 궁금한 것이 발생할 수 있다.
     * 롤백이 발생했을때 save() 메서드에 의해서 insert 쿼리가 발생해야 하는것이 아닌지 의문이 생길 수 있다.
     * jpa 는 커밋될때 쿼리가 날라가는 것이 있다. 어짜피 롤백이 되면 쿼리 자체가 날라가지 않는다.
     */
    @Transactional
    public void order(Order order) throws NotEnoughMoneyException {
        log.info("order 호출");
        orderRepository.save(order);

        log.info("결제 프로제스 진입");
        if (order.getUsername().equals("예외")) {
            log.info("시스템 예외 발생");
            throw new RuntimeException("시스템 예외");

        } else if (order.getUsername().equals("잔고부족")) {
            log.info("잔고 부족 비즈니스 예외 발생");
            order.setPayStatus("대기");
            throw new NotEnoughMoneyException("잔고가 부족합니다");
        } else {
            //정상 승인
            log.info("정상 승인");
            order.setPayStatus("완료");
        }
        log.info("결제 프로세스 완료");
    }
}
