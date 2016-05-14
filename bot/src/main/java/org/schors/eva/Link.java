package org.schors.eva;

/**
 * Created by flicus on 14.05.16.
 */
public class Link {
    private String href;
    private String type;
    private String title;
    private String rel;

    public Link(String href, String type, String title, String rel) {
        this.href = href;
        this.type = type;
        this.title = title;
        this.rel = rel;
    }

    public Link() {
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Link{" +
                "href='" + href + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", rel='" + rel + '\'' +
                '}';
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

}
