package com.example.timbersmartbarcodescanner;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity (foreignKeys = {@ForeignKey(entity = Stocktake.class,
        parentColumns = "stk_id",
        childColumns = "stocktake_id", onDelete = CASCADE)},
        indices ={@Index(value = { "stocktake_id"})})

public class Area {
    @PrimaryKey(autoGenerate = true)
    public long are_id;

    public long stocktake_id; // foreign key

   // public long bcd_id;
    @ColumnInfo(name = "are_name")
    public String areaName;

    @ColumnInfo(name = "are_date")
    public String areaDate;

    @ColumnInfo(name = "are_pre_count")
    public int areaPreCount;

    @ColumnInfo(name = "are_num_of_barcodes")
    public int numOfBarcodes;

    // getters for attributes //
    long getAreaID() { return are_id; }
    String getAreaName() { return areaName; }
    String getAreaDate() { return areaDate; }
    int getAreaPreCount() { return areaPreCount; }
    int getNumOfBarcodes() { return numOfBarcodes; }

    public Area (long stocktake_id, String areaName, String areaDate, int areaPreCount, int numOfBarcodes) {
        this.stocktake_id = stocktake_id;
        this.areaName = areaName;
        this.areaDate = areaDate;
        this.areaPreCount = areaPreCount;
        this.numOfBarcodes = numOfBarcodes;
    }
}
