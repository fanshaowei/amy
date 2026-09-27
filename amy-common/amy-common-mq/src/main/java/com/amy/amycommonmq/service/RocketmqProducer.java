package com.amy.amycommonmq.service;

import com.alibaba.fastjson.JSONObject;
import com.amy.amycommonmq.bo.AmyMqBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQLocalRequestCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Slf4j
@Component
public class RocketmqProducer<T extends AmyMqBO> {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 普通字符串消息，同步
     * @param topic
     * @param messageData
     */
    public void convertAndSend(String topic, T messageData) {
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("convertAndSend send Message topic: {}, messageData : {}", topic, messageDataStr);
        rocketMQTemplate.convertAndSend(topic, messageDataStr);
    }

    /**
     * * @description: 同步消息
     * 这种可靠性同步地发送方式使用的比较广泛，比如：重要的消息通知，短信通知。
     * @param topic
     * @param messageData
     * @return
     */
    public SendResult syncSend(String topic, T messageData) {
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("syncSend send Message topic : {}, messageData : {}", topic, messageDataStr);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        SendResult sendResult = rocketMQTemplate.syncSend(topic, message);
        log.info("syncSend send Message result : {}", JSONObject.toJSONString(sendResult));
        return sendResult;
    }

    /**
     * 同步按顺序发送消息
     * @param topic
     * @param messageData
     * @param hashKey
     * @param delayLevel
     */
    public SendResult syncSendOrderly(String topic, T messageData, String hashKey, int delayLevel){
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("syncSendOrderly send message, topic : {}, messageData : {}", topic, messageDataStr);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(topic, message, hashKey);
        log.info("syncSendOrderly send message result : {}", JSONObject.toJSONString(sendResult));
        return sendResult;
    }

    /**
     * 同步发送消息并返回
     * @param topic
     * @param messageData
     */
    public <R> R sendAndReceiveSync(String topic, T messageData, Class<R> clazz) {
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("sendAndReceiveSync send message, topic : {}, messageData : {}", topic, messageDataStr);
        Message<T> message = MessageBuilder.withPayload(messageData).build();
        R result = rocketMQTemplate.sendAndReceive(topic, message, clazz);
        log.info("sendAndReceiveSync send message result: {}", result);
        return result;
    }

    /**
     * 异步发送消息
     * @param destination formats: `topicName:tags`
     * @param messageData
     */
    public void asyncSend(String destination, T messageData, Consumer<SendResult> consumer) {
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("asyncSend send message, topic : {}, messageData : {}", destination, messageDataStr);
        Message<?> message = MessageBuilder.withPayload(messageData).build();
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info( "asyncSend onSuccess, sendResult : {}" , JSONObject.toJSONString(sendResult));
                consumer.accept(sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info( "asyncSend onException, exception : {}" ,throwable.getMessage());
            }
        });
    }

    /**
     * 异步发送消息并接收返回结果
     * @param topic
     * @param messageData
     * @param consumer
     */
    public void sendAndReceiveAsync(String topic, T messageData, Consumer<String> consumer) {
        String messageDataStr = JSONObject.toJSONString(messageData);
        log.info("sendAndReceiveAsync send message, topic : {}, messageData : {}", topic, messageDataStr);
        Message<String> message = MessageBuilder.withPayload(messageDataStr).build();
        rocketMQTemplate.sendAndReceive(topic, message,
                new RocketMQLocalRequestCallback<String>() {
                    @Override public void onSuccess(String message) {
                        log.info("sendAndReceiveAsync on Success , topic : {}, messageData : {}, successMsg : {}", topic, messageData, message);
                        consumer.accept(message);
                    }

                    @Override public void onException(Throwable e) {
                        log.error("sendAndReceiveAsync on onException , topic : {}, messageData : {}, errorMsg : {}", topic, messageData, message);
                    }
                }, 5000);
    }

}
