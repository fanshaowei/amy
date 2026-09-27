package com.amy.sunpalaceartspace.rocketmq;

import com.alibaba.fastjson2.JSONObject;
import com.amy.sunpalaceartspace.domain.bo.ReservationOrderMqConsumerBO;
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
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = MQ_CONSUMER_GROUP, topic = MQ_TOPIC,
        selectorType = SelectorType.TAG, selectorExpression = MQ_TAG,
        consumeMode = ConsumeMode.CONCURRENTLY, messageModel = MessageModel.BROADCASTING
)
public class ReservationOrderMqConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String o) {
        log.info("消费者接收消息: => {}", o);
        ReservationOrderMqConsumerBO messageMqConsumerBO = JSONObject.parseObject(o, ReservationOrderMqConsumerBO.class);
    }

}
