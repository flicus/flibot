package org.schors.eva;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by flicus on 14.05.16.
 */
public class Entry {
    private String updated;
    private String id;
    private String title;
    private String author;
    private List<Link> links = new ArrayList<>();

    public Entry(String updated, String id, String title, String author) {
        this.updated = updated;
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public Entry() {
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "updated='" + updated + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", links=" + links +
                '}';
    }
}
