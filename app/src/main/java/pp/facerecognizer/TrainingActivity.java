package pp.facerecognizer;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import pp.facerecognizer.env.FileUtils;

import static pp.facerecognizer.CameraActivity.PERMISSION_CAMERA;
import static pp.facerecognizer.CameraActivity.PERMISSION_STORAGE;
import static pp.facerecognizer.env.FileUtils.ROOT;


public class TrainingActivity extends AppCompatActivity {

    private Classifier classifier;
    private static final int FACE_SIZE = 160;
    final static int numberTraining = 50;
    Button btTrain;
    Button btCamera;
    private ProgressDialog dialog;
    private static final int PERMISSIONS_REQUEST = 1;
    boolean initDone;
    PublishSubject<Pair> publishSubject = PublishSubject.create();
    private int length;
    private int count=0;

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
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
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
        btCamera = findViewById(R.id.btCamera);
        btTrain.setOnClickListener(v -> {
            if (initDone) {
                dialog.setMessage("Training, please wait.");
                dialog.show();
                training();
            } else {
                Toast.makeText(this, "Init Failed!", Toast.LENGTH_SHORT).show();
            }
        });
        btCamera.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        btTrain.setEnabled(false);

        publishSubject.subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(new Observer<Pair>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Pair pair) {
                        count++;
                        List<Uri> uris = (List<Uri>) pair.second;
                        classifier.extraction((int)pair.first,uris,getContentResolver());
                        if(count == length)
                            publishSubject.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                        });
                    }
                });

    }

    void init() {
        File dir = new File(ROOT);
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
            initDone = true;
        } catch (Exception e) {
            finish();
        }
        runOnUiThread(() -> btTrain.setEnabled(true));

    }

    void training() {
        FileUtils.clear(FileUtils.LABEL_FILE);
        FileUtils.clear(FileUtils.DATA_FILE);
        FileUtils.clear(FileUtils.MODEL_FILE);
        String directoryUrl = Environment.getExternalStorageDirectory() + "/training";
        File directory = new File(directoryUrl);
        File[] array = directory.listFiles();
        length = array.length;
        for (int i = 0; i < length; i++) {
            File[] files = array[i].listFiles();
            ArrayList<Uri> uris = new ArrayList<>();
            if (files != null && files.length > 0) {
                FileUtils.appendText(array[i].getName(), FileUtils.LABEL_FILE);
                for (File file : files) {
                    if (file.getPath().contains(".jpg"))
                        uris.add(Uri.fromFile(file));

                }
                List<Uri> data = uris.subList(0, numberTraining);
                publishSubject.onNext(new Pair(i,data));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (hasPermission()) {
                new Thread(this::init).start();
            }
        }

    }
}
