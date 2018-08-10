package com.mintwireless.mintegrate.console.utils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.mintwireless.mintegrate.console.ui.LoadingDialogFragment;

/**
 * Created by Jialian on 9/05/16.
 */
public class LoadingHelper {

    public static void showLoading(FragmentManager fragmentManager) {
        LoadingDialogFragment loadingDialogFragment = LoadingDialogFragment.newInstance();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().add(loadingDialogFragment, "loading");
        fragmentTransaction.commit();
    }

    public static void hideLoading(FragmentManager fragmentManager) {
        Fragment fragment = fragmentManager.findFragmentByTag("loading");
        if (fragment instanceof LoadingDialogFragment)
            ((LoadingDialogFragment) fragment).dismiss();
    }

}
