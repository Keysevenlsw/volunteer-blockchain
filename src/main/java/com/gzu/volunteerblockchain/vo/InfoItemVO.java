package com.gzu.volunteerblockchain.vo;

public class InfoItemVO {

    private String title;
    private String tag;
    private String date;

    public InfoItemVO() {
    }

    public InfoItemVO(String title, String tag, String date) {
        this.title = title;
        this.tag = tag;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
