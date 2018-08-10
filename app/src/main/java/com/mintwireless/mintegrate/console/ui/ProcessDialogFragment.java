package com.mintwireless.mintegrate.console.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.Logger;
import com.mintwireless.mintegrate.core.responses.GetTransactionDetailsResponse;

/**
 * Created by Jialian on 5/05/16.
 */
public class ProcessDialogFragment extends AppCompatDialogFragment implements View.OnClickListener {

    private TextView textViewMessage;
    private String message;
    private Button btnCancel;
    private OnCancelCallback cancelCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        Bundle bundle = getArguments();
        if (bundle != null)
            message = bundle.getString("message", "");
    }

    public static ProcessDialogFragment newInstance(String message) {
        ProcessDialogFragment processDialogFragment = new ProcessDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        processDialogFragment.setArguments(bundle);
        return processDialogFragment;
    }

    public static ProcessDialogFragment newInstance() {
        ProcessDialogFragment processDialogFragment = new ProcessDialogFragment();
        return processDialogFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_process, container, false);
        btnCancel = (Button) view.findViewById(R.id.process_cancel);
        textViewMessage = (TextView) view.findViewById(R.id.process_message);

        btnCancel.setOnClickListener(this);
        textViewMessage.setText(message);
        return view;
    }

    public void setProcessMessage(String message) {
        if (textViewMessage != null)
            textViewMessage.setText(message);
    }

    public void setCancelCallback(OnCancelCallback cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    @Override
    public void onClick(View view) {
        if (cancelCallback != null)
            cancelCallback.onCancel();
        dismiss();
    }

    @Override
    public void onDestroy() {
        Logger.logInfo(ProcessDialogFragment.class, "onDestroy");
        cancelCallback = null;
        super.onDestroy();
    }

    public void setCancelProcess(boolean cancel) {
        if (btnCancel == null)
            return;

        if (cancel) {
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }
    }

    public interface OnCancelCallback {
        void onCancel();
    }

}
