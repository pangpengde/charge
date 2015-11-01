package com.px.charge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by pangpengde on 15/8/10.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasCustomContentView()) {
            setContentView(R.layout.base_activity);
        }
    }

//    public BaseFragment showFragment(String tag, String title, Bundle bundle, boolean addToBackStack) {
//        FragmentManager fm = getFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//
//        BaseFragment fragment = (BaseFragment) fm.findFragmentByTag(tag);
//        if (fragment == null) {
//            fragment = newFragmentByTag(tag);
//            if (bundle != null) {
//                fragment.setArguments(bundle);
//            }
//        }
//
//        if (fragment == null) {
//            return null;
//        }
//
//        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//        ft.replace(R.id.content, fragment, tag);
//        if (addToBackStack) {
//            ft.addToBackStack(tag);
//        }
////        ft.commitAllowingStateLoss();
//        ft.commit();
//
//        return fragment;
//    }

//    protected BaseFragment newFragmentByTag(String tag) {
//        return null;
//    }
    protected boolean hasCustomContentView() {
        return false;
    }

}
