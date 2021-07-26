package com.example.timbersmartbarcodescanner;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Stocktake implements Serializable{
    /*
    * mStocktakeString  -- Stocktakes name
    * mAreaList         -- List of areas belonging to stocktake
    * mDateCreated      -- Date stocktake was created
    * mDateModified     -- Date stocktake was last edited
     */
    private String mStocktakeString = "";
    private ArrayList<Area> mAreaList;
    private String mDateModified = "Not Currently Used";
    private String mDateCreated = "Not Currently Used";

    // Constructors
    public Stocktake(String stocktakeString) {
        mStocktakeString = stocktakeString;
        mAreaList = new ArrayList<>();
    }

    public Stocktake(String stocktakeString, ArrayList<Area> areaList) {
        mStocktakeString = stocktakeString;
        mAreaList = areaList;
    }

    public Stocktake(String name, String datecreated, String dmodified) {
        this.mStocktakeString = name;
        this.mDateCreated = datecreated;
        this.mDateModified = dmodified;
        mAreaList = new ArrayList<Area>();
    }

    // Getters and setters
    public String getStocktakeString() {
        return mStocktakeString;
    }

    public void setStocktakeString(String stockTakeString) {
        mStocktakeString = stockTakeString;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        mDateCreated = formatter.format(date);
        mDateModified = formatter.format(date);

    }
    public void setStocktakeName(String stocktakeString, ArrayList<Area> areaList) {
        mStocktakeString = stocktakeString;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        mDateCreated = formatter.format(date);
        mDateModified = formatter.format(date);
        mAreaList = areaList;
    }


    public ArrayList<Area> getAreaList() {
        return mAreaList;
    }
    public Area getAreaAtPosition(int i) {
        return mAreaList.get(i);
    }

    public void addArea(Area area) {
        mAreaList.add(0, area);
    }
    public void setAreaList(ArrayList<Area> areaList) {
        mAreaList = areaList;
    }

    public String getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(String dateCreated) {
        mDateCreated = dateCreated;
    }

    public String getDateModified() {
        return mDateModified;
    }

    public void setDateModified(String dateModified) {
        mDateModified = dateModified;
    }
}
