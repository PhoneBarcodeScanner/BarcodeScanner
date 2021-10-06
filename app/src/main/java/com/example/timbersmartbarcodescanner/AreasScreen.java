package com.example.timbersmartbarcodescanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

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

    private AreaDAO areaDAO;
    private StocktakeDAO stocktakeDAO;
    private Stocktake parentStocktake;

    private int ClientID = -1;
    private String barcodePrefix = null;



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
        //mi = menu.findItem(R.id.check_duplicated);
        help = menu.findItem(R.id.help);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {

           /* case R.id.check_duplicated:
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
                */

            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help Instruction")
                        .setMessage("'Allow Duplication' will allow the entering of duplicate data")
                        .setPositiveButton("OK",null)
                        .show();
                break;



            case R.id.client_ID_set:
                SharedPreferences sp2 = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor2 = sp2.edit();
                AlertDialog.Builder cBuilder = new AlertDialog.Builder(this);
                int current = sp2.getInt("ClientID", ClientID);

                if(current < 0){
                    cBuilder.setTitle("No Client ID Set");
                } else {
                    cBuilder.setTitle("Current Client ID: " + current);
                }

                final EditText input = new EditText(this);
                input.setHint("Enter new client ID...");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                cBuilder.setView(input);

                cBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().length() == 0){
                            dialog.cancel();
                            Context context = getApplicationContext();
                            CharSequence text = "Field Empty: Client ID Not Changed";
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        } else {
                            ClientID = Integer.parseInt(input.getText().toString());
                            editor2.putInt("ClientID", ClientID);
                            editor2.apply();
                            Toast.makeText(AreasScreen.this, "Client ID changed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                cBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                cBuilder.show();

                break;

            case R.id.barcode_prefix_filter:
                SharedPreferences sp3 = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor3 = sp3.edit();
                AlertDialog.Builder bBuilder = new AlertDialog.Builder(this);
                String current2 = sp3.getString("BarcodePrefix", barcodePrefix);

                if(current2 == null){
                    bBuilder.setTitle("No Barcode Prefix Set");
                } else {
                    bBuilder.setTitle("Current Barcode Prefix: " + current2);
                }

                final EditText input2 = new EditText(this);
                input2.setHint("Enter New Barcode Prefix Filter...");
                input2.setInputType(InputType.TYPE_CLASS_TEXT);
                bBuilder.setView(input2);

                bBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input2.getText().length() == 0){
                            dialog.cancel();
                            Context context = getApplicationContext();
                            CharSequence text = "Field Empty: Barcode Prefix Filter Not Changed";
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }else {
                            barcodePrefix = input2.getText().toString();
                            editor3.putString("BarcodePrefix", barcodePrefix);
                            editor3.apply();
                            Toast.makeText(AreasScreen.this, "Prefix setting changed",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                bBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                bBuilder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        barcodePrefix = null; // make barcode prefix equal null
                        editor3.remove("BarcodePrefix"); // remove setting from app
                        editor3.apply();
                        Toast.makeText(AreasScreen.this, "Prefix setting removed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                bBuilder.show();

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas_screen);
        areaDAO = BarcodeScannerDB.getDatabaseInstance(this).areaDao();
        stocktakeDAO = BarcodeScannerDB.getDatabaseInstance(this).stocktakeDao();


        // This screen constructs when we have clicked on a stocktake, so we need to retrieve
        // that specific stock take it is passed an integer of the index.
        // Called when returning from barcode screen
        Intent intent = getIntent();
        mPassedStockTakeIndex = intent.getIntExtra("Stocktake", -1);
        if (mPassedStockTakeIndex != -1) { // get stocktake object the areas belong to
            parentStocktake = stocktakeDAO.getAllStocktakes().get(mPassedStockTakeIndex);
        }
        asort = 0;

        mListView = findViewById(R.id.rowListView);
        mHintLayoutTab = findViewById(R.id.emptyTab);
        toolbar_scanning_screen=(Toolbar)findViewById(R.id.ScanningScreenToolBar);
        setSupportActionBar(toolbar_scanning_screen);

        try {
            mAreaListAdapter = new AreaListAdapter(this, R.layout.listview_areas_screen, new ArrayList<>(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID())));
            mListView.setAdapter(mAreaListAdapter);
            mListView.setOnItemClickListener((adapterView, view, i, l)->{
                try {
                    addBarcodesToArea(view.findViewById(R.id.rowAddButton));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            mArea_title=findViewById(R.id.area_title);
            mArea_title.setText(parentStocktake.getStocktakeName()+"’s Areas");

            mNewAreaName = findViewById(R.id.rowsAddAreaEdit);

            checkIfThereAreAnyAreas();

            // On-Click listener for "ADD NEW AREA" button at bottom of screen
            mAddNewArea = findViewById(R.id.rowAddButton);
            mAddNewArea.setOnClickListener(view -> {
                try {
                    boolean unique = true;
                    String areaName = mNewAreaName.getText().toString();

                    if(!mNewAreaName.getText().toString().equals("")){ // null string check
                        for (int i = 0; i < areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).size(); i++) {
                            if (areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(i).getAreaName().equals(areaName)){
                                unique = false; // duplicate entry
                            }
                        }

                        if (unique /*|| mi.isChecked() && !"".equals(areaName)*/) { // duplicate and null checks already done ^
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                            Date date = new Date();
                            Area mArea = new Area(parentStocktake.getStocktakeID(), areaName, dateFormat.format(date),
                                    0, 0);
                            areaDAO.insertArea(mArea);
                            mAreaListAdapter = new AreaListAdapter(this, R.layout.listview_areas_screen, new ArrayList<>(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID())));
                            mListView.setAdapter(mAreaListAdapter);
                            update();
                            int temp = parentStocktake.getNumOfAreas() + 1; // avoiding the use of an sql query for speed
                            int temp1 = stocktakeDAO.updateNumOfAreas(parentStocktake.getStocktakeID(), temp);
                            temp1 = stocktakeDAO.updateDateModified(parentStocktake.getStocktakeID(), dateFormat.format(date));
                            mNewAreaName.setText("");
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
                update();
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
                        Collections.sort(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return - t1.getAreaName().compareTo(t2.getAreaName());
                            }
                        });
                    }else{
                        mArea.setTag(new Boolean(true));
                        iv1.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
                        Collections.sort(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return t1.getAreaName().compareTo(t2.getAreaName());
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
                        Collections.sort(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return - t1.getAreaDate().compareTo(t2.getAreaDate());
                            }
                        });
                    }else{
                        mDate.setTag(new Boolean(true));
                        iv2.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
                        Collections.sort(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()), new Comparator<Area>() {
                            @Override
                            public int compare(Area t1, Area t2) {
                                return t1.getAreaDate().compareTo(t2.getAreaDate());
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

    //placeholder for no areas//
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


    @Override
    protected void onPause() {
        super.onPause();
        BarcodeScannerDB.closeDatabase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        BarcodeScannerDB.closeDatabase();
    }

    // On-Click listener assigned to ListViews "Add Barcode" Button
    // When clicked will open scanning screen in context to area clicked
    public void addBarcodesToArea(View view) throws Exception {
        LinearLayout parent = (LinearLayout) view.getParent();
        TextView child = (TextView)parent.getChildAt(0);
        String item = child.getText().toString();
        int areaIndex=0;
        for (int i = 0; i <areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).size(); i++) {
            if (areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(i).getAreaName().equals(item)) {
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
                            for (int n = 0; n <areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).size(); n++){
                                if (areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(n).getAreaName().equals(item)){
                                    // delete area from area table and update relevant parent stocktake fields
                                    deleteAllChildren(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(n));
                                    areaDAO.delete(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(n));
                                    mAreaListAdapter = new AreaListAdapter(AreasScreen.this, R.layout.listview_areas_screen, new ArrayList<>(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID())));
                                    mListView.setAdapter(mAreaListAdapter);
                                    update();
                                    int temp = parentStocktake.getNumOfAreas() - 1; // not using an sql query for speed
                                    int temp2 =stocktakeDAO.updateNumOfAreas(parentStocktake.getStocktakeID(), temp);
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                    Date date = new Date();
                                    temp2 = stocktakeDAO.updateDateModified(parentStocktake.getStocktakeID(), dateFormat.format(date));

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
    private void deleteAllChildren(Area parentArea) {
        BarcodeDAO barcodeDAO = BarcodeScannerDB.getDatabaseInstance(this).barcodeDao();
        ArrayList<Barcode> barcodesInStocktake = new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID()));
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File bitmapDirectory = cw.getDir("bitmap_", Context.MODE_PRIVATE);

        for (Barcode barcode : barcodesInStocktake) { // delete all barcodes in each area in the parent stocktake
            Barcode.deleteAllBitmaps(barcode, bitmapDirectory); // static method to delete all bitmaps from storage
            barcodeDAO.delete(barcode);
        }
    }



    public void update(){
        mAreaListAdapter = new AreaListAdapter(this, R.layout.listview_areas_screen, new ArrayList<>(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID())));
        mAreaListAdapter.notifyDataSetChanged();
        mListView.invalidateViews();
        checkIfThereAreAnyAreas();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BarcodeScannerDB.closeDatabase();
    }
}