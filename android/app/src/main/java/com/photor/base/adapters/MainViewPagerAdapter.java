package com.photor.base.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * Created by xujian on 2018/2/26.
 */

public class MainViewPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;

    public MainViewPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {

        Logger.d("MainViewPagerAdapter: " + position);

        return fragments.get(position);
    }

}
