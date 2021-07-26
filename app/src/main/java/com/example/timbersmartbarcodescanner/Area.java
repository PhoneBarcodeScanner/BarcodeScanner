package com.example.timbersmartbarcodescanner;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Area implements Serializable {
    private ArrayList<Barcode> mBarcodeList;
    private String mAreaString, mDate;
    private int mPreCount;


    public Area(String areaString) {
        mAreaString = areaString;
        //SimpleDateFormat formatter =  new SimpleDateFormat("dd/MM/yyyy HH:mm")
        SimpleDateFormat formatter =  new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        mDate = formatter.format(date);
        mBarcodeList = new ArrayList<>();
        mPreCount = 0;
    }

    public Area(String name, String date, String preCount) {
        mAreaString = name;
        mDate = date;
        mBarcodeList = new ArrayList<Barcode>();
        mPreCount = Integer.parseInt(preCount);
    }

    public String getAreaString() { return mAreaString; }
    public void setAreaString(String areaString) {
        mAreaString = areaString;
    }

    public String getDate() {
        return mDate;
    }
    public void setDate(String mDate) {
        this.mDate = mDate;
    }

    public ArrayList<Barcode> getBarcodeList() {
        return mBarcodeList;
    }
    public void setBarcodeList(ArrayList<Barcode> barcodeList) {
        mBarcodeList = barcodeList;
    }

    public void addBarcode(Barcode barcode) { mBarcodeList.add(0, barcode); }

    public int getPreCount() {
        return mPreCount;
    }

    public void setPreCount(int preCount) {
     mPreCount = preCount;
    }
}
