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
public interface StocktakeDAO {

    @Transaction
    @Query("SELECT * FROM Stocktake")
    List<Stocktake> getAllStocktakes();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void insertStocktake(Stocktake stocktake); // returns -1 if row isn't inserted

    @Delete
    void delete(Stocktake stocktake);

    @Query("UPDATE stocktake SET stk_num_of_areas = :numAreas WHERE stk_id = :index")
    int updateNumOfAreas(long index, int numAreas); // updated whenever a new area is added

    @Query("UPDATE stocktake SET stk_date_mod = :modDate WHERE stk_id = :index")
    int updateDateModified(long index, String modDate); // updated whenever a new area is added

   /* @Query(("UPDATE stocktake SET are_id = :areaID WHERE stk_id = :index"))
    int updateAreaID(long index, long areaID);  */

}
