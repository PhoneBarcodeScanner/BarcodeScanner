package com.example.timbersmartbarcodescanner;

import android.annotation.TargetApi;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface  AreaDAO {

    @Query("SELECT * FROM Area") // probably not required...delete later
    List<Area> getAllAreas();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void insertArea(Area area);

    @Delete
    void delete(Area area);

    @Transaction
    @Query("SELECT * FROM area WHERE stocktake_id = :stocktakeIndex")
    List<Area> getAreasForStocktake(long stocktakeIndex);

   // @Query("SELECT COUNT(*) FROM area WHERE stocktake_id = :stocktakeIndex") // not gonna use .... //
   // int getNumAreasForStocktake(long stocktakeIndex); // returns number of areas in given stocktake

    @Query("UPDATE area SET are_pre_count = :preCount WHERE are_id = :index")
    int updatePreCount(long index, int preCount);

    @Query("UPDATE area SET are_num_of_barcodes = :numBarcodes WHERE are_id = :index")
    int updateNumOfBarcodes(long index, int numBarcodes);

    /*@Query("UPDATE area SET bcd_id = :barcodeID WHERE are_id = :index")
    int updateBarcodeID(long index, long barcodeID);    */

}
