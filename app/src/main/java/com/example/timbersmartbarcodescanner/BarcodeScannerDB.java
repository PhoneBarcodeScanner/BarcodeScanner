package com.example.timbersmartbarcodescanner;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Stocktake.class, Area.class, Barcode.class}, version = 1)
public abstract class BarcodeScannerDB extends RoomDatabase{

    public abstract StocktakeDAO stocktakeDao();
    public abstract AreaDAO areaDao();
    public abstract BarcodeDAO barcodeDao();

    private static BarcodeScannerDB instance = null;

    static BarcodeScannerDB getDatabaseInstance(final Context context) { // singleton design pattern
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), BarcodeScannerDB.class,
                    "barcodeDatabase").allowMainThreadQueries().build();
        }
        return instance;
    }

     static void closeDatabase() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }


}
