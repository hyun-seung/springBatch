package com.system.batch.section2;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ZombieBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public ZombieBatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Tasklet zombieProcessCleanupTasklet() {
        return new ZombieProcessCleanupTasklet();
    }

    @Bean
    public Step zombieCleanupStep() {
        return new StepBuilder("zombieCleanupStep", jobRepository)
                // DB 트랜잭션을 관리하지 않는다면
                // DB 트랜잭션을 관리하는 PlatformTransactionManager 구현체 대신
                // ResourcelessTransactionManager 옵션 고려해볼 수 있다.
                /*
                    ResourcelessTransactionManager
                    -> no-op(아무것도 하지 않는) 방식으로 동작하는 PlatformTransactionManager 구현체.
                    -> 이를 사용하면 불필요한 DB 트랜잭션 처리를 생략할 수 있다.
                 */
//                .tasklet(zombieProcessCleanupTasklet(), new ResourcelessTransactionManager())
                // 위와 같이 인스턴스를 직접 생성해 전달해도 되지만,
                // 여러 스텝에서 재사용할 수 있도록 별도의 Bean 으로 정의할 경우 주의 필요.
                // -> Step 의 비즈니스 로직 처리를 위한 트랜잭션과 메타데이터 관리를 위한 트랜잭션이 서로 다른 성격임에도 불구하고
                //      같은 PlatformTransactionManager 빈을 사용하게 되어 의도치 않은 문제가 발생할 수 있다.
                .tasklet(zombieProcessCleanupTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job zombieCleanupJob() {
        return new JobBuilder("zombieCleanupJob", jobRepository)
                .start(zombieCleanupStep())     // Step 등록
                .build();
    }
}
