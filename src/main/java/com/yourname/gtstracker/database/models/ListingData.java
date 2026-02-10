package com.yourname.gtstracker.database.models;

public abstract class ListingData {
    protected String id;
    protected String seller;
    protected int price;
    protected long firstSeen;
    protected long lastSeen;
    protected ListingStatus status;
    protected DataSource sourceFirst;
    protected DataSource sourceLast;

    public abstract ListingType getType();
    public abstract String getDisplayName();
    public abstract String getSearchKey();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public DataSource getSourceFirst() {
        return sourceFirst;
    }

    public void setSourceFirst(DataSource sourceFirst) {
        this.sourceFirst = sourceFirst;
    }

    public DataSource getSourceLast() {
        return sourceLast;
    }

    public void setSourceLast(DataSource sourceLast) {
        this.sourceLast = sourceLast;
    }
}
