package com.system.batch.section2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemDestructionConfig {

    @Bean
    public Job systemDestructionJob2(
            JobRepository jobRepository,
            Step systemDestructionStep
    ) {
        return new JobBuilder("systemDestructionJob", jobRepository)
                .validator(new DefaultJobParametersValidator(
                        new String[]{"destructionPower"},   // 필수 파라미터
                        new String[]{"targetSystem"}        // 선택적 파라미터
                        /*
                            만약 선택적 파라미터 배열에 값을 지정했다면 (빈 배열이 아닌 경우)
                            입력되는 모든 파라미터는 반드시 필수 파라미터나 선택적 파라미터 중 하나에 포함되어 있어야 한다.
                         */
                ))
                .start(systemDestructionStep)
                .build();
    }

    @Bean
    public Job systemDestructionJob(
            JobRepository jobRepository,
            Step systemDestructionStep,
            SystemDestructionValidator validator
    ) {
        return new JobBuilder("systemDestructionJob", jobRepository)
                .validator(validator)
                .start(systemDestructionStep)
                .build();
    }

    @Bean
    public Step systemDestructionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet systemDestructionTasklet
    ) {
        return new StepBuilder("systemDestructionStep", jobRepository)
                .tasklet(systemDestructionTasklet, transactionManager)
                .build();
    }
}
