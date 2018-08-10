package com.mintwireless.mintegrate.console.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.LoadingHelper;
import com.mintwireless.mintegrate.console.utils.Logger;
import com.mintwireless.mintegrate.core.CardDetectionModeController;
import com.mintwireless.mintegrate.core.RefundCallback;
import com.mintwireless.mintegrate.core.ResponseCallback;
import com.mintwireless.mintegrate.core.Session;
import com.mintwireless.mintegrate.core.exceptions.MintegrateError;
import com.mintwireless.mintegrate.core.exceptions.MintegrateException;
import com.mintwireless.mintegrate.core.models.ApplicationSelectionItem;
import com.mintwireless.mintegrate.core.requests.GetTransactionDetailsRequest;
import com.mintwireless.mintegrate.core.requests.SubmitRefundRequest;
import com.mintwireless.mintegrate.core.requests.VerifySignatureRequest;
import com.mintwireless.mintegrate.core.responses.BaseResponse;
import com.mintwireless.mintegrate.core.responses.GetTransactionDetailsResponse;
import com.mintwireless.mintegrate.core.responses.GetTransactionsResponse;
import com.mintwireless.mintegrate.core.responses.SubmitPaymentResponse;
import com.mintwireless.mintegrate.sdk.Mintegrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Jialian on 5/05/16.
 */
public class TransactionDetailActivity extends AppCompatActivity implements
        ProcessDialogFragment.OnCancelCallback, RefundCallback {

    private String authtoken;
    private ProcessDialogFragment processDialogFragment;
    private Toolbar toolbar;
    private GetTransactionsResponse.TransactionSummary transactionSummary;
    private GetTransactionDetailsResponse getTransactionDetailsResponse;
    private Session mSession;
    private boolean isLoadingDetail = false;
    private boolean mIsFallBack = false;
    private boolean mIsFallForward = false;

    private TextView textViewAmount;
    private TextView textViewSTAN;
    private TextView textViewAccountType;
    private TextView textViewDate;
    private TextView textViewPAN;
    private TextView textViewNote;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_transaction_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        textViewAmount = (TextView) findViewById(R.id.amount);
        textViewSTAN = (TextView) findViewById(R.id.stan);
        textViewAccountType = (TextView) findViewById(R.id.account_type);
        textViewDate = (TextView) findViewById(R.id.date);
        textViewPAN = (TextView) findViewById(R.id.masked_pan);
        textViewNote = (TextView) findViewById(R.id.note);
        setSupportActionBar(toolbar);
        authtoken = getIntent().getStringExtra("authtoken");
        transactionSummary = getIntent().getExtras().getParcelable("transaction");
        LoadingHelper.showLoading(getSupportFragmentManager());
        isLoadingDetail = true;
        processDialogFragment = ProcessDialogFragment.newInstance("initialising transaction");
        processDialogFragment.setCancelCallback(this);
        processDialogFragment.setCancelable(false);
        loadTransactionDetails(transactionSummary);
    }

    private void loadTransactionDetails(GetTransactionsResponse.TransactionSummary transactionSummary) {
        try {
            GetTransactionDetailsRequest getTransactionDetailsRequest = new GetTransactionDetailsRequest();
            getTransactionDetailsRequest.setAuthToken(authtoken); // required
            getTransactionDetailsRequest.setTransactionSummary(transactionSummary); // required
            mSession = Mintegrate.getTransactionDetails(getTransactionDetailsRequest, new ResponseCallback<GetTransactionDetailsResponse>() {
                @Override
                public void onCompletion(Session session, GetTransactionDetailsResponse getTransactionDetailsResponse) {
                    showTransactionDetail(getTransactionDetailsResponse);
                }

                @Override
                public void onError(Session session, MintegrateError.Error error) {

                }
            });
            mSession.next();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refund) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            processDialogFragment.setCancelCallback(this);
            fragmentTransaction.add(processDialogFragment, "loadingFragment").commit();
            submitRefund();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeSession();
        finish();
    }


    private void showTransactionDetail(GetTransactionDetailsResponse getTransactionDetailsResponse) {

        String amount = "$" + convertCentToDecimalString(getTransactionDetailsResponse.getAmount());
        String stan = getTransactionDetailsResponse.getMaskedPAN();
        String maskedPan = getTransactionDetailsResponse.getMaskedPAN();
        String note = getTransactionDetailsResponse.getNotes();

        textViewAmount.setText(amount);
        textViewSTAN.setText(stan);

        String localDate = getEEEddMMyyhhmmFromString(getTransactionDetailsResponse.getTransLocalDate(), "dd/MM/yyyy hh:mm:ss");
        String timezone = getTransactionDetailsResponse.getTransTimezoneDSTString();
        textViewDate.setText(localDate + " (" + timezone + ")");

        textViewPAN.setText(maskedPan);
        textViewNote.setText(note);

        String cardType = getTransactionDetailsResponse.getAccountType();

        if (cardType != null) {
            if (cardType.equalsIgnoreCase("CR")) {
                cardType = "Credit";
            } else if (cardType.equalsIgnoreCase("CHQ")) {
                cardType = "Cheque";
            } else if (cardType.equalsIgnoreCase("SAV")) {
                cardType = "Saving";
            }
            textViewAccountType.setText("Account Type: " + cardType);
        }
    }

    private String convertCentToDecimalString(String cent) {
        Double d = Double.parseDouble(cent) / 100;
        String str = new String("");
        DecimalFormat localDecimalFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        str = localDecimalFormat.format(d);
        return str;
    }

    private String getEEEddMMyyhhmmFromString(String localDate, String inputFormat) {

        String formattedDate;
        try {
            formattedDate =
                    new SimpleDateFormat("EEE, dd/MM/yy hh:mm").format(new SimpleDateFormat(inputFormat).parse(localDate));
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void closeSession() {
        Logger.logInfo(PaymentActivity.class, "Close Session");
        if (mSession != null)
            mSession.close();
    }

    @Override
    public void onCancel() {
        closeSession();
    }

    private void submitRefund() {

        try {

            SubmitRefundRequest submitRefundRequest = new SubmitRefundRequest();
            submitRefundRequest.setAmount(getTransactionDetailsResponse.getAmount()); // required
            submitRefundRequest.setAuthToken(authtoken); // required
            submitRefundRequest.setRefundSourceTransactionRequestId("transaction request id"); // required
            mSession = Mintegrate.submitRefund(submitRefundRequest, this, this);
            mSession.next();

        } catch (MintegrateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWaitForMerchantSignature(Session session, GetTransactionDetailsResponse getTransactionDetailsResponse) {

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
                closeSession();
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
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_REFUND_WAITING_FOR_MERCHANT_SIGNATURE_TIMEOUT: {
                Toast.makeText(this, "Transaction Cancelled.Timeout waiting for merchant signature.", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAITING_FOR_CARD_TIMEOUT: {
                Toast.makeText(this, "Time out waiting for card", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SEND_RECEIPT:
//            case MintegrateError.ERROR_SEND_RECEIPT_PRINTER_NOT_CONNECTED:
            case MintegrateError.ERROR_GET_USER_RECEIPT:
            default:
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
        }
    }

    public void onCompletionHandler(Session session, BaseResponse baseResponse) {
        // close session
        closeSession();

        if (baseResponse instanceof GetTransactionDetailsResponse) {
            LoadingHelper.hideLoading(getSupportFragmentManager());
            getTransactionDetailsResponse = (GetTransactionDetailsResponse) baseResponse;
            showTransactionDetail(getTransactionDetailsResponse);
            isLoadingDetail = false;
        } else if (baseResponse instanceof SubmitPaymentResponse) {
            SubmitPaymentResponse submitPaymentResponse = (SubmitPaymentResponse) baseResponse;
            SubmitPaymentResponse.Status status = submitPaymentResponse.getTransactionStatus();
            processDialogFragment.dismiss();
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

    }

}
