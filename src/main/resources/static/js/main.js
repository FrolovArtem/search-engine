function showTab(tabName) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById(tabName).classList.add('active');
    
    if (tabName === 'dashboard') loadStatistics();
}

async function loadStatistics() {
    const response = await fetch('/api/statistics');
    const data = await response.json();
    
    let html = '<div class="stat-item">';
    html += `<h3>Общая статистика</h3>`;
    html += `<p>Сайтов: ${data.statistics.total.sites}</p>`;
    html += `<p>Страниц: ${data.statistics.total.pages}</p>`;
    html += `<p>Лемм: ${data.statistics.total.lemmas}</p>`;
    html += `<p>Индексация: ${data.statistics.total.indexing ? 'В процессе' : 'Завершена'}</p>`;
    html += '</div>';
    
    data.statistics.detailed.forEach(site => {
        html += '<div class="stat-item">';
        html += `<h3>${site.name}</h3>`;
        html += `<p>URL: ${site.url}</p>`;
        html += `<p>Статус: ${site.status}</p>`;
        html += `<p>Страниц: ${site.pages}</p>`;
        html += `<p>Лемм: ${site.lemmas}</p>`;
        if (site.error) html += `<p>Ошибка: ${site.error}</p>`;
        html += '</div>';
    });
    
    document.getElementById('statistics').innerHTML = html;
}

async function startIndexing() {
    const response = await fetch('/api/startIndexing');
    const data = await response.json();
    alert(data.result ? 'Индексация запущена' : data.error);
}

async function stopIndexing() {
    const response = await fetch('/api/stopIndexing');
    const data = await response.json();
    alert(data.result ? 'Индексация остановлена' : data.error);
}

async function indexPage() {
    const url = document.getElementById('pageUrl').value;
    const response = await fetch('/api/indexPage?url=' + encodeURIComponent(url), {
        method: 'POST'
    });
    const data = await response.json();
    alert(data.result ? 'Страница добавлена в очередь' : data.error);
}

async function performSearch() {
    const query = document.getElementById('searchQuery').value;
    const site = document.getElementById('searchSite').value;
    
    let url = `/api/search?query=${encodeURIComponent(query)}`;
    if (site) url += `&site=${encodeURIComponent(site)}`;
    
    const response = await fetch(url);
    const data = await response.json();
    
    if (!data.result) {
        document.getElementById('searchResults').innerHTML = `<p>${data.error}</p>`;
        return;
    }
    
    let html = `<p>Найдено результатов: ${data.count}</p>`;
    data.data.forEach(item => {
        html += '<div class="search-item">';
        html += `<h3><a href="${item.site}${item.uri}" target="_blank">${item.title}</a></h3>`;
        html += `<p>${item.snippet}</p>`;
        html += `<p>Релевантность: ${item.relevance.toFixed(2)}</p>`;
        html += '</div>';
    });
    
    document.getElementById('searchResults').innerHTML = html;
}

// Загрузка статистики при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    loadStatistics();
});
