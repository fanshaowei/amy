package com.amy.sunpalaceartspace.service.rocketmq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQLocalRequestCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @ClassName RocketmqProducer
 * @Description TODO
 * @Author fantasyfan
 * @Date 2023/4/20 14:48
 */
@Slf4j
@Component
public class RocketmqProducer {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 普通字符串消息，同步
     * @param topic
     * @param messageData
     */
    public void convertAndSend(String topic, String messageData) {
        log.info("convertAndSend send Message topic: {}, messageData : {}", topic, messageData);
        rocketMQTemplate.convertAndSend(topic, messageData);
    }

    /**
     * * @description: 同步消息
     * 这种可靠性同步地发送方式使用的比较广泛，比如：重要的消息通知，短信通知。
     * @param topic
     * @param messageData
     * @return
     */
    public SendResult syncSend(String topic, String messageData) {
        log.info("syncSend send Message topic : {}, messageData : {}", topic, messageData);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        SendResult sendResult = rocketMQTemplate.syncSend(topic, message);
        log.info("syncSend send Message result : {}", JSONObject.toJSONString(sendResult));
        return sendResult;
    }

    /**
     * 异步发送消息
     * @param destination formats: `topicName:tags`
     * @param messageData
     */
    public void asyncSend(String destination, String messageData) {
        log.info("asyncSend send message, topic : {}, messageData : {}", destination, messageData);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info( "asyncSend onSuccess, sendResult : {}" , JSONObject.toJSONString(sendResult));
            }

            @Override
            public void onException(Throwable throwable) {
                log.info( "asyncSend onException, exception : {}" ,throwable.getMessage());
            }
        });
    }


    /**
     * 同步按顺序发送消息
     * @param topic
     * @param messageData
     * @param hashKey
     * @param delayLevel
     */
    public void syncSendOrderly(String topic, String messageData, String hashKey, int delayLevel){
        log.info("syncSendOrderly send message, topic : {}, messageData : {}", topic, messageData);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(topic, message, hashKey);
        log.info("syncSendOrderly send message result : {}", JSONObject.toJSONString(sendResult));
    }

    /**
     * 同步发送消息并返回
     * @param topic
     * @param messageData
     */
    public void sendAndReceiveSync(String topic, String messageData) {
            log.info("sendAndReceiveSync send message, topic : {}, messageData : {}", topic, messageData);
            Message<String> message = MessageBuilder.withPayload(messageData).build();
            String result = rocketMQTemplate.sendAndReceive(topic, message, String.class);
            log.info("sendAndReceiveSync send message result: {}", result);
    }

    /**
     * 异步发送消息并接收返回结果
     * @param topic
     * @param messageData
     * @param callback
     */
    public void sendAndReceiveAsync(String topic, String messageData, RocketMQLocalRequestCallback<String> callback) {
        log.info("sendAndReceiveAsync send message, topic : {}, messageData : {}", topic, messageData);
        Message<String> message = MessageBuilder.withPayload(messageData).build();
        rocketMQTemplate.sendAndReceive(topic, message,
                new RocketMQLocalRequestCallback<String>() {
                    @Override public void onSuccess(String message) {
                        log.info("sendAndReceiveAsync on Success , topic : {}, messageData : {}, successMsg : {}",
                                topic, messageData, message);
                        callback.onSuccess(message);
                    }

                    @Override public void onException(Throwable e) {
                        log.error("sendAndReceiveAsync on onException , topic : {}, messageData : {}, errorMsg : {}",
                                topic, messageData, message);
                    }
                }, 5000);
    }

}
