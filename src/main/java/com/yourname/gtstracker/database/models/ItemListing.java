package com.yourname.gtstracker.database.models;

public class ItemListing extends ListingData {
    private String itemName;
    private int quantity;

    @Override
    public ListingType getType() {
        return ListingType.ITEM;
    }

    @Override
    public String getDisplayName() {
        return itemName + (quantity > 1 ? " x" + quantity : "");
    }

    @Override
    public String getSearchKey() {
        return itemName == null ? "unknown" : itemName.toLowerCase();
    }

    public int getPricePerUnit() {
        return getPrice() / Math.max(1, quantity);
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
