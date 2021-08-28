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

public class StocktakeListAdapter extends ArrayAdapter {
    private static final String TAG = "StockTakeListAdapter";
    private Context mContext;
    private int mResource;
    private ArrayList<Stocktake> mData;

    public StocktakeListAdapter(@NonNull Context context, int resource, ArrayList<Stocktake> stocktakes) {
        super(context, resource, stocktakes);
        this.mContext = context;
        this.mResource = resource;
        this.mData = stocktakes;
    }

    @Override
    public int getCount() {
        if(!mData.isEmpty()) {
            return mData.size();
        } else{
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if(mData.size() == 0) {
            return null;
        }
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Stocktake stocktake  = (Stocktake) getItem(position);
        String stocktakeName = stocktake.getStocktakeName();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView textViewStockTakeName = convertView.findViewById(R.id.ActivityMainTextViewStockTakeName);
        textViewStockTakeName.setText(stocktakeName);
        return convertView;
    }
}
