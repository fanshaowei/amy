package com.amy.sunpalaceartspace.rocketmq;

import com.amy.sunpalaceartspace.domain.bo.ReservationOrderMqSendBO;
import com.amy.sunpalaceartspace.service.SubmitReservationOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;

/**
 * @ClassName RocketmqConsumer
 * @Description TODO
 * @Author fantasyfan
 * @Date 2023/4/18 19:40
 */
@RequiredArgsConstructor
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = MQ_CONSUMER_GROUP, topic = MQ_TOPIC,
        selectorType = SelectorType.TAG, selectorExpression = MQ_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY, messageModel = MessageModel.CLUSTERING
)
public class ReservationOrderMqConsumer implements RocketMQListener<String> {
    private final SubmitReservationOrderService submitReservationOrderService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String o) {
        log.info("消费者接收消息: => {}", o);
        try {
            ReservationOrderMqSendBO messageMqConsumerBO = objectMapper.readValue(o, ReservationOrderMqSendBO.class);
            submitReservationOrderService.reservateOrder(messageMqConsumerBO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("处理消息失败: => {}", o, e);
        }
    }

}
