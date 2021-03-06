package com.lawu.mq;

import ch.qos.logback.classic.Level;
import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.common.constant.LoggerName;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.lawu.mq.MQAutoConfiguration.RocketMQAutoConfiguration;
import com.lawu.mq.consumer.CustomConsumerRegister;
import com.lawu.mq.consumer.CustomConsumerRegisterImpl;
import com.lawu.mq.message.MQConsumerFactory;
import com.lawu.mq.message.MessageProducerService;
import com.lawu.mq.message.impl.AbstractMessageConsumerListener;
import com.lawu.mq.message.impl.RocketMQMessageProducerServiceImpl;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RocketMQ自动配置类
 *
 * @author jiangxinjun
 * @createDate 2017年12月22日
 * @updateDate 2017年12月22日
 */
@Configuration
@Import({RocketMQAutoConfiguration.class})
public class MQAutoConfiguration {

    @Bean
    public CustomConsumerRegister customConsumerRegister() {
        return new CustomConsumerRegisterImpl();
    }

    // Mock
    @Bean
    @ConditionalOnMissingBean
    public MessageProducerService messageProducerService() {
        return new MessageProducerService() {
            @Override
            public void sendMessage(String topic, String tags, Object message) {
            }
        };
    }

    @ConditionalOnClass(DefaultMQProducer.class)
    @ConditionalOnProperty(value = "lawu.mq.rocketmq.namesrvAddr")
    @Configuration
    public static class RocketMQAutoConfiguration {

        @Bean(initMethod = "start", destroyMethod = "shutdown")
        @ConfigurationProperties(prefix = "lawu.mq.rocketmq")
        public DefaultMQProducer defaultMQProducer() {
            // 设置为不加载自带的配置,使用项目中的日志配置
            System.setProperty("rocketmq.client.log.loadconfig", "false");
            DefaultMQProducer defaultMQProducer = new DefaultMQProducer();

            /*
             * 调整RocketmqClient日志输出
             * 1.rocketmq.client.log.loadconfig设置为false
             * 2.rocketmq.client.logback.resource.fileName设置为本地的配置文件
             * 3.清除自身所带的输出源,叠加项目的输出源
             * 4.在初始化之前设置rocketmq.client.log.loadconfig为false,不加载自带配置***
             * */
            ch.qos.logback.classic.Logger clientLoggerNameLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoggerName.ClientLoggerName);
            // 清除本身自带的输出源
            //clientLoggerNameLogger.detachAndStopAllAppenders();
            // 叠加顶级的输出源
            ///clientLoggerNameLogger.setAdditive(true);
            // 设置日志等级为WARN
            clientLoggerNameLogger.setLevel(Level.WARN);
            defaultMQProducer.setInstanceName("product");
            return defaultMQProducer;
        }

        @Bean
        public MessageProducerService messageProducerService() {
            return new RocketMQMessageProducerServiceImpl();
        }

        @Bean
        @ConfigurationProperties(prefix = "lawu.mq.rocketmq")
        public DefaultMQPushConsumer defaultMQPushConsumer() {
            DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();
            defaultMQPushConsumer.setInstanceName("consumer");
            //设置为广播方式接受
            defaultMQPushConsumer.setMessageModel(MessageModel.CLUSTERING);
            /*
             * 设置Consumer第一次启动是从队列头部开始消费还是队列尾部开始消费
             * 如果非第一次启动，那么按照上次消费的位置继续消费
             */
            defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            return defaultMQPushConsumer;
        }

        @Bean
        @ConditionalOnMissingBean
        public MessageListenerConcurrently messageConsumerListener() {
            return new AbstractMessageConsumerListener() {
                @Override
                public void consumeMessage(String topic, String tags, Object message) {
                }
            };
        }

        @ConfigurationProperties(prefix = "lawu.mq.rocketmq")
        @Bean(initMethod = "createDeviceDatagramConsumer", destroyMethod = "shutdown")
        public MQConsumerFactory defaultMQConsumerFactory() {
            MQConsumerFactory defaultMQConsumerFactory = new MQConsumerFactory();
            return defaultMQConsumerFactory;
        }
    }
}
