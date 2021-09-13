package com.example.timbersmartbarcodescanner;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class BarcodeListAdapter extends ArrayAdapter {
    private static final String TAG = "BarcodeListAdapter";
    private Context mContext;
    private int mResource;
    private boolean duplicationEnabled;

    /**
     * Default Constructor for the StockTakeListAdapter
     *
     * @param context
     * @param resource
     * @param objects
     */
    public BarcodeListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Barcode> objects, boolean duplicationEn) {
        super(context, resource, objects);
        Log.i(TAG, "BarcodeListAdapter: objects==" + objects.size());
        this.mContext = context;
        this.mResource = resource;
        this.duplicationEnabled = duplicationEn;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);

        }

        Barcode barcode = (Barcode) getItem(position);
        String barcodeDetails = barcode.getBarcodeString();
        String dateTime = barcode.getBarcodeDateTime();
        //String area = barcode.getArea();
        String count = String.valueOf(barcode.getBarcodeCount());

        //Shrink date so it doesn't go offscreen
        TextView textViewDate = convertView.findViewById(R.id.SSLVDate);
        textViewDate.setText(dateTime);
        TextView textViewCount = convertView.findViewById(R.id.SSLVCount);
        textViewCount.setText("Count: " + count);

        TableRow tableRow = convertView.findViewById(R.id.SSLVTableRow);

        TextView textViewBarcode = convertView.findViewById(R.id.SSLVBarcode);
        textViewBarcode.setText(barcodeDetails);

        //TextView textViewArea = convertView.findViewById(R.id.SSLVArea);
        //textViewArea.setText(area);

        if (Integer.parseInt(count) > 1 /*&& !duplicationEnabled*/) {
            int colour = Color.argb(50, 200, 80, 80);
            tableRow.setBackgroundColor(colour);
        }
        if (Integer.parseInt(count) == 1) {
            int colour = Color.argb(50, 255, 255, 255);
            tableRow.setBackgroundColor(colour);
        }

        return convertView;
    }
}
