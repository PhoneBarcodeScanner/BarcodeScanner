package com.example.timbersmartbarcodescanner;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Barcode implements Serializable {
    private String mBarcodeString = "";
    private String mDateTime = "";
    private String mArea = "";
    private String mCount = "";
    private ArrayList<Integer> mBitmapIdArrayList;

    //Constructors
    //new barcode from ScanningScreen.java
    public Barcode(String barcodeString, String area, int count, int bitmapId) {
        mBarcodeString = barcodeString;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        mDateTime = formatter.format(date);
        mArea = area;
        mCount = "" + count;
        mBitmapIdArrayList = new ArrayList<>();
        mBitmapIdArrayList.add(bitmapId);
    }

    //get barcode from txt file from ActivityMain.java
    public Barcode(String barcode, String date, String area, String count, String bitmapIds) {
        this.mBarcodeString = barcode;
        this.mDateTime = date;
        this.mArea = area;
        this.mCount = count;
        mBitmapIdArrayList = new ArrayList<>();
        String[] stringIds = bitmapIds.split(", ");
        for (int i = 0; i < stringIds.length; i++) {
            int id = Integer.parseInt(stringIds[i]);
            mBitmapIdArrayList.add(id);
        }
    }

    public String getCount() {
        return mCount;
    }

    public void setCount(String count) {
        mCount = count;
    }

    public String getArea() {
        return mArea;
    }

    public void setArea(String area) {
        mArea = area;
    }

    // Getters and setters
    public String getBarcode() {
        return mBarcodeString;
    }

    public void setBarcode(String barcode) {
        mBarcodeString = barcode;
    }

    public String getDateTime() {
        return mDateTime;
    }

    public void setDateTime(String dateTime) {
        mDateTime = dateTime;
    }

    public void setBitmapId(int mBitmapid) {
        this.mBitmapIdArrayList.add(mBitmapid);
    }

    public void deleteOneBitmapId(int index) {
        this.mBitmapIdArrayList.remove(index);
    }

    public ArrayList<Integer> getBitmapIdArrayList() {
        return mBitmapIdArrayList;
    }

    public String getBitmapIdArrayToString() {
        String idString = mBitmapIdArrayList.toString();
        return idString.substring(1,idString.length()-1); //returns 1,2,3 or 1 removes []
    }
}
