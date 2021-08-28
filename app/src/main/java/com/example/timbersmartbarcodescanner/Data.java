package com.example.timbersmartbarcodescanner;

import java.io.Serializable;
import java.util.ArrayList;


// This is a singleton class which will hold all the data for each stock take.
// This was decided to avoid passing bundles across many activities.

public class Data implements Serializable {

    private static final String FILENAME = "notused.txt";
    private static String FILE_NAME = "timbersmart.txt";
    private static Data mData = null;
    private ArrayList<Stocktake> mStocktakeList;
    private int imageIdCount;


    // Attaches a list of stocktakes to the data class
    // Used when reading in stocktakes from file
    public void setStocktakeList(ArrayList<Stocktake> stocktakeList) {
        mStocktakeList = stocktakeList;
    }


    // Adds new stocktake to the front of the list
    // This is done so that when the list is displayed, the most recently added is at the top
    public void addStocktake(Stocktake stocktake) {
        mStocktakeList.add(0,stocktake);
    }

    // When creating a Data object, if it does not exist, it is created
    // Otherwise nothing happens
    private Data(){
        if (mData == null){
            mStocktakeList = new ArrayList<Stocktake>();
            imageIdCount = 1;
        }
    }

    public void setImageIdCount(int imageIdCount) {
        this.imageIdCount = imageIdCount;
    }

    public int getImageIdCount() {
        return imageIdCount;
    }

    // Used to access Data Class values on different screens,
    // If function is called when no Data object is created,
    // Then a new blank Data object is created, otherwise previously created Data object is returned
    public static Data getDataInstance() {
        if (mData == null){
            mData = new Data();
        }
        return mData;
    }

    // Used to populate listview in Activity Main
    public ArrayList<Stocktake> getStocktakeList() {
        return mStocktakeList;
    }



    // Used to get selected stocktake on areaScreen
    // Position determined by which item in ListView is clicked
    public Stocktake getStockTake(int i) {
        return mStocktakeList.get(i);
    }

    /*
     *   Rules for saving data:
     *   START-OF-TIMBER-SMART-DATA
     *       STOCKTAKE-START
     *            "StockTake 1"
     *            "StockTake Start Date"
     *            "StockTake Most Recent Edit"
     *            AREAS-START
     *                 "Area 1 Name"
     *                 "Area 1 Date"
     *                 BARCODE-START
     *                      "BarcodeValue"
     *                      "BarcodeDate"
     *                      "BardcodeArea"
     *                 BARCODE-END
     *            AREAS-END
     *       STOCKTAKE-END
     *   END-OF-TIMBER-SMART-DATA
     */

    // Used in when saving Data Class to file
    // Turns data into text that can be read by readfile function
  /*  public String ToString() {
        StringBuilder data;
        data = new StringBuilder("\"START-OF-TIMBER-SMART-DATA\"");
        data.append("\"").append(imageIdCount).append("\""); //current count for unique barcode image id
        for (int i=0; i<mStocktakeList.size(); i++) {
            data.append("\"Stock-take-start\"");
            data.append("\"").append(mStocktakeList.get(i).getStocktakeString()).append("\"");
            data.append("\"").append(mStocktakeList.get(i).getDateCreated()).append("\"");
            data.append("\"").append(mStocktakeList.get(i).getDateModified()).append("\"");
            for (int j = 0; j<mStocktakeList.get(i).getAreaList().size(); j++){
                data.append("\"Area-start\"");
                data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getAreaString()).append("\"");
                data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getDate()).append("\"");
                data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getPreCount()).append("\"");
                for (int k = 0; k<mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().size(); k++){
                    data.append("\"Barcode-start\"");
                    data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().get(k).getBarcode()).append("\"");
                    data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().get(k).getDateTime()).append("\"");
                    data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().get(k).getArea()).append("\"");
                    data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().get(k).getCount()).append("\"");
                    data.append("\"").append(mStocktakeList.get(i).getAreaList().get(j).getBarcodeList().get(k).getBitmapIdArrayToString()).append("\"");
                    data.append("\"Barcode-end\"");
                }
                data.append("\"Area-end\"");
            }
            data.append("\"Stock-take-end\"");
        }
        data.append("\"END-OF-TIMBER-SMART-DATA\"");
        return data.toString();
    }*/
}
