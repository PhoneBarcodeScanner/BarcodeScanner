package com.example.timbersmartbarcodescanner;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface BarcodeDAO {

    @Query("SELECT * FROM Barcode") //  not required...delete later
    List<Barcode> getAllBarcodes();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void insertBarcode(Barcode barcode);

    @Delete
    void delete(Barcode barcode);

    @Transaction
    @Query("SELECT * FROM barcode WHERE area_id = :areaIndex")
    List<Barcode> getBarcodesForArea(long areaIndex);

    @Query("SELECT * FROM barcode WHERE bcd_string = :barcodeString AND area_id = :areaIndex")
    Barcode getBarcodeByString(String barcodeString, long areaIndex); // each barcode should be unique

    /////// not using this, will add up individual barcode counts ////// delete later
    @Query("SELECT COUNT(*) FROM barcode WHERE area_id = :areaIndex")
    int getBarcodeCountForArea(long areaIndex); // returns number of barcodes in given area
    ///////////////////////////////////////////////////////

    @Query("UPDATE barcode SET bcd_date_time = :barcodeDate WHERE bcd_id = :barcodeID")
    int updateBarcodeDate(String barcodeDate, long barcodeID); // to do with duplicates

    @Query("UPDATE barcode SET bcd_count = :barcodeCount WHERE bcd_id = :barcodeID")
    int updateBarcodeCount(int barcodeCount, long barcodeID); // to do with duplicates

    @Query("UPDATE barcode SET bcd_bmap_id = :bitmapID WHERE bcd_id = :barcodeID")
    int updateBitmapID(String bitmapID, long barcodeID); // to do with duplicates

}
