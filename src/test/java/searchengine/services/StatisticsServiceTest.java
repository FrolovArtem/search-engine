package searchengine.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.dto.statistics.StatisticsResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatisticsServiceTest {
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Test
    void testGetStatisticsReturnsValidResponse() {
        StatisticsResponse response = statisticsService.getStatistics();
        
        assertNotNull(response);
        assertTrue(response.isResult());
        assertNotNull(response.getStatistics());
        assertNotNull(response.getStatistics().getTotal());
        assertNotNull(response.getStatistics().getDetailed());
    }
    
    @Test
    void testGetStatisticsTotalIsNotNull() {
        StatisticsResponse response = statisticsService.getStatistics();
        
        assertNotNull(response.getStatistics().getTotal());
        assertTrue(response.getStatistics().getTotal().getSites() >= 0);
        assertTrue(response.getStatistics().getTotal().getPages() >= 0);
        assertTrue(response.getStatistics().getTotal().getLemmas() >= 0);
    }
}
