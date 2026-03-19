package com.ai.knowledge.task;

import com.ai.knowledge.entity.HotRecord;
import com.ai.knowledge.mapper.HotRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class HotDataCleanupTask {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private HotRecordMapper hotRecordMapper;
    
    private static final String HOT_KEY_PREFIX = "hot:graph:";
    private static final String HOT_CATEGORY_PREFIX = "hot:category:";
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupHotData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        List<HotRecord> yesterdayRecords = hotRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HotRecord>()
                .eq(HotRecord::getRecordDate, yesterday)
        );
        
        for (HotRecord record : yesterdayRecords) {
            String key = HOT_KEY_PREFIX + record.getGraphId();
            Object scoreObj = redisTemplate.opsForValue().get(key);
            
            if (scoreObj != null) {
                Long score = Long.valueOf(scoreObj.toString());
                if (score > 0) {
                    redisTemplate.expire(key, 1, java.util.concurrent.TimeUnit.DAYS);
                } else {
                    redisTemplate.delete(key);
                }
            } else {
                redisTemplate.delete(key);
            }
        }
        
        HotRecord newRecord = new HotRecord();
        newRecord.setRecordDate(LocalDate.now());
        hotRecordMapper.insert(newRecord);
    }
}