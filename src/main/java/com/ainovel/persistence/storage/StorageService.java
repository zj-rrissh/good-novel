package com.ainovel.persistence.storage;

import java.io.InputStream;

public interface StorageService {

    String upload(String path, InputStream inputStream, long contentLength, String contentType);

    String signedUrl(String path);
}
