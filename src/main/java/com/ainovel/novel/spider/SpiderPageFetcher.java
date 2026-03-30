package com.ainovel.novel.spider;

import java.io.IOException;
import org.jsoup.nodes.Document;

public interface SpiderPageFetcher {

    Document fetch(String url) throws IOException;
}
