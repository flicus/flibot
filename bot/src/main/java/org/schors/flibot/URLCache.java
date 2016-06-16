package org.schors.flibot;

import jersey.repackaged.com.google.common.cache.Cache;
import jersey.repackaged.com.google.common.cache.CacheBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sskoptsov on 16.06.2016.
 */
public class URLCache {

    private Map<String, Cache<String, String>> users = new HashMap<>();

    public URLCache() {
    }

    public String putNewURL(String userName, String url) {
        Cache<String, String> cache = users.getOrDefault(userName, CacheBuilder.newBuilder().maximumSize(1000).build());
        String id = Integer.toString(url.hashCode());
        cache.put(id, url);
        return id;
    }

    public String getURL(String userName, String id) {
        Cache<String, String> cache = users.get(userName);
        if (cache != null) {
            return cache.getIfPresent(id);
        }
        return null;
    }
}
