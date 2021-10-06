package com.example.timbersmartbarcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/*
*   159.333 - Programming Project Semester 2, 2021
*   TimberSmart Phone Barcode scanner
*
*   Student Name / Student ID:
*       Runyu Luo (17217478)
*       Caitlin Winterburn (19028948)
*       Mohammed Shareef (19032353)
*       Seungwoon Yang (21008279)
*/

public class ActivityMain extends AppCompatActivity implements Serializable {

    private static final String FILE_NAME = "timbersmart.txt";
    private static final String TAG = "ActivityMainDebug";

    private BarcodeScannerDB barcodeScannerDB;
    private AreaDAO areaDAO;
    private BarcodeDAO barcodeDAO;
    private StocktakeDAO stocktakeDAO;

    private StocktakeListAdapter mStocktakeListAdapter;
    private ListView mListView;
    private TextView title,mainTitle;
    private Button mAddNewStocktake;
    private EditText mNewStocktakeName;
    private MenuItem mi, help;
    private ImageView iv;
    private Toolbar toolbar_stock_screen;
    private LinearLayout mHintLayoutTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        barcodeScannerDB = BarcodeScannerDB.getDatabaseInstance(this);
        areaDAO = barcodeScannerDB.areaDao();
        barcodeDAO = barcodeScannerDB.barcodeDao();
        stocktakeDAO = barcodeScannerDB.stocktakeDao();

        if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.CAMERA}, 1);
        else {
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        mi = menu.findItem(R.id.client);
        help = menu.findItem(R.id.help);

        Boolean checkSelect = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE).getBoolean("Stock take",false);
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
            case R.id.client:
                SharedPreferences sp = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                if(item.isChecked())
                {
                    item.setChecked(false);
                    editor.putBoolean("Stock take", false);
                    Toast.makeText(this,"Allow Stocktake Duplication False",Toast.LENGTH_SHORT).show();
                }else{
                    item.setChecked(true);
                    editor.putBoolean("Stock take", true);
                    Toast.makeText(this,"Allow Stocktake Duplication True",Toast.LENGTH_SHORT).show();
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        // check all permissions have been granted
        boolean granted = true;
        for(int result: grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        if(granted) {
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BarcodeScannerDB.closeDatabase();
    }

    private void init() throws Exception {
        //Set up views -------------------------------

        mainTitle = findViewById(R.id.titleas);
        title = findViewById(R.id.ActivityMainStockTakeNameRowTitle);
        mListView = findViewById(R.id.ActivityMainListViewStocktakes);
        mAddNewStocktake = findViewById(R.id.ActivityMainAddNewStocktake);
        mNewStocktakeName = findViewById(R.id.ActivityMainEditStocktake);
        mHintLayoutTab = findViewById(R.id.emptyTab);
        toolbar_stock_screen=(Toolbar)findViewById(R.id.StockScreenToolBar);
        setSupportActionBar(toolbar_stock_screen);
        title.setTag(new Boolean(false));
        title.setOnClickListener(view -> {
            title.animate().scaleX(0.95f).scaleYBy(-0.95f).alpha(0).setDuration(150).withEndAction(()->{
                title.animate().scaleX(1f).scaleY(1f).alpha(1).setDuration(150);
            });
            try {
                if (stocktakeDAO.getAllStocktakes().size() > 0) {
                    if((Boolean) title.getTag()) {
                        title.setTag(new Boolean(false));
                        iv.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
                        Collections.sort(stocktakeDAO.getAllStocktakes(), new Comparator<Stocktake>() {
                            @Override
                            public int compare(Stocktake t1, Stocktake t2) {
                                return - t1.getStocktakeName().compareTo(t2.getStocktakeName());
                            }
                        });
                    }else{
                        title.setTag(new Boolean(true));
                        iv.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
                        Collections.sort(stocktakeDAO.getAllStocktakes(), new Comparator<Stocktake>() {
                            @Override
                            public int compare(Stocktake t1, Stocktake t2) {
                                return t1.getStocktakeName().compareTo(t2.getStocktakeName());
                            }
                        });
                    }
                    update();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });


        try {
            mStocktakeListAdapter = new StocktakeListAdapter(this, R.layout.listview_main, new ArrayList<>(stocktakeDAO.getAllStocktakes()));
            mListView.setAdapter(mStocktakeListAdapter);
            mListView.setOnItemClickListener((adapterView, view, i, l)->{
                    try {
                        StockTakeViewButtonClick(view.findViewById(R.id.ActivityMainButtonView));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        checkIfThereAreAnyStocktakes();

        mAddNewStocktake.setOnClickListener(view -> {
            String newStocktakeName = mNewStocktakeName.getText().toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Date date = new Date();
            try {
                if (stocktakeDAO.getAllStocktakes().size() == 0){
                    if(newStocktakeName.equals("")){
                        Toast.makeText(this, "Field is empty. Please enter a name for the Stocktake", Toast.LENGTH_SHORT).show();
                    } else {
                        stocktakeDAO.insertStocktake(new Stocktake(newStocktakeName, dateFormat.format(date),
                                dateFormat.format(date), 0));
                        mStocktakeListAdapter.notifyDataSetChanged();
                        mListView.invalidateViews();
                        mNewStocktakeName.setText("");
                        mStocktakeListAdapter = new StocktakeListAdapter(this, R.layout.listview_main, new ArrayList<>(stocktakeDAO.getAllStocktakes()));
                        mListView.setAdapter(mStocktakeListAdapter);
                        mListView.invalidateViews();
                    }
                }
                else {
                        boolean unique = true;
                        for (int i = 0; i < stocktakeDAO.getAllStocktakes().size(); i++) {
                            if (newStocktakeName.equals("")) {
                                unique = false;
                                break;
                            }
                            if (stocktakeDAO.getAllStocktakes().get(i).getStocktakeName().equals(newStocktakeName)) {
                                unique = false;
                                break;
                            }
                        }
                        if (unique || mi.isChecked() && !"".equals(newStocktakeName)) {
                            stocktakeDAO.insertStocktake(new Stocktake(newStocktakeName, dateFormat.format(date),
                                    dateFormat.format(date), 0));
                            mStocktakeListAdapter = new StocktakeListAdapter(this, R.layout.listview_main, new ArrayList<>(stocktakeDAO.getAllStocktakes()));
                            mListView.setAdapter(mStocktakeListAdapter);
                            mListView.invalidateViews();
                            mNewStocktakeName.setText("");
                            update();
                            title.setTag(new Boolean(false));
                            iv.setImageBitmap(null);
                            Toast.makeText(this,"Stock Added",Toast.LENGTH_SHORT).show();
                        } else if (newStocktakeName.equals("")) {
                            Toast.makeText(this, "Field is empty. Please enter a name for the Stocktake", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Name already in use. Please choose another", Toast.LENGTH_SHORT).show();
                        }
                }
                checkIfThereAreAnyStocktakes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            update();
        });
    }





    private void checkIfThereAreAnyStocktakes() {
        try {
            if (mStocktakeListAdapter.getCount() == 0) {
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


    // This function acts as the onClickListener for the view button found on each stock-take in the activity main screen,
    // it works by finding the index of which item is clicked, it then sends that index to the next screen (areas) so it knows which stock-take's
    // areas it should be editing. */
    public void StockTakeViewButtonClick(View view) throws Exception {
        LinearLayout parent = (LinearLayout) view.getParent();
        TextView child = (TextView)parent.getChildAt(0);
        String stockTakeClicked = child.getText().toString();

        int index=0;
        ArrayList<Stocktake> tempStocktakes = new ArrayList<>(stocktakeDAO.getAllStocktakes());
        for (int i=0;i<tempStocktakes.size(); i++){
            if (tempStocktakes.get(i).getStocktakeName().equals(stockTakeClicked)){
                index = i;
                break;
            }
        }

        // Passes an intent which holds the index of a stock take
        Intent intent = new Intent (ActivityMain.this, AreasScreen.class);
        intent.putExtra("Stocktake", index);
        startActivity(intent);
    }

    //this function is to delete the stocktake record
    public void StockTakeDeleteButtonClick(View view) throws Exception {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        LinearLayout parent = (LinearLayout) view.getParent();
                        TextView child = (TextView)parent.getChildAt(0);
                        String item = child.getText().toString();

                        Toast.makeText(ActivityMain.this, item +" Deleted", Toast.LENGTH_LONG).show();

                        if(mListView.getCount()<1){
                            Toast.makeText(ActivityMain.this, "hello", Toast.LENGTH_LONG).show();
                        }

                        try {
                            ArrayList<Stocktake> stocktakes = new ArrayList<>(stocktakeDAO.getAllStocktakes());
                            for (int n=0;n<stocktakes.size(); n++){
                                if (stocktakes.get(n).getStocktakeName().equals(item)){
                                    deleteAllChildren(stocktakes.get(n));
                                    stocktakeDAO.delete(stocktakes.get(n));
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
                        Toast.makeText(ActivityMain.this,"Record not deleted",Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void deleteAllChildren (Stocktake parentStocktake) { // delete all areas and barcodes of the stocktake
        ArrayList<Area> areasInStocktake = new ArrayList<>(areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()));
        for (Area area : areasInStocktake) {
            ArrayList<Barcode> barcodesInStocktake = new ArrayList<>(barcodeDAO.getBarcodesForArea(area.getAreaID()));
            for (Barcode barcode : barcodesInStocktake) { // delete all barcodes in each area in the parent stocktake
                barcodeDAO.delete(barcode);
            }
            areaDAO.delete(area);
        }
    }
    //this function is to return the stocktake

    //this function is to update the stocktake
    public void update(){
        StocktakeListAdapter temp = new StocktakeListAdapter(this, R.layout.listview_main, new ArrayList<>(stocktakeDAO.getAllStocktakes()));
        mStocktakeListAdapter = temp;
        mStocktakeListAdapter.notifyDataSetChanged();
        mListView.invalidateViews();
        checkIfThereAreAnyStocktakes();

    }

    // Exports Area name and the barcode assigned to that area as specified by TimberSmart
    // Export can be sent to google drive, email, etc.
    public void export(View view) throws Exception {

        View parentRow = (View) view.getParent();
        ListView listView = (ListView) parentRow.getParent();
        final int position = listView.getPositionForView(parentRow);

        StringBuilder data = new StringBuilder();
        data.append("Area, Barcode");

        // Get data from selected stocktake -- data is retrieved from database
        Stocktake stocktake = stocktakeDAO.getAllStocktakes().get(position);
        ArrayList<Area> areaList = new ArrayList<>(areaDAO.getAreasForStocktake(stocktake.getStocktakeID()));
        for(int i = 0; i < areaList.size(); i++) {
            Area area = areaList.get(i);
            ArrayList<Barcode> barcodeList = new ArrayList<>(barcodeDAO.getBarcodesForArea(area.getAreaID()));
            for(int j = 0; j < barcodeList.size(); j++) {
                Barcode barcode = barcodeList.get(j);
                data.append("\n" + area.getAreaName() + ',' + barcode.getBarcodeString());
            }
        }

        try {
            // Generating file name e.g. Stocktake_5.csv
            // Uses name to create file as well
            String fileSaveName = "Stocktake_" + stocktake.getStocktakeName() + ".csv";
            FileOutputStream out = openFileOutput(fileSaveName, Context.MODE_PRIVATE);
            out.write(data.toString().getBytes());
            out.close();

            // Exporting, allows user to choose preferred method of sharing
            Context context = getApplicationContext();
            File fileLocation = new File(getFilesDir(), fileSaveName);
            Uri path = FileProvider.getUriForFile(context, "com.example.timbersmartbarcodescanner.fileProvider", fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileSaveName);
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send Mail"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() { super.onPause(); }

    @Override
    protected void onResume(){
        super.onResume();
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
