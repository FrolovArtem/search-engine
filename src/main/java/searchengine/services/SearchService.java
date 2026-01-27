package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.*;
import searchengine.model.*;
import searchengine.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        log.info("Поисковый запрос: '{}', сайт: {}, offset: {}, limit: {}", query, siteUrl, offset, limit);
        
        SearchResponse response = validateQuery(query);
        if (response != null) {
            return response;
        }
        
        Map<String, Integer> queryLemmas = lemmaService.getLemmas(query);
        log.debug("Извлечено {} лемм из запроса", queryLemmas.size());
        
        if (queryLemmas.isEmpty()) {
            return createEmptyResponse();
        }
        
        List<SiteEntity> sites = getSites(siteUrl);
        if (sites.isEmpty()) {
            return createErrorResponse("Сайт не проиндексирован");
        }
        
        List<SearchItem> results = performSearch(sites, queryLemmas);
        return createPaginatedResponse(results, offset, limit);
    }
    
    private SearchResponse validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Получен пустой поисковый запрос");
            return createErrorResponse("Задан пустой поисковый запрос");
        }
        return null;
    }
    
    private SearchResponse createEmptyResponse() {
        log.warn("Не удалось извлечь леммы из запроса");
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(0);
        response.setData(new ArrayList<>());
        return response;
    }
    
    private SearchResponse createErrorResponse(String error) {
        SearchResponse response = new SearchResponse();
        response.setResult(false);
        response.setError(error);
        return response;
    }
    
    private List<SearchItem> performSearch(List<SiteEntity> sites, Map<String, Integer> queryLemmas) {
        List<SearchItem> results = new ArrayList<>();
        
        for (SiteEntity site : sites) {
            log.debug("Поиск на сайте: {}", site.getUrl());
            results.addAll(searchInSite(site, queryLemmas));
        }
        
        results.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));
        log.info("Найдено {} результатов", results.size());
        
        return results;
    }
    
    private SearchResponse createPaginatedResponse(List<SearchItem> results, int offset, int limit) {
        int toIndex = Math.min(offset + limit, results.size());
        List<SearchItem> paginatedResults = results.subList(
            Math.min(offset, results.size()), 
            toIndex
        );
        
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(results.size());
        response.setData(paginatedResults);
        return response;
    }
    
    private List<SiteEntity> getSites(String siteUrl) {
        if (siteUrl != null && !siteUrl.isEmpty()) {
            return siteRepository.findByUrl(siteUrl)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        return siteRepository.findAll();
    }
    
    private List<SearchItem> searchInSite(SiteEntity site, Map<String, Integer> queryLemmas) {
        long totalPages = pageRepository.countBySite(site);
        if (totalPages == 0) {
            log.debug("На сайте {} нет страниц", site.getUrl());
            return Collections.emptyList();
        }
        
        List<LemmaEntity> lemmas = filterAndSortLemmas(site, queryLemmas, totalPages);
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<PageEntity> pages = findPagesWithAllLemmas(lemmas);
        if (pages.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<PageEntity, Double> relevanceMap = calculateRelevance(pages, lemmas);
        
        return relevanceMap.entrySet().stream()
                .map(entry -> createSearchItem(entry.getKey(), entry.getValue(), queryLemmas))
                .collect(Collectors.toList());
    }
    
    private List<LemmaEntity> filterAndSortLemmas(SiteEntity site, Map<String, Integer> queryLemmas, long totalPages) {
        int frequencyThreshold = (int) (totalPages * 0.8);
        log.debug("Порог частоты для сайта {}: {} (80% от {} страниц)", 
                  site.getUrl(), frequencyThreshold, totalPages);
        
        List<LemmaEntity> lemmas = new ArrayList<>();
        for (String lemmaText : queryLemmas.keySet()) {
            Optional<LemmaEntity> lemmaOpt = lemmaRepository.findBySiteAndLemma(site, lemmaText);
            if (lemmaOpt.isPresent()) {
                LemmaEntity lemma = lemmaOpt.get();
                if (lemma.getFrequency() <= frequencyThreshold) {
                    lemmas.add(lemma);
                    log.debug("Лемма '{}' добавлена (частота: {})", lemmaText, lemma.getFrequency());
                } else {
                    log.debug("Лемма '{}' отфильтрована как слишком частая (частота: {} > {})", 
                              lemmaText, lemma.getFrequency(), frequencyThreshold);
                }
            }
        }
        
        if (lemmas.isEmpty()) {
            log.debug("Все леммы отфильтрованы как слишком частые");
            return Collections.emptyList();
        }
        
        lemmas.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        log.debug("Леммы отсортированы по частоте: {}", 
                  lemmas.stream().map(l -> l.getLemma() + "(" + l.getFrequency() + ")")
                        .collect(Collectors.joining(", ")));
        
        return lemmas;
    }
    
    private Set<PageEntity> findPagesWithAllLemmas(List<LemmaEntity> lemmas) {
        if (lemmas.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<PageEntity> pages = indexRepository.findByLemma(lemmas.get(0))
                .stream()
                .map(IndexEntity::getPage)
                .collect(Collectors.toSet());
        
        for (int i = 1; i < lemmas.size(); i++) {
            Set<PageEntity> currentPages = indexRepository.findByLemma(lemmas.get(i))
                    .stream()
                    .map(IndexEntity::getPage)
                    .collect(Collectors.toSet());
            pages.retainAll(currentPages);
        }
        
        return pages;
    }
    
    private Map<PageEntity, Double> calculateRelevance(Set<PageEntity> pages, List<LemmaEntity> lemmas) {
        Map<PageEntity, Double> relevanceMap = new HashMap<>();
        
        for (PageEntity page : pages) {
            double absoluteRelevance = 0.0;
            
            for (LemmaEntity lemma : lemmas) {
                List<IndexEntity> indexes = indexRepository.findByPageAndLemma(page, lemma);
                for (IndexEntity index : indexes) {
                    absoluteRelevance += index.getRank();
                }
            }
            
            relevanceMap.put(page, absoluteRelevance);
        }
        
        double maxRelevance = relevanceMap.values().stream()
                .max(Double::compare)
                .orElse(1.0);
        
        relevanceMap.replaceAll((page, value) -> value / maxRelevance);
        
        return relevanceMap;
    }
    
    private SearchItem createSearchItem(PageEntity page, double relevance, Map<String, Integer> queryLemmas) {
        SearchItem item = new SearchItem();
        item.setSite(page.getSite().getUrl());
        item.setSiteName(page.getSite().getName());
        item.setUri(page.getPath());
        item.setRelevance(relevance);
        
        String content = page.getContent();
        org.jsoup.nodes.Document doc = Jsoup.parse(content);
        
        String title = doc.title();
        if (title.isEmpty()) {
            title = "Без названия";
        }
        item.setTitle(title);
        
        String text = doc.body().text();
        String snippet = generateSnippet(text, queryLemmas);
        item.setSnippet(snippet);
        
        return item;
    }
    
    private String generateSnippet(String text, Map<String, Integer> queryLemmas) {
        List<String> relevantSentences = findRelevantSentences(text, queryLemmas);
        String snippet = String.join(". ", relevantSentences);
        snippet = highlightQueryWords(snippet, queryLemmas);
        
        return truncateSnippet(snippet, 300);
    }
    
    private List<String> findRelevantSentences(String text, Map<String, Integer> queryLemmas) {
        String[] sentences = text.split("\\. ");
        List<String> relevantSentences = new ArrayList<>();
        
        for (String sentence : sentences) {
            if (containsQueryLemma(sentence, queryLemmas)) {
                relevantSentences.add(sentence);
                if (relevantSentences.size() >= 3) {
                    break;
                }
            }
        }
        
        if (relevantSentences.isEmpty() && sentences.length > 0) {
            relevantSentences.add(sentences[0]);
        }
        
        return relevantSentences;
    }
    
    private boolean containsQueryLemma(String sentence, Map<String, Integer> queryLemmas) {
        Map<String, Integer> sentenceLemmas = lemmaService.getLemmas(sentence);
        return sentenceLemmas.keySet().stream()
                .anyMatch(queryLemmas::containsKey);
    }
    
    private String highlightQueryWords(String text, Map<String, Integer> queryLemmas) {
        for (String lemma : queryLemmas.keySet()) {
            text = highlightLemma(text, lemma);
        }
        return text;
    }
    
    private String truncateSnippet(String snippet, int maxLength) {
        if (snippet.length() > maxLength) {
            return snippet.substring(0, maxLength - 3) + "...";
        }
        return snippet;
    }
    
    private String highlightLemma(String text, String lemma) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^а-яa-zА-ЯA-Z]", "").toLowerCase();
            Map<String, Integer> wordLemmas = lemmaService.getLemmas(cleanWord);
            
            if (wordLemmas.containsKey(lemma)) {
                result.append("<b>").append(word).append("</b> ");
            } else {
                result.append(word).append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
