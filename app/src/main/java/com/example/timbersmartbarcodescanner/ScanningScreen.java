package com.example.timbersmartbarcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.example.timbersmartbarcodescanner.scan.BarcodeScannerActivity;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ScanningScreen extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "ScanningScreen";

    private Stocktake parentStocktake;
    private Area parentArea;
    private BarcodeDAO barcodeDAO;
    private AreaDAO areaDAO;
    private StocktakeDAO stocktakeDAO;

    private int mCountGlobal, mPreCountGlobal;
    private TextView mCount, mDifference, mArea_title, BarcodeText, mt_Barcode, mt_Row, mt_Count, mt_Date;
    private EditText mBarcode, mPreCount;
    private Button mEnter, mConfirmPreCount, search;
    private ListView mListView;
    private BarcodeListAdapter mBarcodeListAdapter;
    private int mPassedAreaIndex, mPassedStocktakeIndex;
    private Toolbar toolbar_scanning_screen;
    private ImageView iv1, iv2, iv3, iv4;
    private MenuItem mi, help;
    private boolean duplicationEnabled = true;
    private String compareBarcode;
    private boolean justReadBarcode;
    private int imageUniqueId, matchCount;
    private Bitmap videoCaptureBitmap;
    private File bitmapDirectory;
    private int ClientID = -1;
    private String barcodePrefix = null;
    private ConstraintLayout mPlaceholder;

    /**
     * @param mReceivedVideoDataListener received video data listener.
     */
    protected TextureView mVideoSurface = null;
    private boolean isScanRunning;

    private SwitchCompat scScan;
    private View vScan;
    private Button btnScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning_screen);
        toolbar_scanning_screen = (Toolbar) findViewById(R.id.ScanningScreenToolBar);
        setSupportActionBar(toolbar_scanning_screen);
        mPlaceholder = findViewById(R.id.placeholder);
        barcodeDAO = BarcodeScannerDB.getDatabaseInstance(this).barcodeDao();
        areaDAO = BarcodeScannerDB.getDatabaseInstance(this).areaDao();
        stocktakeDAO = BarcodeScannerDB.getDatabaseInstance(this).stocktakeDao();
        //grab Area object from AreaScreen
        Intent intent = getIntent();
        mPassedAreaIndex = intent.getIntExtra("Area Index", -1);
        mPassedStocktakeIndex = intent.getIntExtra("Stocktake Index", -1);
        if (mPassedStocktakeIndex != -1) {
            parentStocktake = stocktakeDAO.getAllStocktakes().get(mPassedStocktakeIndex);
        }
        if (mPassedAreaIndex != -1) {
            parentArea = areaDAO.getAreasForStocktake(parentStocktake.getStocktakeID()).get(mPassedAreaIndex);
        }

        //get directory path to the bitmap storage location
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        bitmapDirectory = cw.getDir("bitmap_", Context.MODE_PRIVATE);

        try {
            //set title for scanning screen
            mArea_title = findViewById(R.id.textViewTitle);
            mArea_title.setText(parentArea.getAreaName() + "'s Barcodes");
            init();
            //Animation code for barcode titles removed from here
        } catch (Exception e) {
            e.printStackTrace();
        }

        checkForEmptyBarcodeList();
    }

    private void checkForEmptyBarcodeList () {
        try {
            if (mBarcodeListAdapter.getCount() == 0) {
                if (mPlaceholder.getVisibility() == View.GONE){
                    mPlaceholder.setVisibility(View.VISIBLE); // make placeholder visible
                    findViewById(R.id.codeDesc).setVisibility(View.INVISIBLE); // make barcode title invisible
                }
            } else { // barcodes exist, make placeholder invisible
                mPlaceholder.setVisibility(View.GONE);
                findViewById(R.id.codeDesc).setVisibility(View.VISIBLE); // make barcode title visible
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 0) {
            if (isScanRunning) {
                setupCamera();
            }
            if (resultCode != Activity.RESULT_OK || data == null) return;
            String[] barcodes = data.getStringArrayExtra(ScanningScreen.RESULT_DATA_BARCODES);
            int imageId = data.getIntExtra(ScanningScreen.RESULT_DATA_IMAGE_ID, -1);

            try {
                onBarcodesRead(barcodes, imageId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onBarcodesRead(String[] barcodes, int imageId) throws Exception {
        for (String barcode : barcodes) {
            saveBarcode(barcode, imageId);
        }
    }

    public void update() {
        mBarcodeListAdapter.notifyDataSetChanged();
        mListView.invalidateViews();
        checkForEmptyBarcodeList();
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
       // mi = menu.findItem(R.id.check_duplicated);
        help = menu.findItem(R.id.help);

        Boolean checkSelect = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE).getBoolean("Scanning", false);
    /*    if (checkSelect) {
            mi.setChecked(true);
        } else {
            mi.setChecked(false);
        } */
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.check_duplicated:
                SharedPreferences sp = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                if (item.isChecked()) {
                    if (duplicationEnabled == true) {
                        duplicationEnabled = false;
                    }
                    item.setChecked(false);
                    editor.putBoolean("Scanning", false);
                    Toast.makeText(this, "Allow Barcode Duplication False", Toast.LENGTH_SHORT).show();
                } else {
                    if (duplicationEnabled == false) {
                        duplicationEnabled = true;
                    }
                    item.setChecked(true);
                    editor.putBoolean("Scanning", true);
                    Toast.makeText(this, "Allow Barcode Duplication True", Toast.LENGTH_SHORT).show();
                }
                editor.apply();
                break;

             */

            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help Instruction")
                        .setMessage("'Allow Duplication' will allow the entering of duplicate data")
                        .setPositiveButton("OK", null)
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
                            Toast.makeText(ScanningScreen.this, "Client ID changed",
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
                            Toast.makeText(ScanningScreen.this, "Prefix setting changed",
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
                        Toast.makeText(ScanningScreen.this, "Prefix setting removed",
                                Toast.LENGTH_SHORT).show();

                    }
                });
                bBuilder.show();
                break;

            case R.id.setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void init() throws Exception {

        // Assigning views to variables
        mCount = findViewById(R.id.textViewCount);
        mDifference = findViewById(R.id.textViewDifference);
        mBarcode = findViewById(R.id.editTextBarcode);
        mPreCount = findViewById(R.id.editTextPreCount);
        mEnter = findViewById(R.id.buttonEnter);
       // mConfirmPreCount = findViewById(R.id.buttonConfirmPreCount);
        mListView = findViewById(R.id.ScanningScreenListView);
        search = findViewById(R.id.buttonSearch);
        mVideoSurface = findViewById(R.id.tvScan);
        scScan = findViewById(R.id.scScan);
        Boolean scScanState = scScan.isChecked();
        vScan = findViewById(R.id.vScan);
        BarcodeText = findViewById(R.id.codeDesc);

        mVideoSurface.setOnClickListener(v -> {
            //if (ScanningScreen.this.camera != null) {
            if(scScanState == false) {
                camera.close();
                camera = null;
            }
            startActivityForResult(
                    new Intent(this, BarcodeScannerActivity.class),
                    0);
        });

        vScan.setOnClickListener(v -> {
            isScanRunning = !isScanRunning;
            scScan.setChecked(isScanRunning);
            if (isScanRunning && ScanningScreen.this.camera == null) {
                scScan.setText("Scanning is currently enabled");
                setupCamera();
            } else if (ScanningScreen.this.camera != null) {
                scScan.setText("Scanning is currently disabled");
                camera.close();
                camera = null;
            }
            Toast.makeText(this, isScanRunning ? "Start Scanning" : "Stop Scanning", Toast.LENGTH_SHORT).show();
        });
        search.setOnClickListener(v -> {

            if ("".equals(search.getText())) {
                final EditText editText = new EditText(this);
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_search_24)
                        .setTitle("Please enter barcode to search for")
                        .setView(editText)
                        .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!"".equals(editText.getText().toString())) {
                                    ArrayList<Barcode> temp = new ArrayList<Barcode>();
                                    try {
                                        for (Barcode b : barcodeDAO.getBarcodesForArea(parentArea.getAreaID())) {
                                            if (b.getBarcodeString().equals(editText.getText().toString())) {
                                                temp.add(b);
                                                break;
                                            }
                                        }
                                        mBarcodeListAdapter = new BarcodeListAdapter(ScanningScreen.this, R.layout.listview_scanning_screen, temp, duplicationEnabled);
                                        mListView.setAdapter(mBarcodeListAdapter);
                                        search.setText("");
                                        update();
                                        if (temp.size() > 0)
                                            Toast.makeText(ScanningScreen.this, "Barcode results found", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(ScanningScreen.this, "No results found", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(ScanningScreen.this, "No barcode entered", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .show();
            } else {
                try {
                    mBarcodeListAdapter = new BarcodeListAdapter(ScanningScreen.this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mListView.setAdapter(mBarcodeListAdapter);
                search.setText("");
                update();
            }
        });


        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(ScanningScreen.this);
            //sets up the video display, interface methods are overridden below
        }

        // Set this to be the value passed from area
        // or just count how many barcodes are currently in array
        int s = 0;
        for (Barcode b : barcodeDAO.getBarcodesForArea(parentArea.getAreaID())) {
            s = s + b.getBarcodeCount();
        }
        mCountGlobal = s;
        mPreCountGlobal = parentArea.getAreaPreCount(); // parent area holds precount of barcodes

        mCount.setText(String.valueOf(mCountGlobal));
        mPreCount.setHint("Enter PreCount");
        calculateDifference();

        mPreCount.setText(String.valueOf(mPreCountGlobal));
        mPreCount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override // this is used alongside the textwatcher to ensure a value is displayed in the field
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) { // if preCount edittext does not have focus
                    if (mPreCount.getText().toString().equals("")) {
                        mPreCount.setText("0"); // set default value of 0 if no value was entered before loss of focus
                    }
                }
            }
        });

        mBarcodeListAdapter = new BarcodeListAdapter(this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
        mListView.setAdapter(mBarcodeListAdapter);

        // When enter is pressed, adds on a \n character
        // Program then picks up this change and saves the barcode
        mEnter.setOnClickListener((View v) -> {
            String temp = "";
            try {
                temp = mBarcode.getText().toString();
                temp = temp + "\n";
            } catch (Exception ex) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
            }
            mBarcode.setText(temp);
        });

     /*   mConfirmPreCount.setOnClickListener((View v) -> {

            String tempString = mPreCount.getText().toString();
            int tempPreCount;

            if (tempString.equals("")) {
                tempPreCount = 0;
            } else {
                tempPreCount = Integer.parseInt(tempString);
            }


            mPreCountGlobal = tempPreCount;
            try {
                // area.setPreCount(tempPreCount);
                int temp = areaDAO.updatePreCount(parentArea.getAreaID(), tempPreCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            calculateDifference();
            mBarcode.requestFocus();

        }); */
        initTextWatchers();
        SharedPreferences barcodeFilter = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE);
        barcodePrefix = barcodeFilter.getString("BarcodePrefix", barcodePrefix);
    }

    /**
     * Show toast.
     *
     * @param msg the msg
     */
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ScanningScreen.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void initTextWatchers() {
        // barcodeTextWatcher
        TextWatcher barcodeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable barcodeEditable) {
               // boolean unique = true;
                boolean scanned = true;
                String temp = barcodeEditable.toString();
                if (temp.contains("\n")) {

                    // Only shorten barcode if it had more then just \n
                    if (temp.length() > 0) {
                        temp = temp.substring(0, temp.length() - 1);
                    }

                    // Check if barcode is empty,
                    // If empty then this means user has pushed enter with nothing entered
                    // Therefore display toast to inform user that barcode hasn't been added
                    if (temp.equals("")) {
                        Toast.makeText(getApplicationContext(), "No barcode entered. Please check barcode entry box", Toast.LENGTH_SHORT).show();
                        mBarcode.setText("");
                        return;
                    } else {
                        // Send Barcode string to addBarcodeLogic function
                        // This function handles creation of barcode object
                        try {
                            scanned = saveBarcode(temp, 0); //have a test if 0 then No image is stored
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (scanned/*unique || duplicationEnabled*/) { // only care about whether barcode was scanned or not
                            CharSequence text = "Barcode " + temp + " scanned";
                            Log.d(TAG, text.toString());
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }

                        mBarcodeListAdapter.notifyDataSetChanged();

                    //    calculateDifference();

                        mBarcode.setText("");
                    }
                }
            }
        };
        mBarcode.addTextChangedListener(barcodeTextWatcher);
        // preCount watcher
        TextWatcher preCountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // not used
            }

            @Override
            public void afterTextChanged(Editable s) { // save new preCount, update stats
                String tempString = mPreCount.getText().toString();
                int tempPreCount;

                if (tempString.equals("")) {
                    tempPreCount = 0; // textwatcher takes care of updating preCount values on default case as well

                } else {
                    tempPreCount = Integer.parseInt(tempString);
                }

                mPreCountGlobal = tempPreCount;
                try {

                    int temp = areaDAO.updatePreCount(parentArea.getAreaID(), tempPreCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                calculateDifference();

            }
        };
        mPreCount.addTextChangedListener(preCountWatcher);

    }

    public void calculateDifference() {
        int difference;
        mCount.setText(String.valueOf(mCountGlobal));

        if (mPreCount.getText().toString() == "") {
            mPreCountGlobal = 0;
        }

        difference = mCountGlobal - mPreCountGlobal;

        // if 0, set difference text to green
        // Otherwise set text Colour to red
        if (difference == 0) {
            mDifference.setTextColor(Color.GREEN);
        } else {
            mDifference.setTextColor(Color.RED);
        }
        if (difference > 0) {
            mDifference.setText("+" + Integer.toString(difference));
        } else {
            mDifference.setText(Integer.toString(difference));
        }

    }

    // Firstly checks if scanned barcode is already in system
    // If not it then constructs new barcode object and adds it to the arrayList
    public boolean saveBarcode(String barcode, int bitmapId) throws Exception {
       // playSound();
        if (barcodePrefix != null) { // if barcode prefix is set, filter barcodes to scan
            int prefixLength = barcodePrefix.length(); // get prefix length
            if (barcode.length() >= prefixLength) { // barcode must be at least as long as the prefix
                String scannedBarcodePrefix = barcode.substring(0, prefixLength);
                if (!scannedBarcodePrefix.equals(barcodePrefix)) { // scanned barcode doesn't match prefix filter
                    Toast.makeText(this, "Barcode ignored. Doesn't match prefix settings", Toast.LENGTH_SHORT).show();
                    return false; // barcode ignored, not added to database
                }
            } else { // barcode is too short. ignore it
                Toast.makeText(this, "Barcode ignored. Doesn't match prefix settings", Toast.LENGTH_SHORT).show();
                return false; // barcode ignored, not added to database
            }
        }

        playSound();
        //Check to see if the barcode doesn't exist before adding.
        boolean unique = true;
        for (int i = 0; i < barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).size(); i++) {
            if (barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).get(i).getBarcodeString().equals(barcode)) {
                unique = false;
                break;
            }
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        if (unique || duplicationEnabled) {//barcode can be unique or duplicate
            mCountGlobal++;
            calculateDifference();
            int x = 0;
            for (Barcode b : barcodeDAO.getBarcodesForArea(parentArea.getAreaID())) {
                if (b.getBarcodeString().equals(barcode)) {
                    x = b.getBarcodeCount();
                    x++;
                    // b.setCount("" + x);
                    int temp5 = barcodeDAO.updateBarcodeCount(x, b.getBarcodeID());
                    temp5 = barcodeDAO.updateBarcodeDate(dateFormat.format(new Date()), b.getBarcodeID());
                    // b.setBitmapId(bitmapId);
                    String tempBitmapID = b.getBitmapID() + bitmapId + ",";
                    temp5 = barcodeDAO.updateBitmapID(tempBitmapID, b.getBarcodeID());
                    mBarcodeListAdapter = new BarcodeListAdapter(this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
                    mListView.setAdapter(mBarcodeListAdapter);
                    update();
                  //  BarcodeText.setText(getString(R.string.barcode));
                    break;
                }
            }
            if (x == 0) {   // unique barcode //
                // area.addBarcode(new Barcode(barcode, area.getAreaString(), 1, bitmapId));
                Barcode newBarcode = new Barcode(parentArea.getAreaID(), barcode, dateFormat.format(new Date()),
                        1, bitmapId + ",");
                barcodeDAO.insertBarcode(newBarcode);
                mBarcodeListAdapter = new BarcodeListAdapter(this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
                mListView.setAdapter(mBarcodeListAdapter);
                update();
              //  BarcodeText.setText(getString(R.string.barcode));
            } // update parents...//
            int temp5 = stocktakeDAO.updateDateModified(parentStocktake.getStocktakeID(), dateFormat.format(date));
            int tempCount = parentArea.getNumOfBarcodes() + 1; // increment number of barcodes in area
            temp5 = areaDAO.updateNumOfBarcodes(parentArea.getAreaID(), tempCount);
            //writeFileOnInternalStorage();
        } else {
            Toast.makeText(this, "Barcode ignored, already exists in system", Toast.LENGTH_LONG).show();
        }

        update();
        return true; //unique; - function now returns true if barcode was scanned
                    // and false if it was not. Duplication is enabled by default
    }


    //home button for top title
    public void goHome(View view) {
        Intent intent = new Intent(this, ActivityMain.class);

        startActivity(intent);
        finishAffinity();
    }

    public void DeleteRow(View view) throws Exception {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        LinearLayout parent = (LinearLayout) view.getParent();
                        TextView child = (TextView) parent.getChildAt(0);
                        String item = child.getText().toString();
                        Toast.makeText(ScanningScreen.this, item + " deleted", Toast.LENGTH_LONG).show();
                      //  mPreCount.setText(item+ " deleted");
                        try {
                            for (int n = 0; n < barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).size(); n++) {
                                Barcode barcode = barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).get(n);
                                if (barcode.getBarcodeString().equals(item)) {
                                    int bitmapId;
                                    String[] bitmapIDs = barcode.getBitmapID().split(",");
                                    bitmapId = Integer.parseInt(bitmapIDs[0]); // get first bitmap id
                                    if (barcode.getBarcodeCount() == 1) {
                                        barcodeDAO.delete(barcode); /// delete barcode from database
                                    } else {
                                        int x = barcode.getBarcodeCount();
                                        x--;
                                        int temp5 = barcodeDAO.updateBarcodeCount(x, barcode.getBarcodeID()); // update barcode count for specific barcode

                                        int beginIndex = bitmapIDs[0].length() + 1; // need to start substring passed the "," of old bitmapID
                                        String newBitmapIDs = barcode.getBitmapID().substring(beginIndex);
                                        temp5 = barcodeDAO.updateBitmapID(newBitmapIDs, barcode.getBarcodeID()); // update database entry
                                    }
                                    int temp5 = stocktakeDAO.updateDateModified(parentStocktake.getStocktakeID(), // update stocktake modified date
                                            new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
                                    int tempBarcodeCount = parentArea.getNumOfBarcodes() - 1; // decrement total barcode count
                                    temp5 = areaDAO.updateNumOfBarcodes(parentArea.getAreaID(), tempBarcodeCount); // update database entry
                                    mCountGlobal--;
                                    calculateDifference();
                                    //    writeFileOnInternalStorage();
                                    view.getId();
                                    mBarcodeListAdapter = new BarcodeListAdapter(ScanningScreen.this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
                                    mListView.setAdapter(mBarcodeListAdapter);
                                    update();
                                    BarcodeText.setText(getString(R.string.barcode));

                                    File f = new File(bitmapDirectory, bitmapId + ".jpg");
                                    boolean delete = f.delete();

                                    break;
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
                        Toast.makeText(ScanningScreen.this, "Record not deleted", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    public void findBarcodeImage(View view) throws Exception {

        LinearLayout parent = (LinearLayout) view.getParent();
        TextView child = (TextView) parent.getChildAt(0);
        String barcodeName = child.getText().toString();

        Barcode barcode = null;
        try {
            for (int n = 0; n < barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).size(); n++) {
                if (barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).get(n).getBarcodeString().equals(barcodeName)) {
                    barcode = barcodeDAO.getBarcodesForArea(parentArea.getAreaID()).get(n);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (barcode != null) {
            ArrayList<Integer> bitmapIdArray = new ArrayList<>(); //getting photos to display
            String[] bitmapIds = barcode.getBitmapID().split(",");
            for (String bitmap : bitmapIds) { // need to add string bitmapIDs to integer ArrayList
                bitmapIdArray.add(Integer.parseInt(bitmap));
            }
            ArrayList<Integer> arrayNoZeroIds = new ArrayList<>(); //removing any manual barcode entries not scanned via camera
            boolean containsBitmap = false;

            for (int id : bitmapIdArray) { //removes any 0 entries from the bitmap array that were manually entered
                if (id != 0) {
                    arrayNoZeroIds.add(id);
                    containsBitmap = true;
                }
            }
            if (containsBitmap) {
                boolean last = false; //is this the last bitmap?
                if (1 == arrayNoZeroIds.size()) {
                    last = true; //only one image stored
                }
                displayImageInAlertDialog(arrayNoZeroIds, 0, last, barcode.getBarcodeString());
            } else {
                Toast.makeText(ScanningScreen.this, "No image found for: " + barcode.getBarcodeString(), Toast.LENGTH_LONG).show();
            }
        } else {
            showToast("No barcode found");
        }

    }

    public void displayImageInAlertDialog(ArrayList<Integer> bitmapIdArray, int index, boolean last, String barcodeName) {

        int id = bitmapIdArray.get(index);
        File f = new File(bitmapDirectory, id + ".jpg"); //get file location for specified id

        try {
            Bitmap bit = BitmapFactory.decodeStream(new FileInputStream(f));
            final ImageView alertImageView = new ImageView(ScanningScreen.this);
            alertImageView.setImageBitmap(bit);

            if (!last) { //last is false so there are more images to possibly display
                index++; //get to next index place
                if (index == bitmapIdArray.size() - 1) {
                    last = true; //so the next alert dialog will have NO next button to press
                }
                int finalIndex = index++; //need these to be final for onClick
                boolean finalLast = last;
                new AlertDialog.Builder(ScanningScreen.this)
                        .setIcon(R.drawable.ic_baseline_photo_camera_24)
                        .setTitle("Barcode: " + barcodeName + " " + (index - 1) + "/" + bitmapIdArray.size())
                        .setView(alertImageView)
                        .setPositiveButton("next", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                displayImageInAlertDialog(bitmapIdArray, finalIndex, finalLast, barcodeName);
                            }
                        })
                        .setNeutralButton("delete", (dialogInterface, i) -> {
                            new AlertDialog.Builder(ScanningScreen.this)
                                    .setTitle("Delete Barcode?")
                                    .setMessage("Are you sure you want to delete this barcode?")
                                    .setPositiveButton("Yes", (dInterface, j) -> {
                                        deleteBarcodeImage(String.valueOf(id), barcodeName);
                                    })
                                    .setNegativeButton("No", (d, k) -> { // display toast
                                        Toast.makeText(ScanningScreen.this, "Barcode not deleted", Toast.LENGTH_LONG).show();
                                    }).show();

                        })
                        .setNegativeButton("close", null)//end the recursion
                        .show();
            } else {
                new AlertDialog.Builder(ScanningScreen.this)
                        .setIcon(R.drawable.ic_baseline_photo_camera_24)
                        .setTitle("Barcode: " + barcodeName + " " + (index + 1) + "/" + bitmapIdArray.size())
                        .setView(alertImageView)
                        .setNeutralButton("delete", (dialogInterface, i) -> {
                            new AlertDialog.Builder(ScanningScreen.this)
                                    .setTitle("Delete Barcode?")
                                    .setMessage("Are you sure you want to delete this barcode?")
                                    .setPositiveButton("Yes", (dInterface, j) -> {
                                        deleteBarcodeImage(String.valueOf(id), barcodeName);
                                    })
                                    .setNegativeButton("No", (d, k) -> {
                                        Toast.makeText(ScanningScreen.this, "Barcode not deleted", Toast.LENGTH_LONG).show();
                                    }).show();

                        })
                        .setNegativeButton("close", null)//last is true so no next button
                        .show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void deleteBarcodeImage(String bitmapID, String barcodeString) { // delete specified barcode image
        Barcode barcode = barcodeDAO.getBarcodeByString(barcodeString, parentArea.getAreaID());
        String[] bitmapIDs = barcode.getBitmapID().split(",");
        if (bitmapIDs.length == 1) { // only one bitmap...delete barcode from database...
            barcodeDAO.delete(barcode);
        }
        else {  // multiple bitmapIDs for given barcode have to modify string
            StringBuilder builder = new StringBuilder();
            String updatedBitmapIDs;
            for (String bitmap : bitmapIDs) { // add all bitmapIDs except selected one
                if (!bitmap.equals(bitmapID)) {
                    builder.append(bitmap).append(",");
                }
            }
            updatedBitmapIDs = builder.toString();
            barcodeDAO.updateBitmapID(updatedBitmapIDs, barcode.getBarcodeID());
            int tempCount = barcode.getBarcodeCount() - 1;
            barcodeDAO.updateBarcodeCount(tempCount, barcode.getBarcodeID());
        }

        int temp5 = stocktakeDAO.updateDateModified(parentStocktake.getStocktakeID(), // update stocktake modified date
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        int tempBarcodeCount = parentArea.getNumOfBarcodes() - 1; // decrement total barcode count
        temp5 = areaDAO.updateNumOfBarcodes(parentArea.getAreaID(), tempBarcodeCount); // update database entry
        mCountGlobal--;
        calculateDifference();

        mBarcodeListAdapter = new BarcodeListAdapter(ScanningScreen.this, R.layout.listview_scanning_screen, new ArrayList<>(barcodeDAO.getBarcodesForArea(parentArea.getAreaID())), duplicationEnabled);
        mListView.setAdapter(mBarcodeListAdapter);
        update();   //update listview

        try {
            File f = new File(bitmapDirectory, bitmapID + ".jpg");
            boolean delete = f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(ScanningScreen.this, "Barcode " + barcode.getBarcodeString()+ " deleted", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            //writeFileOnInternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        intervalTime = getSharedPreferences("Scan interval", Context.MODE_PRIVATE).getLong("Scan interval time", 2000);
        // try to re-initialise the previewer and check if the product(drone) is attached and ready to use

        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    protected void onStop() {
        BarcodeScannerDB.closeDatabase();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // BarcodeScannerDB.closeDatabase();
    }

    public void playSound() {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        mp.start();
    }


    public static final String RESULT_DATA_BARCODES = "barcodes";
    public static final String RESULT_DATA_IMAGE_ID = "image";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0;
    private static final Matrix BITMAP_ROTATION_MATRIX = new Matrix() {{
        postRotate(-90);
    }};

    private final BarcodeScanner scanner = BarcodeScanning.getClient();

    private int width, height;

    private CameraDevice camera;
    private Size size;

    private boolean isRunning = false;


    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        this.width = width;
        this.height = height;
        Log.i(TAG, "onSurfaceTextureAvailable: ");
        if (isScanRunning) {
            setupCamera();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        this.width = width;
        this.height = height;
        Log.i(TAG, "onSurfaceTextureSizeChanged: ");
        if (isScanRunning) {
            setupCamera();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (camera != null) {
            camera.close();
            camera = null;
        }
        return false;
    }

    private long preScanTime = 0;
    private long intervalTime;

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        if (isRunning) return;
        isRunning = true;
        Log.i(TAG, "onSurfaceTextureUpdated: first scan ready");
        if ((System.currentTimeMillis() - preScanTime) > intervalTime) {
            Bitmap bitmap = mVideoSurface.getBitmap();
            scanner.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(barcodes -> {
                        Log.i(TAG, "onSurfaceTextureUpdated: first scan ");
                        preScanTime = System.currentTimeMillis();
                        onBarcodeRead(barcodes, bitmap);
                    })
                    .addOnFailureListener(e -> isRunning = false);
        } else {
            Log.i(TAG, "onSurfaceTextureUpdated: repetition");
            Toast.makeText(this, "Do not scan code repeatedly within " + intervalTime / 1000 + " seconds", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
            setupCamera();
        }
    }

    private void setupCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        if (camera != null) {
            camera.close();
            camera = null;
        }

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            size = map.getOutputSizes(SurfaceTexture.class)[0];

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    ScanningScreen.this.camera = camera;
                    SurfaceTexture surfaceTexture = mVideoSurface.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                    Surface surface = new Surface(surfaceTexture);

                    try {
                        // Ignore deprecation inspection for android version match.
                        //noinspection deprecation
                        camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    captureRequestBuilder.addTarget(surface);
                                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    CaptureRequest captureRequest = captureRequestBuilder.build();

                                    session.setRepeatingRequest(captureRequest, null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }

                                configureTransform(width, height);
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    ScanningScreen.this.camera = null;

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    ScanningScreen.this.camera = null;

                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mVideoSurface || null == size) {
            return;
        }
        int rotation = getDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, size.getHeight(), size.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / size.getHeight(),
                    (float) viewWidth / size.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mVideoSurface.setTransform(matrix);
    }

    private void onBarcodeRead(List<com.google.mlkit.vision.barcode.Barcode> barcodes, Bitmap capture) {
        isRunning = false;

        if (barcodes.isEmpty()) return;
        for (String barcode : barcodes.stream().map(com.google.mlkit.vision.barcode.Barcode::getRawValue).toArray(String[]::new)) {
            try {
                saveBarcode(barcode, saveImage(capture));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int saveImage(Bitmap image) {
        int imageId = generateUniqueId();

        File file = new File(bitmapDirectory, imageId + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bitmapToByteArray(image));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageId;
    }

    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        Bitmap processed = processBitmap(bitmap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        processed.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        processed.recycle();
        return stream.toByteArray();
    }

    private static Bitmap processBitmap(Bitmap bitmap) {
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), BITMAP_ROTATION_MATRIX, false);
        Bitmap scaled = Bitmap.createScaledBitmap(rotated, rotated.getHeight(), rotated.getWidth(), false);
        rotated.recycle();
        return scaled;
    }

    private static int generateUniqueId() {
        int id = com.example.timbersmartbarcodescanner.Barcode.getImageIdCount();
        com.example.timbersmartbarcodescanner.Barcode.setImageIdCount(id + 1);
        return id;
    }
}