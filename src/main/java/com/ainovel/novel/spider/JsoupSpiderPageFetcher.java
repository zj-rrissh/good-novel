package com.ainovel.novel.spider;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class JsoupSpiderPageFetcher implements SpiderPageFetcher {

    private final NovelSpiderProperties properties;

    public JsoupSpiderPageFetcher(NovelSpiderProperties properties) {
        this.properties = properties;
    }

    @Override
    public Document fetch(String url) throws IOException {
        URL target = URI.create(url).toURL();
        String protocol = target.getProtocol();
        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
            return Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout(Math.toIntExact(properties.getRequestTimeout().toMillis()))
                    .get();
        }
        return Jsoup.parse(target, Math.toIntExact(properties.getRequestTimeout().toMillis()));
    }
}
