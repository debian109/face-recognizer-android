package pp.facerecognizer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import pp.facerecognizer.env.FileUtils;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static pp.facerecognizer.CameraActivity.PERMISSION_CAMERA;
import static pp.facerecognizer.CameraActivity.PERMISSION_STORAGE;


public class TrainingActivity extends AppCompatActivity implements Classifier.CompleteListener {

    private Classifier classifier;
    private static final int FACE_SIZE = 160;
    Button btTrain;
    Button btCamera;
    EditText et;
    private ProgressDialog dialog;
    int numberTraining = 100;
    private static final int PERMISSIONS_REQUEST = 1;
    boolean initDone;

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(TrainingActivity.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        if (hasPermission()) {
            new Thread(this::init).start();
        } else {
            requestPermission();
        }
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        btTrain = findViewById(R.id.btTrain);
        et = findViewById(R.id.et);
        btCamera = findViewById(R.id.btCamera);
        btTrain.setOnClickListener(v -> {
            if(initDone){
                dialog.setMessage("Training, please wait.");
                dialog.show();
                training();
            }else {
                Toast.makeText(this, "Init Failed!", Toast.LENGTH_SHORT).show();
            }
        });
        btCamera.setOnClickListener(v->{
            startActivity(new Intent(this,MainActivity.class));
        });
        et.setText(String.valueOf(numberTraining));
        btTrain.setEnabled(false);

    }

    void init() {
        File dir = new File(FileUtils.ROOT);
        if (!dir.isDirectory()) {
            if (dir.exists()) dir.delete();
            dir.mkdirs();
            AssetManager mgr = getAssets();
            FileUtils.copyAsset(mgr, FileUtils.DATA_FILE);
            FileUtils.copyAsset(mgr, FileUtils.MODEL_FILE);
            FileUtils.copyAsset(mgr, FileUtils.LABEL_FILE);
        }
        try {
            classifier = Classifier.getInstance(getAssets(), FACE_SIZE, FACE_SIZE);
            classifier.setListener(this);
            initDone = true;
        } catch (Exception e) {
            finish();
        }
        runOnUiThread(() -> btTrain.setEnabled(true));

    }
    int count=0;
    int maxLength=0;

    void training(){
        try{
            numberTraining = Integer.parseInt(et.getText().toString());
        }catch (Exception e){

        }
        Log.d("TAG", "training: START");
        String directoryUrl = Environment.getExternalStorageDirectory()+"/0";
        File directory = new File(directoryUrl);
        ContentResolver content = getContentResolver();
        File[] array  = directory.listFiles();
        maxLength = array.length;
        for (int i =0;i<array.length;i++){
            File[] files = array[i].listFiles();
            ArrayList<Uri> uris = new ArrayList<>();
            if(files!=null && files.length>0){
                classifier.addPerson(array[i].getName());
                for (File file:files){
                    if(file.getPath().contains(".jpg"))
                        uris.add(Uri.fromFile(file));
                }
                List<Uri> l = uris.subList(0,numberTraining);
                final int finalI = i;
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            classifier.updateData(finalI, content, l);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }

    @Override
    public void onCompleted() {
        runOnUiThread(() -> {
            count++;
            if(count>=maxLength)
                dialog.dismiss();
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if(requestCode == PERMISSIONS_REQUEST){
            if(hasPermission()){
                new Thread(this::init).start();
            }
        }
    }
}
