package pp.facerecognizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import pp.facerecognizer.env.FileUtils;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrainingActivity extends AppCompatActivity implements Classifier.CompleteListener {

    private Classifier classifier;
    private static final int FACE_SIZE = 160;
    Button btTrain;
    Button btCamera;
    EditText et;
    private ProgressDialog dialog;
    int numberTraining = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        new Thread(this::init).start();
        btTrain = findViewById(R.id.btTrain);
        et = findViewById(R.id.et);
        btCamera = findViewById(R.id.btCamera);
        btTrain.setOnClickListener(v -> {
            dialog.setMessage("Training, please wait.");
            dialog.show();
            training();
        });
        btCamera.setOnClickListener(v->{
            startActivity(new Intent(this,MainActivity.class));
        });
        et.setText(String.valueOf(numberTraining));

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
        } catch (Exception e) {
            finish();
        }
    }
    int count=0;
    int maxLength=0;

    void training(){
        try{
            numberTraining = Integer.parseInt(et.getText().toString());
        }catch (Exception e){

        }
        AssetManager mgr = getAssets();
        File label = new File(FileUtils.LABEL_FILE);
        try {
            label.delete();
            FileUtils.copyAsset(mgr, FileUtils.LABEL_FILE);
        }catch (Exception ex){

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                count++;
                if(count>=maxLength)
                    dialog.dismiss();
            }
        });
    }
}
