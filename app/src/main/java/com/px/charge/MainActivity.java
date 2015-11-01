package com.px.charge;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.px.charge.ui.AddFragment;
import com.px.charge.ui.ListFragment;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    private final ArrayList<Fragment> mFragments = new ArrayList<>(2);
    private Class[] classes = {AddFragment.class, ListFragment.class};
    private String[] drawerTitles = {"新建", "历史"};
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_activity);

        mFragments.add(new AddFragment());
        mFragments.add(new ListFragment());

        findViews();
//        selectItem(0);
    }

    private void findViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
//        ((LinearLayout.LayoutParams) mToolbar.getLayoutParams()).setMargins(0, ScreenUtils.getStatusBarHeight(this), 0, 0);
        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        if (null != ab) {
//            ab.setHomeAsUpIndicator(R.drawable.ic_menu);
            ab.setDisplayHomeAsUpEnabled(true);
        }
        ViewPager pager = (ViewPager) findViewById(R.id.content);
        pager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    private void selectItem(int position) {
        FragmentTransaction fragmentTransaction = this.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        //先隐藏所有fragment
        for (Fragment fragment : mFragments) {
            if (null != fragment) fragmentTransaction.hide(fragment);
        }

        Fragment fragment;
        if (position >= mFragments.size() || null == mFragments.get(position)) {
            Bundle bundle = new Bundle();
//            bundle.putString(Constant.TITLE, drawerTitles[position]);
            fragment = Fragment.instantiate(this, classes[position].getName(), bundle);
            mFragments.set(position, fragment);
            // 如果Fragment为空，则创建一个并添加到界面上
            fragmentTransaction.add(R.id.content, fragment);
        } else {
            // 如果Fragment不为空，则直接将它显示出来
            fragment = mFragments.get(position);

            fragmentTransaction.show(fragment);
        }
        fragmentTransaction.commit();

        getSupportActionBar().setTitle(drawerTitles[position]);
    }

//    @Override
//    protected BaseFragment newFragmentByTag(String tag) {
//        if (TextUtils.equals(tag, AddFragment.TAG)) {
//            return new ListFragment();
//        }
//        return super.newFragmentByTag(tag);
//    }
}
