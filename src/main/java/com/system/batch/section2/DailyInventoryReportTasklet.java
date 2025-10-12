package com.system.batch.section2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DailyInventoryReportTasklet implements Tasklet {

    private final AlimService alimService;
    private final InventoryRepository inventoryRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<ItemStock> lowStockItems = inventoryRepository.findLowStockItems(10);

        if (lowStockItems.isEmpty()) {
            log.info("✅ 모든 품목 재고 안정");
            return RepeatStatus.FINISHED;
        }

        StringBuilder message = new StringBuilder("⚠️ [재고 부족 품목 알림]\n");
        for (ItemStock item : lowStockItems) {
            message.append(String.format("- %s: 재고 %d개\n", item.getItemName(), item.getStock()));
        }

        log.info("📦 재고 부족 리포트 발송");
        alimService.send(message.toString());
        return RepeatStatus.FINISHED;
    }
}
