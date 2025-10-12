package com.system.batch.section2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Repository
public class InventoryRepository {

    public List<ItemStock> findLowStockItems(int cnt) {
        return IntStream.range(1, cnt)
                .mapToObj(i -> new ItemStock("itemName " + i, i))
                .collect(Collectors.toList());
    }
}
