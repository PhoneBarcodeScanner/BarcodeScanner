package com.example.timbersmartbarcodescanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class AreaListAdapter extends ArrayAdapter {
    private static final String TAG = "AreaListAdapter";
    private Context mContext;
    private int mResource;

    /**
     *   Default Constructor for the StockTakeListAdapter
     *  @param context
     *  @param resource
     *  @param objects
     */
    public AreaListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Area> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Area location  = (Area) getItem(position);
        String Area, Date;
        Area = location.getAreaString();
        Date = location.getDate();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView textViewStockTakeName = convertView.findViewById(R.id.rowLocationText);
        textViewStockTakeName.setText(Area);

        TextView date =convertView.findViewById(R.id.rowEditDate);
        date.setText(Date);

        return convertView;
    }
}

