package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exception.IndexingAlreadyStartedException;
import searchengine.exception.IndexingNotStartedException;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repository.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {
    
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final LemmaService lemmaService;
    private final PageIndexer pageIndexer;
    
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;
    
    public void startIndexing() {
        if (indexingInProgress.get()) {
            log.warn("Попытка запуска индексации, но она уже выполняется");
            throw new IndexingAlreadyStartedException("Индексация уже запущена");
        }
        
        log.info("Запуск полной индексации всех сайтов");
        indexingInProgress.set(true);
        
        new Thread(() -> {
            try {
                performIndexing();
            } catch (Exception e) {
                log.error("Ошибка при индексации: {}", e.getMessage(), e);
            } finally {
                cleanupResources();
            }
        }).start();
    }
    
    private void performIndexing() {
        log.info("Удаление старых данных из БД");
        deleteAllData();
        
        forkJoinPool = new ForkJoinPool();
        log.info("ForkJoinPool создан");
        
        for (Site site : sitesList.getSites()) {
            if (!indexingInProgress.get()) {
                log.info("Индексация остановлена пользователем");
                break;
            }
            log.info("Начало индексации сайта: {}", site.getUrl());
            indexSite(site);
        }
        log.info("Индексация всех сайтов завершена");
    }
    
    private void cleanupResources() {
        indexingInProgress.set(false);
        if (forkJoinPool != null) {
            forkJoinPool.shutdown();
            log.info("ForkJoinPool остановлен");
        }
    }
    
    public void stopIndexing() {
        if (!indexingInProgress.get()) {
            log.warn("Попытка остановки индексации, но она не запущена");
            throw new IndexingNotStartedException("Индексация не запущена");
        }
        
        log.info("Остановка индексации по запросу пользователя");
        indexingInProgress.set(false);
        if (forkJoinPool != null) {
            forkJoinPool.shutdownNow();
            log.info("ForkJoinPool принудительно остановлен");
        }
    }
    
    public boolean isIndexing() {
        return indexingInProgress.get();
    }
    
    private void deleteAllData() {
        log.debug("Удаление индексов");
        indexRepository.deleteAll();
        log.debug("Удаление лемм");
        lemmaRepository.deleteAll();
        log.debug("Удаление страниц");
        pageRepository.deleteAll();
        log.debug("Удаление сайтов");
        siteRepository.deleteAll();
        log.info("Все данные успешно удалены");
    }
    
    private void indexSite(Site site) {
        log.info("Создание записи для сайта: {}", site.getUrl());
        SiteEntity siteEntity = createSiteEntity(site);
        
        try {
            log.info("Загрузка robots.txt для сайта: {}", site.getUrl());
            RobotsTxtParser robotsTxtParser = new RobotsTxtParser(site.getUrl());
            
            log.info("Запуск обхода сайта: {}", site.getUrl());
            Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
            
            SiteIndexingTask task = new SiteIndexingTask(
                site.getUrl(),
                siteEntity,
                pageRepository,
                lemmaRepository,
                indexRepository,
                lemmaService,
                pageIndexer,
                indexingInProgress,
                visitedUrls,
                robotsTxtParser
            );
            
            forkJoinPool.invoke(task);
            
            updateSiteStatus(siteEntity, IndexingStatus.INDEXED, null);
            log.info("Сайт {} успешно проиндексирован", site.getUrl());
            
        } catch (Exception e) {
            log.error("Ошибка при индексации сайта {}: {}", site.getUrl(), e.getMessage(), e);
            updateSiteStatus(siteEntity, IndexingStatus.FAILED, e.getMessage());
        }
    }
    
    private SiteEntity createSiteEntity(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(IndexingStatus.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteRepository.save(siteEntity);
    }
    
    private void updateSiteStatus(SiteEntity siteEntity, IndexingStatus status, String error) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        if (error != null) {
            siteEntity.setLastError(error);
        }
        siteRepository.save(siteEntity);
    }
}
