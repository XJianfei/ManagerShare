package com.peter.parttime.managershare;

import java.io.Serializable;

public class Article implements Serializable {
    private static final long serialVersionUID = 1L;
    public Article() {
        title = content = lead = meta = path = null;
    }
    public Article(String title, String content, String lead, String meta, String path) {
        this.title = title;
        this.content = content;
        this.lead = lead;
        this.meta = meta;
        this.path = path;
    }

    public String title;
    public String content;
    public String lead;
    public String meta;
    public String path;
    public String image;
    public String comment;

    @Override
    public String toString() {
        return "" + title + ":" + path;
    }
}
