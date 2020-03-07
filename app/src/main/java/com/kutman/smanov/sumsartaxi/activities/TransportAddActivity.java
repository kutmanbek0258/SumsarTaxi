package com.kutman.smanov.sumsartaxi.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kutman.smanov.sumsartaxi.R;
import com.kutman.smanov.sumsartaxi.TransportApplication;
import com.kutman.smanov.sumsartaxi.data.UserData;
import com.kutman.smanov.sumsartaxi.models.Response;
import com.kutman.smanov.sumsartaxi.models.Transport;
import com.kutman.smanov.sumsartaxi.models.User;
import com.kutman.smanov.sumsartaxi.network.NetworkUtil;
import com.kutman.smanov.sumsartaxi.session.TransportSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.kutman.smanov.sumsartaxi.utils.Validation.validateFields;

public class TransportAddActivity extends AppCompatActivity {

    private ImageView transportImageIV;
    private EditText stateNumberET;
    private EditText modelET;
    private TextInputLayout transportImageTI;
    private TextInputLayout stateNumberTI;
    private TextInputLayout modelTI;
    private Button typeBT;
    private Button addBT;

    private TransportSession session;
    private UserData userData;
    private HashMap<String, String> user;

    private TransportApplication application;

    private CompositeSubscription mSubscriptions;

    private String image;

    private ProgressDialog progressDialog;

    public static final int REQUEST_IMAGE = 200;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_add_activity);

        session = new TransportSession(getApplicationContext());

        application = (TransportApplication)getApplication();

        mSubscriptions = new CompositeSubscription();

        userData = new UserData(getApplicationContext());
        user = userData.getUserDetails();

        getProfile(user.get("token"),user.get("email"));

        initViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = intent.getParcelableExtra("path");
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    transportImageIV.setImageBitmap(bitmap);
                    image = bmpToBase64(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initViews(){
        transportImageIV = (ImageView)findViewById(R.id.transport_add_iv_image);
        stateNumberET = (EditText)findViewById(R.id.transport_add_et_state_number);
        modelET = (EditText)findViewById(R.id.transport_add_et_model);
        transportImageTI = (TextInputLayout)findViewById(R.id.transport_add_ti_image);
        stateNumberTI = (TextInputLayout)findViewById(R.id.transport_add_ti_state_number);
        modelTI = (TextInputLayout)findViewById(R.id.transport_add_ti_model);
        typeBT = (Button)findViewById(R.id.transport_add_btn_type);
        addBT = (Button)findViewById(R.id.transport_add_btn);

        transportImageIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity(TransportAddActivity.this)
                        .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report.areAllPermissionsGranted()) {
                                    showImagePickerOptions();
                                }

                                if (report.isAnyPermissionPermanentlyDenied()) {
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        });

        addBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndAddTransport();
            }
        });
    }

    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(this, new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(TransportAddActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);

        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(TransportAddActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    public static String bmpToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void progressDialog(String message){
        progressDialog = new ProgressDialog(TransportAddActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void setError(){
        stateNumberTI.setError(null);
        modelTI.setError(null);
    }

    private void validateAndAddTransport(){
        setError();
        int error = 0;

        String stateNumber = stateNumberET.getText().toString();
        String model = modelET.getText().toString();

        if(image == null){
            error++;
            transportImageTI.setError(getResources().getString(R.string.transport_image_error));
        }

        if(!validateFields(stateNumber)){
            error++;
            stateNumberTI.setError(getResources().getString(R.string.state_number_error));
        }

        if(!validateFields(model)){
            error++;
            modelTI.setError(getResources().getString(R.string.model_error));
        }

        if(error == 0){
            Transport transport = new Transport();
            transport.setUser(application.getUser());
            transport.setStateNumber(stateNumber);
            transport.setModel(model);
            transport.setImage(image);
            addTransport(user.get("token"), user.get("email"),transport);
        }
    }

    private void addTransport(String token, String email, Transport transport){
        progressDialog(getResources().getString(R.string.wait));
        mSubscriptions.add(NetworkUtil.getRetrofit(token)
        .transportAdd(email,transport)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(new Observer<Response>() {
            @Override
            public void onCompleted() {
                progressDialog.dismiss();
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
            }

            @Override
            public void onNext(Response response) {
                session.setLogin(true);
                progressDialog.dismiss();
                Intent intent = new Intent(TransportAddActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }
        }));
    }

    private void getProfile(String token, String email){
        mSubscriptions.add(NetworkUtil.getRetrofit(token)
                .getProfile(email)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(User resUser) {
                        application.setUser(resUser);
                    }
                }));
    }
}
