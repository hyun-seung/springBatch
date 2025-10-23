package com.system.batch.section2;

/*
    외부 클래스로 두어 실행할 경우
     ./gradlew bootRun --args='--spring.batch.job.name=terminatorJob questDifficulty=HARD'
 */
public enum QuestDifficulty {

    EASY,
    NORMAL,
    HARD,
    EXTREME
}
