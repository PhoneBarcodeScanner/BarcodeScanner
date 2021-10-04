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
import android.os.Environment;
import android.provider.Settings;
import android.renderscript.ScriptGroup;
import android.text.InputType;
import android.util.Log;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
            /*case R.id.check_duplicated:
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
                */

            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help Instruction")
                        .setMessage("'Allow Duplication' will allow the entering of duplicate data")
                        //.setMessage("The setting of Allow Duplication True/False will enable or disable adding same Barcode/Area/Stock take in the activity of Barcode/Area/Stock.")
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
                        ClientID = Integer.parseInt(input.getText().toString());
                        editor2.putInt("ClientID", ClientID);
                        editor2.commit();
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

                AlertDialog.Builder cBuilder2 = new AlertDialog.Builder(this);
                barcodePrefix = sp3.getString("BarcodePrefix", barcodePrefix);
                if(barcodePrefix == null){
                    cBuilder2.setTitle("No Prefix Set");
                } else {
                    cBuilder2.setTitle("Current Barcode Prefix: " + barcodePrefix);
                }
                final EditText input2 = new EditText(this);
                input2.setHint("Enter Barcode Prefix Filter...");
                input2.setInputType(InputType.TYPE_CLASS_TEXT);
                cBuilder2.setView(input2);

                cBuilder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        barcodePrefix = input2.getText().toString();
                        editor3.putString("BarcodePrefix", barcodePrefix);
                        editor3.commit();
                    }
                });

                cBuilder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                cBuilder2.show();

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
       /* try {
           writeFileOnInternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        BarcodeScannerDB.closeDatabase();
    }
///////
    private void init() throws Exception {
      /*  try {
            readFromFile();
        } catch (Exception e) {
            e.printStackTrace();
        }   */
        //Set up views -------------------------------

        mainTitle = findViewById(R.id.titleas);
        title = findViewById(R.id.ActivityMainStockTakeNameRowTitle);
        mListView = findViewById(R.id.ActivityMainListViewStocktakes);
        mAddNewStocktake = findViewById(R.id.ActivityMainAddNewStocktake);
        mNewStocktakeName = findViewById(R.id.ActivityMainEditStocktake);
        mHintLayoutTab = findViewById(R.id.emptyTab);
        // iv = findViewById(R.id.Stockimage);
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
        //--------------------------------------------------------------

        // Layout stuff----------------------------------------
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

        //----------------------------------------------------------------------------
        //on click listener for returning to home

        // Add addNew stocktake button onclick listener----------------
        //    StockTakeListAdapter finalStockTakeListAdapter = stockTakeListAdapter;
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
        //----------------------------------------------------------------------------
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
            // Generating file name e.g. Stocktake_5.csv
            // Uses name to create file as well
            SharedPreferences sh = getSharedPreferences("Timber Smart", MODE_PRIVATE);
            int id = sh.getInt("ClientID", ClientID);
            String fileSaveName = id+"_"+ stocktake.getStocktakeName() + ".csv";
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
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send Mail"));
            //Toast.makeText(this, "test", Toast.LENGTH_LONG ).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       /* try {
            writeFileOnInternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
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

   /* public void writeFileOnInternalStorage() throws Exception {
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

    private void readFromFile() throws Exception {
        File path = getApplicationContext().getExternalFilesDir(null);
        File file = new File(path, "my-file-name.txt");
        int length = (int) file.length();

        byte[] bytes = new byte[length];

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null) return;
        try {
            try {
                in.read(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Stocktake> tempArrayListStocktake = new ArrayList<Stocktake>();
        String contents = new String(bytes);
        String[] newContents = getQuotesString(contents);

        // Start of file read
        if (newContents[0].equals("START-OF-TIMBER-SMART-DATA")) {

            newContents = getQuotesString(newContents[1]); //is the uniqueImageIdCount
            Data.getDataInstance().setImageIdCount(Integer.parseInt(newContents[0]));

            newContents = getQuotesString(newContents[1]);
            // Start reading in stocktakes
            while (newContents[0].equals("Stock-take-start")) {

                    String[] StockTakeName = getQuotesString(newContents[1]);
                    String[] StockTakeDateCreated = getQuotesString(StockTakeName[1]);
                    String[] StockTakeDateModified = getQuotesString(StockTakeDateCreated[1]);
                    Stocktake tempStocktake = new Stocktake(StockTakeName[0], StockTakeDateCreated[0], StockTakeDateModified[0]);
                    newContents = getQuotesString(StockTakeDateModified[1]);

                    // Start reading in areas related to stocktake
                    while (newContents[0].equals("Area-start")) {
                        String[] AreaName = getQuotesString(newContents[1]);
                        String[] AreaDate = getQuotesString(AreaName[1]);
                        String[] AreaPreCount = getQuotesString(AreaDate[1]);
                        Area tempArea = new Area(AreaName[0], AreaDate[0], AreaPreCount[0]);
                        newContents = getQuotesString(AreaPreCount[1]);

//asd                   // Start reading in barcodes related to area
                        while (newContents[0].equals("Barcode-start")) {

                            String[] Barcode = getQuotesString(newContents[1]);
                            String[] BarcodeDate = getQuotesString(Barcode[1]);
                            String[] BarcodeArea = getQuotesString(BarcodeDate[1]);
                            String[] BarcodeCount = getQuotesString(BarcodeArea[1]);
                            String[] BarcodeBitmapId = getQuotesString(BarcodeCount[1]);
                            Barcode tempBarcode = new Barcode(Barcode[0], BarcodeDate[0], BarcodeArea[0], BarcodeCount[0], BarcodeBitmapId[0]);
                            tempArea.addBarcode(tempBarcode);

                            newContents = getQuotesString(BarcodeBitmapId[1]);
                            newContents = getQuotesString(newContents[1]);

                        }
                        tempStocktake.addArea(tempArea);
                        newContents = getQuotesString(newContents[1]);
                    }
                    tempArrayListStocktake.add(tempStocktake);
                    newContents = getQuotesString(newContents[1]);
            }
        }
        Data.getDataInstance().setStocktakeList(tempArrayListStocktake);
        mStocktakeListAdapter.notifyDataSetChanged();
    }

    /* This function will return the first item found in quotations as the first parameter
    * It returns the rest of the contents as a second parameter*/
   /* private String[] getQuotesString(String contents) {
        String[] newContents = new String[2];
        StringBuilder firstItem = new StringBuilder();
        int i = 1; //Starts at 1 to skip the first "
        while (contents.charAt(i) != '\"'){
            firstItem.append(contents.charAt(i));
            i++;
        }
        newContents[0] = firstItem.toString();
        newContents[1] = contents.substring(i+1);//+1 to remove the end "
        return newContents;
    } */
}
