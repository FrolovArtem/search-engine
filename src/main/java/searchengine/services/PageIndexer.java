package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.model.*;
import searchengine.repository.*;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageIndexer {
    
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    
    /**
     * Индексирует содержимое страницы: извлекает леммы и сохраняет в индекс
     */
    public void indexPageContent(PageEntity page, Document doc, SiteEntity site) {
        String text = doc.text();
        Map<String, Integer> lemmas = lemmaService.getLemmas(text);
        
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaText = entry.getKey();
            Integer count = entry.getValue();
            
            LemmaEntity lemma = findOrCreateLemma(site, lemmaText);
            lemma.setFrequency(lemma.getFrequency() + 1);
            lemma = lemmaRepository.save(lemma);
            
            saveIndex(page, lemma, count);
        }
    }
    
    private LemmaEntity findOrCreateLemma(SiteEntity site, String lemmaText) {
        return lemmaRepository
                .findBySiteAndLemma(site, lemmaText)
                .orElseGet(() -> {
                    LemmaEntity newLemma = new LemmaEntity();
                    newLemma.setSite(site);
                    newLemma.setLemma(lemmaText);
                    newLemma.setFrequency(0);
                    return newLemma;
                });
    }
    
    private void saveIndex(PageEntity page, LemmaEntity lemma, Integer count) {
        IndexEntity index = new IndexEntity();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(count.floatValue());
        indexRepository.save(index);
    }
}
