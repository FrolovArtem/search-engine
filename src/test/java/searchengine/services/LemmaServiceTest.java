package searchengine.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LemmaServiceTest {
    
    @Autowired
    private LemmaService lemmaService;
    
    @Test
    void testGetLemmasFromRussianText() {
        String text = "Повторное повторение слов";
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        assertNotNull(lemmas);
        assertFalse(lemmas.isEmpty());
        assertTrue(lemmas.containsKey("повтор"));
    }
    
    @Test
    void testGetLemmasFromEmptyText() {
        String text = "";
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        assertNotNull(lemmas);
        assertTrue(lemmas.isEmpty());
    }
    
    @Test
    void testGetLemmasIgnoresStopWords() {
        String text = "и в на с";
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        assertNotNull(lemmas);
        assertTrue(lemmas.isEmpty());
    }
    
    @Test
    void testGetLemmasCountsFrequency() {
        String text = "слово слово слово";
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        assertNotNull(lemmas);
        assertTrue(lemmas.containsKey("слов"));
        assertEquals(3, lemmas.get("слов"));
    }
    
    @Test
    void testGetLemmasIgnoresShortWords() {
        String text = "я ты он";
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        assertNotNull(lemmas);
        assertTrue(lemmas.isEmpty());
    }
}
