package com.mintwireless.mintegrate.console.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.Logger;
import com.mintwireless.mintegrate.core.CardDetectionModeController;
import com.mintwireless.mintegrate.core.PaymentCallback;
import com.mintwireless.mintegrate.core.Session;
import com.mintwireless.mintegrate.core.exceptions.MintegrateError;
import com.mintwireless.mintegrate.core.exceptions.MintegrateException;
import com.mintwireless.mintegrate.core.models.ApplicationSelectionItem;
import com.mintwireless.mintegrate.core.requests.SubmitPaymentRequest;
import com.mintwireless.mintegrate.core.requests.VerifySignatureRequest;
import com.mintwireless.mintegrate.core.responses.AddOnFeatureResponse;
import com.mintwireless.mintegrate.core.responses.BaseResponse;
import com.mintwireless.mintegrate.core.responses.SubmitPaymentResponse;
import com.mintwireless.mintegrate.sdk.Mintegrate;
import com.mintwireless.mintegrate.sdk.ReaderConnectionListener;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Jialian on 4/05/16.
 */
public class PaymentActivity extends AppCompatActivity implements
        ProcessDialogFragment.OnCancelCallback {

    private EditText editTextAmount;
    private EditText editTextNote;
    private Button btnSubmitPayment;
    private String authToken;
    private Session session;
    private ProcessDialogFragment processDialogFragment;
    private Toolbar toolbar;

    private boolean mIsFallBack = false;
    private boolean mIsFallForward = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        authToken = getIntent().getStringExtra("authtoken");
        editTextAmount = (EditText) findViewById(R.id.amount);
        editTextNote = (EditText) findViewById(R.id.note);
        btnSubmitPayment = (Button) findViewById(R.id.submit_payment);
        btnSubmitPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitPayment();
            }
        });
        findViewById(R.id.checkReader).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkReader();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.payment_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_transactions)
            openTransactions();
        return true;
    }

    private void openTransactions() {
        Intent intentPayment = new Intent(this, TransactionsActivity.class);
        intentPayment.putExtra("authtoken", authToken);
        startActivity(intentPayment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Mintegrate.handleGoingToForeground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Mintegrate.handleGoingToBackground();
    }

    private void submitPayment() {

        try {

            SubmitPaymentRequest submitPaymentRequest = new SubmitPaymentRequest();

            // amount in cents
            submitPaymentRequest.setAmount(editTextAmount.getText().toString()); // required
            submitPaymentRequest.setNotes(editTextNote.getText().toString());
            submitPaymentRequest.setAuthToken(authToken); // required

            submitPaymentRequest.setTippingEnabled(false);
            submitPaymentRequest.setSurchargeAmount("0");
            submitPaymentRequest.setSignOnPaper(false);

            // you can change the card detection mode by calling setCardDetectionMode
            // default is CardDetectionModeAll if not set
            submitPaymentRequest.setCardDetectionMode(CardDetectionModeController.CARD_DETECTION_MODE.ALL);
            session = Mintegrate.submitPayment(submitPaymentRequest, new PaymentCallback() {
                @Override
                public void onDuplicateTransactionFound(Session session) {

                }

                @Override
                public void onAddOnFeatureStateChanged(AddOnFeatureResponse addOnFeatureResponse) {

                }

                @Override
                public void onPrepareToWaitForCard(Session session, CardDetectionModeController cardDetectionModeController) {
                    processDialogFragment.setProcessMessage("Please tap/insert/swipe card...");
                }

                @Override
                public void onCardApplicationSelection(Session session, ArrayList<ApplicationSelectionItem> arrayList) {

                }

                @Override
                public void onWaitForSignature(Session session, SubmitPaymentResponse submitPaymentResponse) {

                }

                @Override
                public void onWaitForSignatureVerification(Session session) {
                    // called after onWaitForSignature and let the merchant to verify the customer's signature with a
                    // verification PIN (default is 0000 and can be changed using the SDK's configure operation)

                    VerifySignatureRequest request = new VerifySignatureRequest();

                    // setCancelling set to YES and SDK will call onWaitForSignature again so integrator needs
                    // to redisplay the customer signature screen
                    request.setCancelling(false);

                    // LastAttempt should be set to YES when this is the last try so that
                    // SDK will be informed and call the onError callback when error occurs
                    // and integrator will have to make sure not to allow user to try and enter a verification pin
                    // and close the session accordingly. If the verification PIN is correct then SDK will proceed
                    // with the next operation state.
                    request.setLastAttempt(false);

                    request.setPin("your pin");

                    session.nextWithParameter(request);
                }

                @Override
                public void onWaitForRemoveCard(Session session, SubmitPaymentResponse submitPaymentResponse) {
                    processDialogFragment.setProcessMessage("Please remove card");
                }

                @Override
                public void onUpdatingReader(String s, float v) {
                    processDialogFragment.setProcessMessage(s+" = "+v+"%");
                }

                @Override
                public void onReaderStatusMessageReceived(String s) {
                    processDialogFragment.setProcessMessage(s);
                }

                @Override
                public void onProgress(String s) {
                    processDialogFragment.setProcessMessage(s);
                    processDialogFragment.setCancelProcess(false);
                }

                @Override
                public void onCompletion(Session session, SubmitPaymentResponse submitPaymentResponse) {
                    onCompletionHandler(session, submitPaymentResponse);
                }

                @Override
                public void onError(Session session, MintegrateError.Error error) {
                    onErrorHandler(session, error);
                }
            }, this);
            processDialogFragment = ProcessDialogFragment.newInstance("initialising transaction");
            processDialogFragment.setCancelCallback(this);
            processDialogFragment.setCancelable(false);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(processDialogFragment, "loadingFragment").commit();
            session.next();

        } catch (MintegrateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    public void onErrorHandler(Session session, MintegrateError.Error error) {
        Logger.logDebug(PaymentActivity.class, "onError: " + error.getMessage());

        // handle errors
        // please refer to the SDK's documentation for more detail about the error code
        switch (error.getCode()) {
            case MintegrateError.ERROR_COMMON_CARD_READER_NOT_CONNECTED:
                processDialogFragment.setProcessMessage("Waiting for reader...");
                break;
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_PIN_MISMATCH:
                break;
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_READ_ERROR: {
                String message = "Please Insert or Swipe Card";
                if (mIsFallBack) {
                    message = "Please Swipe Card";
                } else if (mIsFallForward) {
                    message = "Please Insert Card";
                }
                processDialogFragment.setProcessMessage(message);
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLBACK: {
                mIsFallBack = true;
                processDialogFragment.setProcessMessage("Please Swipe Card");
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLFORWARD: {
                mIsFallForward = true;
                processDialogFragment.setProcessMessage("Please Insert Card");
                break;
            }

            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_TRANSACTION_DECLINED_FALLBACK_TO_CHIP: {
                processDialogFragment.dismiss();
                closeSession(session);
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP: {
                processDialogFragment.setProcessMessage("Please Insert Card");
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_NOT_SUPPORTED: {
                processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP_OR_SWIPE: {
                processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_ACCOUNT_SELECTION_TIME_OUT:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_USER_CANCELLED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_PIN_TIME_OUT:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_REMOVED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_CANCEL_FAILED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_UNKNOWN_STATUS: {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession(session);
                break;
            }
            case MintegrateError.ERROR_SUBMIT_REFUND_WAITING_FOR_MERCHANT_SIGNATURE_TIMEOUT: {
                Toast.makeText(this, "Transaction Cancelled.Timeout waiting for merchant signature.", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession(session);
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAITING_FOR_CARD_TIMEOUT: {
                Toast.makeText(this, "Time out waiting for card", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession(session);
                break;
            }
            case MintegrateError.ERROR_SEND_RECEIPT:
//            case MintegrateError.ERROR_SEND_RECEIPT_PRINTER_NOT_CONNECTED:
            case MintegrateError.ERROR_GET_USER_RECEIPT:
            default:
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession(session);
        }
    }

    public void onCompletionHandler(Session session, BaseResponse baseResponse) {
        // Transaction is finished
        // always keep in mind, close the session
        SubmitPaymentResponse submitPaymentResponse = (SubmitPaymentResponse) baseResponse;
        SubmitPaymentResponse.Status status = submitPaymentResponse.getTransactionStatus();
        processDialogFragment.dismiss();
        closeSession(session);
        String message = null;
        switch (status) {
            case APPROVED:
                message = "Transaction Approved";
                break;
            case DECLINED:
                message = "Transaction Declined";
                break;
            case CANCELLED:
                message = "Transaction Cancelled";
        }

        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }

    @Override
    public void onCancel() {
        closeSession(session);
    }

    private void closeSession(Session session) {
        Logger.logInfo(PaymentActivity.class, "Close Session");
        if (session != null)
            session.close();
    }

    public void checkReader(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        BluetoothDevice deviceFound = null;
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.

            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (!TextUtils.isEmpty(deviceName) && deviceName.equalsIgnoreCase("Mint mPOS 946")) {
                    deviceFound = device;
                    break;
                }
            }
            if (deviceFound != null) {
                Mintegrate.getReaderConnectionManager().connect(deviceFound,
                        new ReaderConnectionListener() {
                            @Override
                            public void onCompletion() {
                                // reader is connected, you can perform Bluetooth related SDK operations
                                Toast.makeText(PaymentActivity.this, "Reader found",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(MintegrateError.Error error) {
                                // reader connection failed
                                Toast.makeText(PaymentActivity.this, "Error: "
                                                + error.getMessage() + " CODE: "+error.getCode(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}
