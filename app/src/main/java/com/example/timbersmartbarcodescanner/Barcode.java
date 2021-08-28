package com.example.timbersmartbarcodescanner;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

import static androidx.room.ForeignKey.CASCADE;

@Entity (foreignKeys = {@ForeignKey(entity = Area.class,
        parentColumns = "are_id",
        childColumns = "area_id", onDelete = CASCADE)},
        indices = {@Index("area_id")})

public class Barcode {
    @PrimaryKey(autoGenerate = true)
    public long bcd_id;

    public long area_id; // column name is the same as field name - Foreign key

    @ColumnInfo(name = "bcd_string")
    public String barcodeString;

    @ColumnInfo(name = "bcd_date_time")
    public String barcodeDateTime;

    @ColumnInfo(name = "bcd_area_name")
    public String barcodeAreaName;

    @ColumnInfo(name = "bcd_count")
    public int barcodeCount; // records how many duplicate barcodes exist

    @ColumnInfo(name = "bcd_bmap_id")
    public String bitmapID;    // string will be used with "," to delimit between different bitmapIDs

    @Ignore // annotation to ignore field and not count it as part of the Barcode entity
    private static int imageIdCount = 1;        // used for bitmap ID generation....not part of database
    public static int getImageIdCount() { return imageIdCount; }
    public static void setImageIdCount(int count) { imageIdCount = count; }

    // getters for columns //
    long getBarcodeID() { return bcd_id; }
    String getBarcodeString() { return barcodeString; }
    String getBarcodeDateTime() { return barcodeDateTime; }
    String getBarcodeAreaName() { return barcodeAreaName; }
    int getBarcodeCount() { return barcodeCount; }
    String getBitmapID() { return bitmapID; }

    Barcode(long area_id, String barcodeString, String barcodeDateTime, int barcodeCount, String bitmapID) {
        this.area_id = area_id;
        this.barcodeString = barcodeString;
        this.barcodeDateTime = barcodeDateTime;
        this.barcodeCount = barcodeCount;
        this.bitmapID = bitmapID;

    }
}
