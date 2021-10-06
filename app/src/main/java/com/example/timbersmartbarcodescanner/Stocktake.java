package com.example.timbersmartbarcodescanner;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class Stocktake {
    @PrimaryKey(autoGenerate = true)
    public long stk_id;

    @ColumnInfo(name = "stk_name")
    public String stocktakeName;

    @ColumnInfo(name = "stk_date_created")
    public String stocktakeDateCreated;

    @ColumnInfo(name = "stk_date_mod")
    public String stocktakeDateMod;

    @ColumnInfo(name = "stk_num_of_areas") // number of areas in a stocktake
    public int numOfAreas; // updated every time an area is added to a stocktake

    // getters for attributes //
    long getStocktakeID() { return stk_id; }
    String getStocktakeName() { return stocktakeName; }
    int getNumOfAreas() { return numOfAreas; }

    public Stocktake (String stocktakeName, String stocktakeDateCreated, String stocktakeDateMod, int numOfAreas) {
        this.stocktakeName = stocktakeName;
        this.stocktakeDateCreated = stocktakeDateCreated;
        this.stocktakeDateMod = stocktakeDateMod;
        this.numOfAreas = numOfAreas;
    }
}
