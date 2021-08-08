package com.example.timbersmartbarcodescanner;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;

//yo
public class AreasScreen extends AppCompatActivity implements Serializable {
    private static final String TAG = "AreasScreen";

    private AreaListAdapter mAreaListAdapter;
    private int mPassedStockTakeIndex, asort;
    private ListView mListView;
    private Button mAddNewArea;
    private EditText mNewAreaName;
    private TextView mArea_title, mArea, mDate;
    private ImageView iv1, iv2;
    private MenuItem mi, help;
    private Toolbar toolbar_scanning_screen;
    private LinearLayout mHintLayoutTab;



    public void goHome(View view){
        Intent intent = new Intent(this,ActivityMain.class);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        mi = menu.findItem(R.id.check_duplicated);
        help = menu.findItem(R.id.help);

        Boolean checkSelect = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE).getBoolean("Area",false);
        if(checkSelect){
            mi.setChecked(true);
        }else {
            mi.setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.check_duplicated:
                SharedPreferences sp = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                if(item.isChecked())
                {
                    item.setChecked(false);
                    editor.putBoolean("Area", false);
                    Toast.makeText(this,"Allow Area Duplication False",Toast.LENGTH_SHORT).show();
                }else{
                    item.setChecked(true);
                    editor.putBoolean("Area", true);
                    Toast.makeText(this,"Allow Area Duplication True",Toast.LENGTH_SHORT).show();
                }
                editor.apply();
                break;
            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help Instruction")
                        .setMessage("'Allow Duplication' will allow the entering of duplicate data")
                        //.setMessage("The setting of Allow Duplication True/False will enable or disable adding same Barcode/Area/Stock take in the activity of Barcode/Area/Stock.")
                        .setPositiveButton("OK",null)
                        .show();
                break;
            case R.id.bluetooth_connect:
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas_screen);

        // This screen constructs when we have clicked on a stocktake, so we need to retrieve
        // that specific stock take it is passed an integer of the index.
        // Called when returning from barcode screen
        Intent intent = getIntent();
        mPassedStockTakeIndex = intent.getIntExtra("Stocktake", -1);
        asort = 0;

        mListView = findViewById(R.id.rowListView);
        mHintLayoutTab = findViewById(R.id.emptyTab);
        toolbar_scanning_screen=(Toolbar)findViewById(R.id.ScanningScreenToolBar);
        setSupportActionBar(toolbar_scanning_screen);

        //feature for sorting on activity areas
      //  iv1 = findViewById(R.id.Areaimage);
     //   iv2 = findViewById(R.id.Dateimage2);

        try {
            mAreaListAdapter = new AreaListAdapter(this, R.layout.listview_areas_screen, getStocktakeFromData(mPassedStockTakeIndex).getAreaList());
            mListView.setAdapter(mAreaListAdapter);
            mListView.setOnItemClickListener((adapterView, view, i, l)->{
                try {
                    addBarcodesToArea(view.findViewById(R.id.rowAddButton));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            mArea_title=findViewById(R.id.area_title);
            mArea_title.setText(getStocktakeFromData(mPassedStockTakeIndex).getStocktakeString()+"’s Areas");

            mNewAreaName = findViewById(R.id.rowsAddAreaEdit);

            checkIfThereAreAnyAreas();

            // On-Click listener for "ADD NEW AREA" button at bottom of screen
            mAddNewArea = findViewById(R.id.rowAddButton);
            mAddNewArea.setOnClickListener(view -> {
                try {
                    boolean unique = true;
                    String areaName = mNewAreaName.getText().toString();

                    if(!mNewAreaName.getText().toString().equals("")){
                        for (int i = 0; i < getStocktakeFromData(mPassedStockTakeIndex).getAreaList().size(); i++) {
                            if (getStocktakeFromData(mPassedStockTakeIndex).getAreaList().get(i).getAreaString().equals(areaName)){
                                unique = false;
                            }
                        }

                        if (unique || mi.isChecked() && !"".equals(areaName)) {
                            Area mArea = new Area(areaName);
                            getStocktakeFromData(mPassedStockTakeIndex).addArea(mArea);
                            mNewAreaName.setText("");
                            update();
                            this.mArea.setTag(new Boolean(false));
                            mDate.setTag(new Boolean(false));
                            iv1.setImageBitmap(null);
                            iv2.setImageBitmap(null);
                            Toast.makeText(AreasScreen.this, "Area Added", Toast.LENGTH_LONG).show();
                            mNewAreaName.getText().clear();
                        }
                        else {
                            Toast.makeText(AreasScreen.this, areaName + " already exists, please use a different name", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(AreasScreen.this, "Field is empty. Please enter a name for the area", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });

            mArea = findViewById(R.id.rowLocation);
            mArea.setTag(new Boolean(false));
            mArea.setOnClickListener(v->{
                mArea.animate().scaleX(0.95f).scaleYBy(-0.95f).alpha(0).setDuration(150).withEndAction(()->{
                    mArea.animate().scaleX(1f).scaleY(1f).alpha(1).setDuration(150);
                });

                try{
                    iv2.setImageBitmap(null);
                    mDate.setTag(new Boolean(false));
                    if((Boolean) mArea.getTag()) {
                        mArea.setTag(new Boolean(false));
                        iv1.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
                        Collections.sort(getStocktakeFromData(mPassedStockTakeIndex).getAreaList(), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return - t1.getAreaString().compareTo(t2.getAreaString());
                            }
                        });
                    }else{
                        mArea.setTag(new Boolean(true));
                        iv1.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
                        Collections.sort(getStocktakeFromData(mPassedStockTakeIndex).getAreaList(), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return t1.getAreaString().compareTo(t2.getAreaString());
                            }
                        });
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                update();
            });

            mDate = findViewById(R.id.rowDate);
            mDate.setTag(new Boolean(false));
            mDate.setOnClickListener(v->{
                mDate.animate().scaleX(0.95f).scaleYBy(-0.95f).alpha(0).setDuration(150).withEndAction(()->{
                    mDate.animate().scaleX(1f).scaleY(1f).alpha(1).setDuration(150);
                });

                try{
                    iv1.setImageBitmap(null);
                    mArea.setTag(new Boolean(false));
                    if((Boolean) mDate.getTag()) {
                        mDate.setTag(new Boolean(false));
                        iv2.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
                        Collections.sort(getStocktakeFromData(mPassedStockTakeIndex).getAreaList(), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return - t1.getDate().compareTo(t2.getDate());
                            }
                        });
                    }else{
                        mDate.setTag(new Boolean(true));
                        iv2.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
                        Collections.sort(getStocktakeFromData(mPassedStockTakeIndex).getAreaList(), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return t1.getDate().compareTo(t2.getDate());
                            }
                        });
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                update();
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //placeholder for no areas
    private void checkIfThereAreAnyAreas() {
        try {
            if (mAreaListAdapter.getCount() == 0) {
                if (mHintLayoutTab.getVisibility() == View.GONE) {
                    // Change visibility to visible
                    mHintLayoutTab.setVisibility(View.VISIBLE);
                }
            } else {
                // Hide placeholder
                mHintLayoutTab.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Small function to help prevent long lines of code when accessing Data class
    public Stocktake getStocktakeFromData(int i) throws Exception {
        return Data.getDataInstance().getStocktakeList().get(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            writeFileOnInternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeFileOnInternalStorage() throws Exception {
        File path = getApplicationContext().getExternalFilesDir(null);
        File file = new File(path, "my-file-name.txt");
        FileOutputStream stream = new FileOutputStream(file);
        String stringToWriteInFile = Data.getDataInstance().ToString();
        try {
            stream.write(stringToWriteInFile.getBytes());
        } finally {
            stream.close();
        }
    }

    // On-Click listener assigned to ListViews "Add Barcode" Button
    // When clicked will open scanning screen in context to area clicked
    public void addBarcodesToArea(View view) throws Exception {
        LinearLayout parent = (LinearLayout) view.getParent();
        TextView child = (TextView)parent.getChildAt(0);
        String item = child.getText().toString();
        int areaIndex=0;
        for (int i = 0; i <getStocktakeFromData(mPassedStockTakeIndex).getAreaList().size(); i++) {
            if (getStocktakeFromData(mPassedStockTakeIndex).getAreaList().get(i).getAreaString().equals(item)) {
                areaIndex = i;
            }
        }
        Intent intent = new Intent(AreasScreen.this, ScanningScreen.class);
        intent.putExtra("Stocktake Index", mPassedStockTakeIndex);
        intent.putExtra("Area Index", areaIndex);
        startActivity(intent);
    }

    // On-Click listener assigned to ListViews "Delete Area" Button
    // Deletes selected area from areaList when clicked
    public void deleteArea(View view) throws Exception {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        LinearLayout parent = (LinearLayout) view.getParent();
                        TextView child = (TextView)parent.getChildAt(0);
                        String item = child.getText().toString();

                        Toast.makeText(AreasScreen.this, item +" Deleted", Toast.LENGTH_LONG).show();
                        try {
                            for (int n = 0; n <getStocktakeFromData(mPassedStockTakeIndex).getAreaList().size(); n++){
                                if (getStocktakeFromData(mPassedStockTakeIndex).getAreaList().get(n).getAreaString().equals(item)){
                                    getStocktakeFromData(mPassedStockTakeIndex).getAreaList().remove(n);
                                    update();
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(AreasScreen.this,"Record not deleted",Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    public void update(){
        mAreaListAdapter.notifyDataSetChanged();
        mListView.invalidateViews();
        checkIfThereAreAnyAreas();
    }
}