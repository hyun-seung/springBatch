package com.system.batch.section4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.RedisItemWriter;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.batch.item.redis.builder.RedisItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import static com.system.batch.section4.AttackModels.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HackerAttackAggregationJob {
    private final RedisConnectionFactory redisConnectionFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job aggregateHackerAttackJob(
            Step aggregateAttackStep,
            Step reportAttackStep,
            AttackCounter attackCounter
    ) {
        return new JobBuilder("aggregateHackerAttackJob", jobRepository)
                .start(aggregateAttackStep)
                .next(reportAttackStep)
                .listener(attackCounter)
                .build();
    }

//    @Bean
//    public Step aggregateAttackStep(
//            RedisItemReader<String, AttackLog> attackLogReader,
//            AttackCounterItemWriter attackCounterItemWriter
//    ) {
//        return new StepBuilder("aggregateAttackStep", jobRepository)
//                .<AttackLog, AttackLog>chunk(10, transactionManager)
//                .reader(attackLogReader)
//                .processor(item -> {
//                    log.info("{}", item);
//                    return item;
//                })
//                .writer(attackCounterItemWriter)
//                .build();
//    }

    @Bean
    public Step aggregateAttackStep(
            RedisItemReader<String, AttackLog> attackLogReader,
            AttackCounter attackCounter,
            RedisItemWriter<String, AttackLog> deleteAttackLogWriter
    ) {
        return new StepBuilder("aggregateAttackStep", jobRepository)
                .<AttackLog, AttackLog>chunk(10, transactionManager)
                .reader(attackLogReader)
                .processor(attackLog -> {
                    log.info("{}", attackLog);
                    attackCounter.record(attackLog);
                    return attackLog;
                })
                .writer(deleteAttackLogWriter)
                .build();
    }

    @Bean
    public RedisItemWriter<String, AttackLog> deleteAttackLogWriter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RedisTemplate<String, AttackLog> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, AttackLog.class));
        template.afterPropertiesSet();

        return new RedisItemWriterBuilder<String, AttackLog>()
                .redisTemplate(template)
                .itemKeyMapper(attackLog -> "attack:" + attackLog.getId())
                .delete(true)
                .build();
    }

    @Bean
    public Step reportAttackStep(AttackCounter attackCounter) {
        return new StepBuilder("reportAttackStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("{}", attackCounter.generateAnalysis());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public RedisItemReader<String, AttackLog> attackLogReader() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RedisTemplate<String, AttackLog> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, AttackLog.class));
        template.afterPropertiesSet();

        return new RedisItemReaderBuilder<String, AttackLog>()
                .redisTemplate(template)
                .scanOptions(ScanOptions.scanOptions()
                        .match("attack:*")
                        .count(10)
                        .build())
                .build();
    }

    @Component
    @RequiredArgsConstructor
    public static class AttackCounterItemWriter implements ItemWriter<AttackLog> {
        private final AttackCounter attackCounter;

        @Override
        public void write(Chunk<? extends AttackLog> chunk) {
            for (AttackLog attackLog : chunk) {
                attackCounter.record(attackLog);
            }
        }
    }
}
