package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageIndexingService {
    
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final PageIndexer pageIndexer;
    private final SitesList sitesList;
    
    public boolean indexPage(String url) {
        log.info("Запрос на индексацию страницы: {}", url);
        
        Site configSite = findSiteInConfig(url);
        if (configSite == null) {
            log.warn("Страница {} не принадлежит ни одному сайту из конфигурации", url);
            return false;
        }
        
        Optional<SiteEntity> siteEntityOpt = siteRepository.findByUrl(configSite.getUrl());
        if (siteEntityOpt.isEmpty()) {
            log.warn("Сайт {} не найден в базе данных", configSite.getUrl());
            return false;
        }
        
        SiteEntity site = siteEntityOpt.get();
        String path = url.replace(site.getUrl(), "");
        if (path.isEmpty()) {
            path = "/";
        }
        
        final String finalPath = path;
        Optional<PageEntity> existingPage = pageRepository.findBySiteAndPath(site, path);
        existingPage.ifPresent(page -> {
            log.debug("Удаление существующих индексов для страницы: {}", finalPath);
            indexRepository.findByPage(page).forEach(indexRepository::delete);
            pageRepository.delete(page);
        });
        
        try {
            log.debug("Загрузка страницы: {}", url);
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .execute();
            
            Document doc = response.parse();
            
            PageEntity page = new PageEntity();
            page.setSite(site);
            page.setPath(finalPath);
            page.setCode(response.statusCode());
            page.setContent(doc.html());
            page = pageRepository.save(page);
            log.debug("Страница сохранена с кодом: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                pageIndexer.indexPageContent(page, doc, site);
                log.info("Страница {} успешно проиндексирована", url);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Ошибка при индексации страницы {}: {}", url, e.getMessage(), e);
            return false;
        }
    }
    
    private Site findSiteInConfig(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return site;
            }
        }
        return null;
    }
}
