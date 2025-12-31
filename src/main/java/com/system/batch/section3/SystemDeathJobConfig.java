package com.system.batch.section3;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import static com.system.batch.section3.DeathNoteWriteJobConfig.*;

@Configuration
@RequiredArgsConstructor
public class SystemDeathJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    /*
        ./gradlew bootRun --args='--spring.batch.job.name=systemDeathJob inputFile=./system_death.jsonl'

        ./gradlew bootRun --args='--spring.batch.job.name=systemDeathJob inputFile=./pretty_system_death.jsonl'
     */
    @Bean
    public Job systemDeathJob(Step systemDeathStep) {
        return new JobBuilder("systemDeathJob", jobRepository)
                .start(systemDeathStep)
                .build();
    }

    @Bean
    public Step systemDeathStep(
//            FlatFileItemReader<SystemDeath> systemDeathReader
            JsonItemReader<SystemDeath> systemDeathReader
    ) {
        return new StepBuilder("systemDeathStep", jobRepository)
                .<SystemDeath, SystemDeath>chunk(10, transactionManager)
                .reader(systemDeathReader)
                .writer(items -> items.forEach(System.out::println))
                .build();
    }

    @Bean
    @StepScope
//    public FlatFileItemReader<SystemDeath> systemDeathReader(
    public JsonItemReader<SystemDeath> systemDeathReader(
            @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
//        return new FlatFileItemReaderBuilder<SystemDeath>()
//                .name("systemDeathReader")
//                .resource(new FileSystemResource(inputFile))
//                .lineMapper((line, lineNumber) -> objectMapper.readValue(line, SystemDeath.class))
//                .recordSeparatorPolicy(new JsonRecordSeparatorPolicy())
//                .build();
        return new JsonItemReaderBuilder<SystemDeath>()
                .name("systemDeathReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(SystemDeath.class))
                .resource(new FileSystemResource(inputFile))
                .build();
    }

    public record SystemDeath(String command, int cpu, String status) {}
}
