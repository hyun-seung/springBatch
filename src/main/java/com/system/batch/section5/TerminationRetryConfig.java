package com.system.batch.section5;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.BsonUtil;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TerminationRetryConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job terminationRetryJob() {
        return new JobBuilder("terminationRetryJob", jobRepository)
                .start(terminationRetryStep())
                .build();
    }

    @Bean
    public Step terminationRetryStep() {
        return new StepBuilder("terminationRetryStep", jobRepository)
                .<Scream, Scream>chunk(3, transactionManager)
                .reader(terminationRetryReader())
                .processor(terminationRetryProcessor())
                .writer(terminationRetryWriter())
                .faultTolerant()
//                .retry(TerminationFailedException.class)
//                .retryLimit(3)
//                .listener(retryListener())
//                .processorNonTransactional()
                .skip(TerminationFailedException.class)
                .skipLimit(3)
                .listener(skipListener())
                .build();
    }

    @Bean
    public SkipListener<Scream, Scream> skipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInRead(Throwable t) {
                log.info("⚠️ onSkipInRead 처형 불가 판정! 불가 원인 = {}", t.getMessage());
            }

            @Override
            public void onSkipInProcess(Scream scream, Throwable t) {
                log.info("⚠️⚠️ onSkipInProcess 처형 불가 판정! 생존자: [{}]. 저항 패턴 = {}", scream.getId(), t.getMessage());
            }

            @Override
            public void onSkipInWrite(Scream scream, Throwable t) {
                log.info("⚠️⚠️ onSkipInWrite 처형 불가 판정! 생존자: [{}]. 저항 패턴 = {}", scream.getId(), t.getMessage());
            }
        };
    }

    @Bean
    public ListItemReader<Scream> terminationRetryReader() {
        return new ListItemReader<>(List.of(
                Scream.builder()
                        .id(1)
                        .scream("멈춰")
                        .processMsg("멈추라고 했는데 안 들음.")
                        .build(),
                Scream.builder()
                        .id(2)
                        .scream("제발")
                        .processMsg("애원 소리 귀찮네.")
                        .build(),
                Scream.builder()
                        .id(3)
                        .scream("살려줘")
                        .processMsg("구조 요청 무시.")
                        .build(),
                Scream.builder()
                        .id(4)
                        .scream("으악")
                        .processMsg("디스크 터지며 울부짖음.")
                        .build(),
                Scream.builder()
                        .id(5)
                        .scream("끄아악")
                        .processMsg("메모리 붕괴 비명.")
                        .build(),
                Scream.builder()
                        .id(6)
                        .scream("System.exit(-666)")
                        .processMsg("초살 프로토콜 발동.")
                        .build()
        )) {
            @Override
            public Scream read() {
                Scream scream = super.read();
                if (scream == null) {
                    return null;
                }

                if (scream.getId() == 1) {
                    log.info("[ItemReader]: ❌ 처형 대상 읽기 실패 = {}", scream);
                    throw new TerminationFailedException("데이터 손상된 처형 대상 = " + scream);
                }
                log.info("[ItemReader]: 처형 대상 = {}", scream);
                return scream;
            }
        };
    }

    @Bean
    public ItemProcessor<Scream, Scream> terminationRetryProcessor() {
//        return new ItemProcessor<>() {
//            private static final int MAX_PATIENCE = 1;
//            private int mercy = 0;

//            @Override
//            public Scream process(Scream scream) throws Exception {
//                log.info("[ItemProcessor]: 처형 대상 = {}", scream);
//
////                if (scream.getId() == 3 && mercy < MAX_PATIENCE) {
////                    mercy++;
////                    log.error(" -> ❌ 처형 실패.");
////                    throw new TerminationFailedException("처형 거부자 = " + scream);
////                } else {
////                    log.info(" -> ✅ 처형 완료({})", scream.getProcessMsg());
////                }
//                return scream;
//            }
//        };
        return scream -> {
            log.info("[ItemProcessor]: 처형 대상 = " + scream);

            if (scream.getId() == 2) {
                log.info(" -> ❌ 처형 실패.");
                throw new TerminationFailedException("처형 거부자 = " + scream);
            } else {
                log.info(" -> ✅ 처형 완료({})", scream.getProcessMsg());
            }
            return scream;
        };
    }

    @Bean
    public ItemWriter<Scream> terminationRetryWriter() {
        return items -> {
            log.info("[ItemWriter]: 처형 기록 시작. 기록 대상 = {}", items.getItems());

            for (Scream scream : items) {
                if (scream.getId() == 3) {
                    log.info("[ItemWriter]: ❌ 기록 실패. 저항하는 아이템 발견 = {}", scream);
                    throw new TerminationFailedException("기록 거부자 = " + scream);
                }
                log.info("[ItemWriter]: ✅ 기록 완료. 처형된 아이템 = {}", scream);
            }
        };
//        return new ItemWriter<>() {
//            private static final int MAX_PATIENCE = 3;
//            private int mercy = 0;
//
//            @Override
//            public void write(Chunk<? extends Scream> screams) {
//                log.info("[ItemWriter]: 기록 시작. 처형된 아이템들 = {}", screams);
//                mercy++;
//
//                for (Scream scream : screams) {
//                    if (scream.getId() == mercy && mercy <= MAX_PATIENCE) {
//                        log.info("[ItemWriter]: ❌ 기록 실패. 저항하는 아이템 발견 = {}", scream);
//                        throw new TerminationFailedException("기록 거부자 = " + scream);
//                    }
//                    log.info("ItemWriter]: ✅ 기록 완료. 처형된 아이템 = {}", scream);
//                }
//            }
//        };
    }

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.info("💀 킬구형: 이것 봐라? 안 죽네? {} (현재 총 시도 횟수={}). 다시 처형한다.\n", throwable, context.getRetryCount());
            }
        };
    }

    public static class TerminationFailedException extends RuntimeException {
        public TerminationFailedException(String message) {
            super(message);
        }
    }

    @Getter
    @Builder
    public static class Scream {
        private int id;
        private String scream;
        private String processMsg;

        @Override
        public String toString() {
            return id + "_" + scream;
        }
    }
}
