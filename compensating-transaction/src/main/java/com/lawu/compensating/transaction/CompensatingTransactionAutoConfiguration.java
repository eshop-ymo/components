package com.lawu.compensating.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.lawu.compensating.transaction.CompensatingTransactionAutoConfiguration.TransactionScheduledJobAutoConfiguration;
import com.lawu.compensating.transaction.job.TransactionScheduledJob;
import com.lawu.compensating.transaction.properties.TransactionProperties;
import com.lawu.compensating.transaction.properties.TransactionProperties.TransactionJob;
import com.lawu.compensating.transaction.service.CacheService;
import com.lawu.compensating.transaction.service.FollowTransactionRecordService;
import com.lawu.compensating.transaction.service.impl.CacheServiceImpl;
import com.lawu.compensating.transaction.service.impl.FollowTransactionRecordServiceImpl;
import com.lawu.compensating.transaction.service.impl.TransactionStatusServiceImpl;

/**
 * RocketMQ自动配置类
 * 
 * @author jiangxinjun
 * @createDate 2017年12月22日
 * @updateDate 2017年12月22日
 */
@ConditionalOnProperty(name = {"lawu.compensating-transaction.enabled"}, havingValue="true", matchIfMissing = false)
@MapperScan({"com.lawu.compensating.transaction.mapper"})
@Configuration
@EnableConfigurationProperties({ TransactionProperties.class })
@Import({TransactionInitializing.class, TransactionScheduledJobAutoConfiguration.class})
public class CompensatingTransactionAutoConfiguration {
    
    @Bean
    public CacheService cacheService() {
        return new CacheServiceImpl();
    }
    
    @Bean
    public FollowTransactionRecordService followTransactionRecordService(DataSource dataSource, @Value("classpath:sql/follow_transaction_record.sql") Resource resource) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder();
        try (InputStream in = resource.getInputStream()) {
            byte[] tempbytes = new byte[1024];
            int len = 0;
            while ((len = in.read(tempbytes)) != -1) {
                sql.append(new String(tempbytes, 0, len, "UTF-8"));
            }
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
            preparedStatement.execute();
        }
        return new FollowTransactionRecordServiceImpl();
    }
    
    @ConditionalOnProperty(name = {"lawu.compensating-transaction.job.enabled"}, havingValue="true", matchIfMissing = false)
    @Configuration
    public static class TransactionScheduledJobAutoConfiguration {
        
        @Bean
        public TransactionStatusService transactionStatusService(DataSource dataSource, @Value("classpath:sql/transaction_record.sql") Resource resource) throws IOException, SQLException {
            StringBuilder sql = new StringBuilder();
            try (InputStream in = resource.getInputStream()) {
                byte[] tempbytes = new byte[1024];
                int len = 0;
                while ((len = in.read(tempbytes)) != -1) {
                    sql.append(new String(tempbytes, 0, len, "UTF-8"));
                }
            }
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
                preparedStatement.execute();
            }
            return new TransactionStatusServiceImpl();
        }
        
        @Bean
        public SimpleJob transactionScheduledJob() {
            return new TransactionScheduledJob(); 
        }
        
        @Bean(initMethod = "init")
        public JobScheduler simpleJobScheduler(ApplicationContext applicationContext, TransactionProperties properties) {
            ZookeeperRegistryCenter regCenter = null;
            try {
                regCenter = applicationContext.getBean(ZookeeperRegistryCenter.class);
            } catch (NoSuchBeanDefinitionException e) {
                return null;
            }
            TransactionJob transactionJob = properties.getJob();
            SimpleJob transactionScheduledJob = transactionScheduledJob();
            SimpleJobConfiguration jobCoreConfiguration = new SimpleJobConfiguration(JobCoreConfiguration.newBuilder("transactionScheduledJob1", "0/10 * * * * ?", 1).description("补偿事务").build(), transactionScheduledJob.getClass().getCanonicalName());
            LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration.newBuilder(jobCoreConfiguration).overwrite(true).disabled(transactionJob.getDisabled()).build();
            return new SpringJobScheduler(transactionScheduledJob(), regCenter, liteJobConfiguration);
        }
    }

}
