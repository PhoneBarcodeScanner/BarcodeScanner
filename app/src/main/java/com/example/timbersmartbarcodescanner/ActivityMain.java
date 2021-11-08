package com.example.timbersmartbarcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.core.content.FileProvider;

import com.google.android.gms.common.api.Api;

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
 *    Student Name / Student ID:
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
    private int ClientID = -1;
    private String barcodePrefix = null;


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

    //Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        help = menu.findItem(R.id.help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            //Displays a dialog showing what can be done on the activity
            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help")
                        .setMessage("-Add Stocktake to add Areas\n-Delete Stocktake to remove its areas and barcodes" +
                                "\n-Export stocktake to CSV file using export icon ('Set Client ID' will add prefix to filename)")
                        //.setMessage("The setting of Allow Duplication True/False will enable or disable adding same Barcode/Area/Stock take in the activity of Barcode/Area/Stock.")
                        .setPositiveButton("OK",null)
                        .show();
                break;
            //Sets the client ID to append to the exported filename
            case R.id.client_ID_set:
                SharedPreferences sp2 = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE); //open shared preferences
                SharedPreferences.Editor editor2 = sp2.edit(); //create editor object
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
                            editor2.putInt("ClientID", ClientID); //store client ID in shared preferences under "ClientID" key
                            editor2.apply(); //apply changes
                            Toast.makeText(ActivityMain.this, "Client ID changed",
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
            //Filters out barcodes if they do not match the filter
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
                            Toast.makeText(ActivityMain.this, "Prefix setting changed",
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
                        Toast.makeText(ActivityMain.this, "Prefix setting removed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                bBuilder.show();

                break;

            case R.id.setting:
                // add menu item in menu.xml and link it
                startActivity(new Intent(this, SettingActivity.class));
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
    ///////
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

        // Add addNew stocktake button onclick listener----------------
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
                                dateFormat.format(date), 0)); //add stocktake to the database
                        mStocktakeListAdapter.notifyDataSetChanged(); //update listview to populate with new stocktake
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
                        if (newStocktakeName.equals("")) { // if string is null
                            unique = false;
                            break;
                        }
                        if (stocktakeDAO.getAllStocktakes().get(i).getStocktakeName().equals(newStocktakeName)) {
                            unique = false; // becomes false if stocktake name already exists
                            break;
                        }
                    }
                    if (unique /*|| mi.isChecked() && !"".equals(newStocktakeName)*/) { // don't need duplication and null string check twice
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
                    } else if (newStocktakeName.equals("")) { // checks for null string
                        Toast.makeText(this, "Field is empty. Please enter a name for the Stocktake", Toast.LENGTH_SHORT).show();
                    } else { // last case - duplicate entry
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
                                    mStocktakeListAdapter = new StocktakeListAdapter(ActivityMain.this, R.layout.listview_main, new ArrayList<>(stocktakeDAO.getAllStocktakes()));
                                    mListView.setAdapter(mStocktakeListAdapter);
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
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File bitmapDirectory = cw.getDir("bitmap_", Context.MODE_PRIVATE);

        for (Area area : areasInStocktake) {
            ArrayList<Barcode> barcodesInStocktake = new ArrayList<>(barcodeDAO.getBarcodesForArea(area.getAreaID()));
            for (Barcode barcode : barcodesInStocktake) { // delete all barcodes in each area in the parent stocktake
                Barcode.deleteAllBitmaps(barcode, bitmapDirectory);
                barcodeDAO.delete(barcode);
            }
            areaDAO.delete(area);
        }
    }

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
        data.append("Area, Barcode, Count");

        // Get data from selected stocktake -- data is retrieved from database
        Stocktake stocktake = stocktakeDAO.getAllStocktakes().get(position);
        ArrayList<Area> areaList = new ArrayList<>(areaDAO.getAreasForStocktake(stocktake.getStocktakeID()));
        for(int i = 0; i < areaList.size(); i++) {
            Area area = areaList.get(i);
            ArrayList<Barcode> barcodeList = new ArrayList<>(barcodeDAO.getBarcodesForArea(area.getAreaID()));
            for(int j = 0; j < barcodeList.size(); j++) {
                Barcode barcode = barcodeList.get(j);
                data.append("\n" + area.getAreaName() + ',' + barcode.getBarcodeString() + ',' + barcode.getBarcodeCount());
            }
        }

        try {
            // Generating file name, clientID_StocktakeName.csv
            SharedPreferences sh = getSharedPreferences("Timber Smart", MODE_PRIVATE);
            int id = sh.getInt("ClientID", ClientID);
            String fileSaveName = id+"_"+ stocktake.getStocktakeName() + ".csv"; //sets export filename using shared preferences stored value
            FileOutputStream out = openFileOutput(fileSaveName, Context.MODE_PRIVATE);
            out.write(data.toString().getBytes());
            out.close();
            // Exporting, allows user to choose preferred method of sharing
            Context context = getApplicationContext();
            File fileLocation = new File(getFilesDir(), fileSaveName);



            Uri path = FileProvider.getUriForFile(context, "com.example.timbersmartbarcodescanner.fileProvider", fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Phone_Scanner_Automated_CSV_"+id);
            fileIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"timberxchange@timbersmart.co.nz"}); //sets recipient to timbersmart to initialise exchange network
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send Mail"));

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
