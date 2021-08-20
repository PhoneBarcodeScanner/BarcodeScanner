package com.example.timbersmartbarcodescanner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.appcompat.widget.Toolbar;

import com.example.timbersmartbarcodescanner.scan.BarcodeScannerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ScanningScreen extends AppCompatActivity implements Serializable {

    private static final String TAG = "ScanningScreen";
    
    private Area area;
    
    private int mCountGlobal, mPreCountGlobal;
    private TextView mCount, mDifference, mArea_title, mt_Barcode, mt_Row, mt_Count, mt_Date;
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

    /**
     * @param mReceivedVideoDataListener received video data listener.
     */
    protected TextureView mVideoSurface = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning_screen);
//        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        toolbar_scanning_screen = (Toolbar) findViewById(R.id.ScanningScreenToolBar);
        setSupportActionBar(toolbar_scanning_screen);

        //grab Area object from AreaScreen
        Intent intent = getIntent();
        mPassedAreaIndex = intent.getIntExtra("Area Index", -1);
        mPassedStocktakeIndex = intent.getIntExtra("Stocktake Index", -1);
        area = Data.getDataInstance().getStocktakeList().get(mPassedStocktakeIndex).getAreaList().get(mPassedAreaIndex);

        //get directory path to the bitmap storage location
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        bitmapDirectory = cw.getDir("bitmap_", Context.MODE_PRIVATE);

        //get the current imageUniqueId that a new image can be stored under
        try {
            imageUniqueId = Data.getDataInstance().getImageIdCount();
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            //set title for scanning screen
            mArea_title = findViewById(R.id.textViewTitle);
            mArea_title.setText(area.getAreaString() + "'s Barcodes");
            init();
            //Animation code for barcode titles removed from here
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 바코드 읽기 요청
        findViewById(R.id.scanBtn).setOnClickListener(v -> startActivityForResult(
                new Intent(this, BarcodeScannerActivity.class),
                0));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // 바코드 읽기 결과 처리
        if (requestCode == 0) {
            if (resultCode != Activity.RESULT_OK || data == null) return;
            String[] barcodes = data.getStringArrayExtra(BarcodeScannerActivity.RESULT_DATA_BARCODES);
            int imageId = data.getIntExtra(BarcodeScannerActivity.RESULT_DATA_IMAGE_ID, -1);

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
    }

    // Small function to help prevent long lines of code when accessing Data class
    public Stocktake getStocktakeFromData(int i) throws Exception {
        return Data.getDataInstance().getStocktakeList().get(i);
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
        mi = menu.findItem(R.id.check_duplicated);
        help = menu.findItem(R.id.help);

        Boolean checkSelect = getSharedPreferences("Timber Smart", Context.MODE_PRIVATE).getBoolean("Scanning", false);
        if (checkSelect) {
            mi.setChecked(true);
        } else {
            mi.setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.check_duplicated:
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
            case R.id.help:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_info_24)
                        .setTitle("Help Instruction")
                        .setMessage("'Allow Duplication' will allow the entering of duplicate data")
                        //.setMessage("The setting of Allow Duplication True/False will enable or disable adding same Barcode/Area/Stock take in the activity of Barcode/Area/Stock.")
                        .setPositiveButton("OK", null)
                        .show();
                break;
            case R.id.bluetooth_connect:
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
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
        mConfirmPreCount = findViewById(R.id.buttonConfirmPreCount);
        mListView = findViewById(R.id.ScanningScreenListView);
        search = findViewById(R.id.buttonSearch);
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
                                        for (Barcode b : area.getBarcodeList()) {
                                            if (b.getBarcode().equals(editText.getText().toString())) {
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
                    mBarcodeListAdapter = new BarcodeListAdapter(ScanningScreen.this, R.layout.listview_scanning_screen, area.getBarcodeList(), duplicationEnabled);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mListView.setAdapter(mBarcodeListAdapter);
                search.setText("");
                update();
            }
        });

        mVideoSurface = findViewById(R.id.video_previewer_surface);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener((TextureView.SurfaceTextureListener) this);
            //sets up the video display, interface methods are overridden below
        }

        // Set this to be the value passed from area
        // or just count how many barcodes are currently in array
        int s = 0;
        for (Barcode b : area.getBarcodeList()) {
            s = s + Integer.parseInt(b.getCount());
        }
        mCountGlobal = s;
        mPreCountGlobal = area.getPreCount();

        mCount.setText(String.valueOf(mCountGlobal));
        mPreCount.setHint("Enter PreCount");
        calculateDifference();

        mPreCount.setText(String.valueOf(mPreCountGlobal));

        mBarcodeListAdapter = new BarcodeListAdapter(this, R.layout.listview_scanning_screen, area.getBarcodeList(), duplicationEnabled);
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

        mConfirmPreCount.setOnClickListener((View v) -> {
            String tempString = mPreCount.getText().toString();
            int tempPreCount;

            if (tempString.equals("")) {
                tempPreCount = 0;
            } else {
                tempPreCount = Integer.parseInt(tempString);
            }

            mPreCountGlobal = tempPreCount;
            try {
                area.setPreCount(tempPreCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
            calculateDifference();
            mBarcode.requestFocus();

        });
        initTextWatchers();
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
                boolean unique = true;
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
                            unique = saveBarcode(temp, 0); //have a test if 0 then No image is stored
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (unique || duplicationEnabled) {
                            CharSequence text = "Barcode " + temp + " scanned";
                            Log.d(TAG, text.toString());
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }

                        mBarcodeListAdapter.notifyDataSetChanged();

                        calculateDifference();

                        mBarcode.setText("");
                    }
                }
            }
        };

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
        playSound();

        //Check to see if the barcode doesn't exist before adding.
        boolean unique = true;
        for (int i = 0; i < area.getBarcodeList().size(); i++) {
            if (area.getBarcodeList().get(i).getBarcode().equals(barcode)) {
                unique = false;
                break;
            }
        }

        if (unique || mi.isChecked()) {//if the barcode is unique
            mCountGlobal++;
            int x = 0;
            for (Barcode b : area.getBarcodeList()) {
                if (b.getBarcode().equals(barcode)) {
                    x = Integer.parseInt(b.getCount());
                    x++;
                    b.setCount("" + x);
                    b.setDateTime(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
                    b.setBitmapId(bitmapId);
                    break;
                }
            }
            if (x == 0) {
                area.addBarcode(new Barcode(barcode, area.getAreaString(), 1, bitmapId));
            }
            writeFileOnInternalStorage();
        } else {
            Toast.makeText(this, "Barcode ignored, already exists in system", Toast.LENGTH_LONG).show();
        }

        update();
        return unique;
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
                        try {
                            for (int n = 0; n < area.getBarcodeList().size(); n++) {
                                Barcode barcode = area.getBarcodeList().get(n);
                                if (barcode.getBarcode().equals(item)) {
                                    int bitmapId;
                                    if (barcode.getCount().equals("1")) {
                                        bitmapId = barcode.getBitmapIdArrayList().get(0);//delete the only entry at index 0
                                        area.getBarcodeList().remove(n);
                                    } else {
                                        int x = Integer.parseInt(barcode.getCount());
                                        x--;
                                        barcode.setCount("" + x); //decrease count
                                        //delete one image instance first or last entry in array list???
                                        ArrayList<Integer> bitmapIdArray = barcode.getBitmapIdArrayList();
                                        int bitmapIndex = bitmapIdArray.size() - 1;//get last index to delete from???
                                        bitmapId = barcode.getBitmapIdArrayList().get(bitmapIndex);
                                        barcode.deleteOneBitmapId(bitmapIndex); //??
                                    }
                                    mCountGlobal--;
                                    calculateDifference();
                                    writeFileOnInternalStorage();
                                    view.getId();
                                    update();

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
            for (int n = 0; n < area.getBarcodeList().size(); n++) {
                if (area.getBarcodeList().get(n).getBarcode().equals(barcodeName)) {
                    barcode = area.getBarcodeList().get(n);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (barcode != null) {
            ArrayList<Integer> bitmapIdArray = barcode.getBitmapIdArrayList(); //getting photos to display
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
                displayImageInAlertDialog(arrayNoZeroIds, 0, last, barcode.getBarcode());
            } else {
                Toast.makeText(ScanningScreen.this, "No image found for: " + barcode.getBarcode(), Toast.LENGTH_LONG).show();
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
                        .setNegativeButton("close", null)//end the recursion
                        .show();
            } else {
                new AlertDialog.Builder(ScanningScreen.this)
                        .setIcon(R.drawable.ic_baseline_photo_camera_24)
                        .setTitle("Barcode: " + barcodeName + " " + (index + 1) + "/" + bitmapIdArray.size())
                        .setView(alertImageView)
                        .setNegativeButton("close", null)//last is true so no next button
                        .show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        // try to re-initialise the previewer and check if the product(drone) is attached and ready to use

        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void playSound() {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        mp.start();
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

    // Camera and usbCameraActivity Methods/classes
    // Documentation for Library can be found here - https://github.com/jiangdongguo/AndroidUSBCamera#readme
    //
    // Picks up on whether or not USB camera is connected or disconnected
    // This is required for the guidance camera to work
//    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
//
//        @Override
//        public void onAttachDev(UsbDevice device) {
//            // request open permission(must have)
//            if (!isRequest) {
//                isRequest = true;
//                if (mCameraHelper != null) {
//                    mCameraHelper.requestPermission(0);
//                }
//            }
//        }
//
//        @Override
//        public void onDettachDev(UsbDevice device) {
//            // Close camera(must have)
//            if (isRequest) {
//                isRequest = false;
//                mCameraHelper.closeCamera();
//            }
//        }
//
//        @Override
//        public void onConnectDev(UsbDevice device, boolean isConnected) {
//            if (!isConnected) {
//                Toast.makeText(ScanningScreen.this,"Failed to connect, please check resolution parameters",Toast.LENGTH_LONG).show();
//                isPreview = false;
//            } else {
//                isPreview = true;
//                Toast.makeText(ScanningScreen.this,"Connecting",Toast.LENGTH_LONG).show();
//            }
//        }
//
//        @Override
//        public void onDisConnectDev(UsbDevice device) {}
//    };
//
//    @Override
//    public USBMonitor getUSBMonitor() {
//        return mCameraHelper.getUSBMonitor();
//    }
//
//    @Override
//    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
//        // Must have
//        if (!isPreview && mCameraHelper.isCameraOpened()) {
//            mCameraHelper.startPreview(mUVCCameraView);
//            isPreview = true;
//        }
//    }
//
//    @Override
//    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
//        // must have
//        if (isPreview && mCameraHelper.isCameraOpened()) {
//            mCameraHelper.stopPreview();
//            isPreview = false;
//        }
//    }
//
//    // Unused but needed to be overridden ==========================================================
//    @Override
//    public void onDialogResult(boolean canceled) {}
//
//    @Override
//    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {}
//    // =============================================================================================
}