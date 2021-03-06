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
import com.kutman.smanov.sumsartaxidriver.models.User;
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

public class RegisterFragment extends Fragment {

    public static final String TAG = RegisterFragment.class.getSimpleName();

    private EditText mEtName;
    private EditText mEtPhone;
    private EditText mEtPassword;
    private Button mBtRegister;
    private TextView mTvLogin;
    private TextInputLayout mTiName;
    private TextInputLayout mTiPhone;
    private TextInputLayout mTiPassword;
    private ProgressBar mProgressbar;

    private CompositeSubscription mSubscriptions;

    private UserData userData;
    private UserSession session;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_register,container,false);
        mSubscriptions = new CompositeSubscription();
        userData = new UserData(getActivity().getApplicationContext());
        session = new UserSession(getActivity().getApplicationContext());
        initViews(view);
        return view;
    }

    private void initViews(View v) {

        mEtName = (EditText) v.findViewById(R.id.et_name);
        mEtPhone = (EditText) v.findViewById(R.id.et_phone);
        mEtPassword = (EditText) v.findViewById(R.id.et_password);
        mBtRegister = (Button) v.findViewById(R.id.btn_register);
        mTvLogin = (TextView) v.findViewById(R.id.tv_login);
        mTiName = (TextInputLayout) v.findViewById(R.id.ti_name);
        mTiPhone = (TextInputLayout) v.findViewById(R.id.ti_phone);
        mTiPassword = (TextInputLayout) v.findViewById(R.id.ti_password);
        mProgressbar = (ProgressBar) v.findViewById(R.id.progress);

        mEtPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        mBtRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
        mTvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
            }
        });
    }

    private void register() {

        setError();

        String name = mEtName.getText().toString();
        String phone = mEtPhone.getText().toString();
        String password = mEtPassword.getText().toString();

        int err = 0;

        if (!validateFields(name)) {

            err++;
            mTiName.setError("Name should not be empty !");
        }

        if (!validatePhone(phone)) {

            err++;
            mTiPhone.setError("Email should be valid !");
        }

        if (!validateFields(password)) {

            err++;
            mTiPassword.setError("Password should not be empty !");
        }

        if (err == 0) {

            User user = new User();
            user.setName(name);
            user.setPhone(convertToE164(phone));
            user.setPassword(password);

            mProgressbar.setVisibility(View.VISIBLE);
            registerProcess(user);

        } else {

            showSnackBarMessage("Enter Valid Details !");
        }
    }

    private void setError() {

        mTiName.setError(null);
        mTiPhone.setError(null);
        mTiPassword.setError(null);
    }

    private void registerProcess(User user) {

        mSubscriptions.add(NetworkUtil.getRetrofit().register(user)
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

        mProgressbar.setVisibility(View.GONE);
        session.setLogin(true);
        userData.addUser(response.getMessage(),response.getToken());

        Intent intent = new Intent(getActivity(), MapActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void handleError(Throwable error) {

        mProgressbar.setVisibility(View.GONE);

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

    private void goToLogin(){

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        LoginFragment fragment = new LoginFragment();
        ft.replace(R.id.fragmentFrame, fragment, LoginFragment.TAG);
        ft.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
    }
}
