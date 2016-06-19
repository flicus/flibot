package org.schors.flibot;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by flicus on 19.06.16.
 */
public class Page {
    private String title = null;
    private List<Link> links = new ArrayList<>();
    private List<Entry> entries = new ArrayList<>();

    public Page() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
