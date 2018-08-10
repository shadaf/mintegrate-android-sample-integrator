package com.mintwireless.mintegrate.console.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.Logger;

/**
 * Created by Jialian on 5/05/16.
 */
public class ResetPinFragment extends AppCompatDialogFragment implements View.OnClickListener {

    private Button btnCancel;
    private Button btnContinue;
    private EditText textViewPin_1;
    private EditText textViewPin_2;
    private OnResetPINCallback onResetPINCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    public static ResetPinFragment newInstance() {
        ResetPinFragment processDialogFragment = new ResetPinFragment();
        return processDialogFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reset_pin, container, false);
        btnCancel = (Button) view.findViewById(R.id.cancel);
        btnContinue = (Button) view.findViewById(R.id.next);

        textViewPin_1 = (EditText) view.findViewById(R.id.new_pin_1);
        textViewPin_2 = (EditText) view.findViewById(R.id.new_pin_2);
        btnCancel.setOnClickListener(this);
        btnContinue.setOnClickListener(this);
        return view;
    }

    public void setCallback(OnResetPINCallback onResetPINCallback) {
        this.onResetPINCallback = onResetPINCallback;
    }

    @Override
    public void onClick(View view) {

        if (onResetPINCallback != null) {
            int id = view.getId();
            if (id == R.id.cancel) {
                onResetPINCallback.onCancel();
            } else if (id == R.id.next) {
                String pin1 = textViewPin_1.getText().toString();
                String pin2 = textViewPin_2.getText().toString();
                if (!pin1.equals(pin2)) {
                    Toast.makeText(getActivity(), "PINs do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(pin2.length() > 6 || pin2.length() < 6){
                    Toast.makeText(getActivity(), "PINs must be 6 digits", Toast.LENGTH_SHORT).show();
                    return;
                }

                onResetPINCallback.onContinue(textViewPin_2.getText().toString());
            }
        }
        dismiss();
    }

    @Override
    public void onDestroy() {
        Logger.logInfo(ResetPinFragment.class, "onDestroy");
        onResetPINCallback = null;
        super.onDestroy();
    }

    public interface OnResetPINCallback {
        void onCancel();

        void onContinue(String pin);
    }

}
