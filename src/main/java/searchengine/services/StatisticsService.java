package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repository.*;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexingService indexingService;
    
    public StatisticsResponse getStatistics() {
        List<SiteEntity> sites = siteRepository.findAll();
        
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.size());
        total.setIndexing(indexingService.isIndexing());
        
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        long totalPages = 0;
        long totalLemmas = 0;
        
        for (SiteEntity site : sites) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());
            item.setStatus(site.getStatus().toString());
            item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            item.setError(site.getLastError());
            
            long pages = pageRepository.countBySite(site);
            long lemmas = lemmaRepository.countBySite(site);
            
            item.setPages(pages);
            item.setLemmas(lemmas);
            
            totalPages += pages;
            totalLemmas += lemmas;
            
            detailed.add(item);
        }
        
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        
        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        
        return response;
    }
}
