package com.mintwireless.mintegrate.console.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mintwireless.mintegrate.console.R;

/**
 * Created by Jialian on 4/05/16.
 */
public class LoadingDialogFragment extends AppCompatDialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.LoadingDialog);
        setCancelable(false);
    }

    public static LoadingDialogFragment newInstance(){
        LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();
        return loadingDialogFragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        return view;
    }
}
