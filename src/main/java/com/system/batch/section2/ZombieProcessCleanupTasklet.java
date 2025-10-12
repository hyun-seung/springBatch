package com.system.batch.section2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class ZombieProcessCleanupTasklet implements Tasklet {

    private final int processToKill = 10;
    private int killedProcesses = 0;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        killedProcesses += 1;
        log.info("☠️ 프로세스 강제 종료... ({}/{})", killedProcesses, processToKill);

        if (killedProcesses >= processToKill) {
            log.info("💀 시스템 안정화 완료. 모든 좀비 프로세스 제거.");
            return RepeatStatus.FINISHED;
            /*
                다 끝났다. 이제 Step 을 종료해도 된다.
             */
        }

        return RepeatStatus.CONTINUABLE;
        /*
            작업 진행중, 추가 실행이 필요하다.

            RepeatStatus 가 필요한 이유 : 짧은 트랜잭션을 활용한 안전한 배치 처리
            -> Why no using While ?
             --> 100만건 삭제 중 80만건 삭제 시 예외 발생 시, 이미 처리했던 79만건 롤백.
            -> RepeatStatus.CONTINUABLE 로 반복한다면
             --> 매 만 건 처리마다 트랜잭션이 커밋되므로, 예외가 발생하더라도 79만의 데이터는 이미 안전한게 정리된 상태로 남는다.
         */
    }
}
