package com.kutman.smanov.sumsartaxidriver.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kutman.smanov.sumsartaxidriver.R;
import com.kutman.smanov.sumsartaxidriver.activities.MapActivity;
import com.kutman.smanov.sumsartaxidriver.data.UserData;
import com.kutman.smanov.sumsartaxidriver.models.Response;
import com.kutman.smanov.sumsartaxidriver.network.NetworkUtil;
import com.kutman.smanov.sumsartaxidriver.session.UserSession;

import java.io.IOException;

import androidx.annotation.Nullable;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.kutman.smanov.sumsartaxidriver.utils.Validation.convertToE164;
import static com.kutman.smanov.sumsartaxidriver.utils.Validation.validateFields;
import static com.kutman.smanov.sumsartaxidriver.utils.Validation.validatePhone;


public class LoginFragment extends Fragment {

    public static final String TAG = LoginFragment.class.getSimpleName();

    private EditText mEtPhone;
    private EditText mEtPassword;
    private Button mBtLogin;
    private TextView mTvRegister;
    private TextView mTvForgotPassword;
    private TextInputLayout mTiPhone;
    private TextInputLayout mTiPassword;
    private ProgressBar mProgressBar;

    private CompositeSubscription mSubscriptions;

    private UserSession session;
    private UserData userData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login,container,false);
        mSubscriptions = new CompositeSubscription();
        session = new UserSession(getActivity().getApplicationContext());
        userData = new UserData(getActivity().getApplicationContext());
        initViews(view);
        if(session.isLoggedIn()){
            Intent intent = new Intent(getActivity(), MapActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
        return view;
    }

    private void initViews(View v) {

        mEtPhone = (EditText) v.findViewById(R.id.et_phone);
        mEtPassword = (EditText) v.findViewById(R.id.et_password);
        mBtLogin = (Button) v.findViewById(R.id.btn_login);
        mTiPhone = (TextInputLayout) v.findViewById(R.id.ti_phone);
        mTiPassword = (TextInputLayout) v.findViewById(R.id.ti_password);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        mTvRegister = (TextView) v.findViewById(R.id.tv_register);
        //mTvForgotPassword = (TextView) v.findViewById(R.id.tv_forgot_password);

        mEtPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        mBtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        mTvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegister();
            }
        });
        /*(mTvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });*/
    }

    private void login() {

        setError();

        String phone = mEtPhone.getText().toString();
        String password = mEtPassword.getText().toString();

        int err = 0;

        if (!validatePhone(phone)) {

            err++;
            mTiPhone.setError("Email should be valid !");
        }

        if (!validateFields(password)) {

            err++;
            mTiPassword.setError("Password should not be empty !");
        }

        if (err == 0) {

            loginProcess(convertToE164(phone),password);
            mProgressBar.setVisibility(View.VISIBLE);

        } else {

            showSnackBarMessage("Enter Valid Details !");
        }
    }

    private void setError() {

        mTiPhone.setError(null);
        mTiPassword.setError(null);
    }

    private void loginProcess(String email, String password) {

        mSubscriptions.add(NetworkUtil.getRetrofit(email, password)
                .login()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Response>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        handleError(e);
                    }

                    @Override
                    public void onNext(Response response) {
                        handleResponse(response);
                    }
                }));
    }

    private void handleResponse(Response response) {

        mProgressBar.setVisibility(View.GONE);

        mEtPhone.setText(null);
        mEtPassword.setText(null);

        session.setLogin(true);
        userData.addUser(response.getMessage(),response.getToken());

        Intent intent = new Intent(getActivity(), MapActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void handleError(Throwable error) {

        mProgressBar.setVisibility(View.GONE);

        if (error instanceof HttpException) {

            Gson gson = new GsonBuilder().create();

            try {

                String errorBody = ((HttpException) error).response().errorBody().string();
                Response response = gson.fromJson(errorBody,Response.class);
                showSnackBarMessage(response.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            showSnackBarMessage("Network Error !");
        }
    }

    private void showSnackBarMessage(String message) {

        if (getView() != null) {

            Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();
        }
    }

    private void goToRegister(){

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        RegisterFragment fragment = new RegisterFragment();
        ft.replace(R.id.fragmentFrame,fragment,RegisterFragment.TAG);
        ft.commit();
    }

    private void showDialog(){

        ResetPasswordDialog fragment = new ResetPasswordDialog();
        fragment.show(getFragmentManager(), ResetPasswordDialog.TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
    }
}
