package com.mintwireless.mintegrate.console.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.Logger;

/**
 * Created by Jialian on 5/05/16.
 */
public class UserActivationFragment extends AppCompatDialogFragment implements View.OnClickListener {

    private Button btnCancel;
    private Button btnActivate;
    private EditText textViewActivationCode;
    private OnActivationCallback onActivationCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    public static UserActivationFragment newInstance() {
        UserActivationFragment processDialogFragment = new UserActivationFragment();
        return processDialogFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_activation, container, false);
        textViewActivationCode = (EditText) view.findViewById(R.id.activation_code);
        btnCancel = (Button) view.findViewById(R.id.cancel);
        btnActivate = (Button) view.findViewById(R.id.activate);
        btnCancel.setOnClickListener(this);
        btnActivate.setOnClickListener(this);
        return view;
    }

    public void setCallback(OnActivationCallback onActivationCallback) {
        this.onActivationCallback = onActivationCallback;
    }

    @Override
    public void onClick(View view) {

        if (onActivationCallback != null) {
            int id = view.getId();
            if (id == R.id.cancel) {
                onActivationCallback.onCancel();
            } else if (id == R.id.activate) {
                onActivationCallback.onConfirmActivationCode(textViewActivationCode.getText().toString());
            }
        }
        dismiss();
    }

    @Override
    public void onDestroy() {
        Logger.logInfo(UserActivationFragment.class, "onDestroy");
        onActivationCallback = null;
        super.onDestroy();
    }

    public interface OnActivationCallback {
        void onCancel();

        void onConfirmActivationCode(String activationCode);
    }

}
