package com.ternaryop.photoshelf.drawer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.drawer.adapter.DrawerItem;

/**
 * Created by dave on 28/10/17.
 * Contain info about the item to test
 */

public class ItemTestDrawerItem extends DrawerItem {
    public ItemTestDrawerItem(int itemId, String title, Class<? extends Fragment> fragmentClass) {
        super(itemId, title, fragmentClass);
    }

    public Fragment instantiateFragment(Context context) {
        final Fragment fragment = super.instantiateFragment(context);
        Bundle args = new Bundle();
        args.putString(Constants.EXTRA_URL, context.getString(R.string.test_page_url));
        fragment.setArguments(args);

        return fragment;
    }
}
