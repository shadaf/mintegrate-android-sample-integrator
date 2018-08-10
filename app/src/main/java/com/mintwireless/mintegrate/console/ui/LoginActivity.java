
package com.mintwireless.mintegrate.console.ui;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.LoadingHelper;
import com.mintwireless.mintegrate.core.Session;
import com.mintwireless.mintegrate.core.exceptions.MintegrateError;
import com.mintwireless.mintegrate.core.exceptions.MintegrateException;
import com.mintwireless.mintegrate.core.requests.SubmitLoginRequest;
import com.mintwireless.mintegrate.core.responses.LoginResponse;
import com.mintwireless.mintegrate.sdk.Mintegrate;


public class LoginActivity extends AppCompatActivity implements SubmitLoginRequest.LoginCallback,
        View.OnClickListener {

    private Button mBtnLogin;
    private EditText mEtxtEmail;
    private EditText mEtxtPassword;
    private Session session;

    private String apiKey = "zJlMblxFP21rBtj6eS55";
    private String secrekey = "reycHj0sCSJp15aJ2DnE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mEtxtEmail = (EditText) findViewById(R.id.txt_email);
        mEtxtPassword = (EditText) findViewById(R.id.txt_password);

        mBtnLogin.setOnClickListener(this);
        Mintegrate.initialise(this);
        Mintegrate.setApiKeyClientSecretKey(apiKey, secrekey,"au_mintmpos");

    }

    @Override
    public void onClick(View v) {
        submitLogin();
    }

    private void submitLogin() {

        // If using offline SDK, pass in "user1" as username to trigger user activation flow,
        // otherwise just pass in a different value

        LoadingHelper.showLoading(getSupportFragmentManager());
        try {
            SubmitLoginRequest submitLoginRequest = new SubmitLoginRequest();
            submitLoginRequest.setUserID("mangalya.pratap");//mEtxtEmail.getText().toString());
            submitLoginRequest.setUserPin("333333");//mEtxtPassword.getText().toString());
            session = Mintegrate.submitLogin(submitLoginRequest, this);
            session.next();

        } catch (MintegrateException e) {
            Log.d(LoginActivity.this.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
        }
    }

    @Override
    public void onError(Session session, MintegrateError.Error error) {
        int errorCode = error.getCode();
        LoadingHelper.hideLoading(getSupportFragmentManager());

        // Handle errors
        // you can either close the session or
        // handle specified error

        if (errorCode == MintegrateError.ERROR_USER_ACTIVATION_FAIL) {
            showActivation();
        } else if (errorCode == MintegrateError.ERROR_SET_NEW_PIN_FAIL) {
            showResetPin();
        } else {
            Log.d(LoginActivity.this.getClass().getSimpleName(), error.getMessage());

            // always keep in mind that close the session after we are done with the operation when
            // onCompletion or onError is called
            closeSession();
        }
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onWaitForTerms(Session session) {

        // Display terms and conditions if you have one
        // If agreed to terms call next otherwise call close

        LoadingHelper.hideLoading(getSupportFragmentManager());
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        builder.setCancelable(false);
        builder.setTitle("Terms & Conditions");
        builder.setMessage("Agree to Terms?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                next(null);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeSession();
            }
        });
        builder.show();
    }

    private void next(Object param) {
        session.nextWithParameter(param);
    }

    private void closeSession() {
        if (session != null)
            session.close();
    }

    @Override
    public void onWaitForUserActivation(Session session) {
        LoadingHelper.hideLoading(getSupportFragmentManager());
        showActivation();
    }

    private void showActivation() {

        // Prompt user to enter activation code sent to their phones when not using the offline SDK
        // Then pass it in as the parameter value
        // Activation code for Offline SDK is 0000

        UserActivationFragment userActivationFragment = UserActivationFragment.newInstance();
        userActivationFragment.setCallback(new UserActivationFragment.OnActivationCallback() {
            @Override
            public void onCancel() {
                closeSession();
            }

            @Override
            public void onConfirmActivationCode(String activationCode) {
                if (activationCode.length() < 4 || activationCode.length() > 4) {
                    Toast.makeText(LoginActivity.this, "Please enter 4 digits code", Toast.LENGTH_SHORT).show();
                    showActivation();
                } else {
                    LoadingHelper.showLoading(getSupportFragmentManager());
                    next(activationCode);
                }
            }
        });
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction().add(userActivationFragment, "user_activation");
        fragmentTransaction.commit();
    }

    @Override
    public void onWaitForChangePIN(Session session) {
        LoadingHelper.hideLoading(getSupportFragmentManager());
        showResetPin();
    }

    private void showResetPin() {

        // Prompt user to enter valid new pin
        // Then pass in as parameter the new pin
        // Valid PIN to set for Offline SDK is 123456

        ResetPinFragment resetPinFragment = ResetPinFragment.newInstance();
        resetPinFragment.setCallback(new ResetPinFragment.OnResetPINCallback() {
            @Override
            public void onCancel() {
                closeSession();
            }

            @Override
            public void onContinue(String pin) {
                LoadingHelper.showLoading(getSupportFragmentManager());
                next(pin);
            }
        });
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction().add(resetPinFragment, "reset_pin");
        fragmentTransaction.commit();
    }

    @Override
    public void onCompletion(Session session, LoginResponse loginResponse) {
        LoadingHelper.hideLoading(getSupportFragmentManager());

        // always keep in mind that close the session after we are done with the operation when
        // onCompletion or onError is called
        session.close();

        String authToken = loginResponse.getAuthToken();

        Intent intentPayment = new Intent(this, PaymentActivity.class);
        intentPayment.putExtra("authtoken", authToken);
        startActivity(intentPayment);
        finish();
    }
}
