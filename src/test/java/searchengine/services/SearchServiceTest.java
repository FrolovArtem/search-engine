package searchengine.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.dto.search.SearchResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchServiceTest {
    
    @Autowired
    private SearchService searchService;
    
    @Test
    void testSearchWithEmptyQueryReturnsError() {
        SearchResponse response = searchService.search("", null, 0, 20);
        
        assertNotNull(response);
        assertFalse(response.isResult());
        assertEquals("Задан пустой поисковый запрос", response.getError());
    }
    
    @Test
    void testSearchWithNullQueryReturnsError() {
        SearchResponse response = searchService.search(null, null, 0, 20);
        
        assertNotNull(response);
        assertFalse(response.isResult());
        assertEquals("Задан пустой поисковый запрос", response.getError());
    }
    
    @Test
    void testSearchWithValidQueryReturnsResponse() {
        SearchResponse response = searchService.search("тест", null, 0, 20);
        
        assertNotNull(response);
        assertTrue(response.isResult());
        assertNotNull(response.getData());
        assertTrue(response.getCount() >= 0);
    }
    
    @Test
    void testSearchWithOffsetAndLimit() {
        SearchResponse response = searchService.search("тест", null, 0, 10);
        
        assertNotNull(response);
        assertTrue(response.isResult());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() <= 10);
    }
}
