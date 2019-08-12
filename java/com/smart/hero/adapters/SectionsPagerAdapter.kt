package com.smart.hero.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.smart.hero.CardFragment
import com.smart.hero.R
import com.smart.hero.ProfileDataFragment

class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        return when(position) {
            0-> ProfileDataFragment.newInstance()
            else-> CardFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0-> context.resources.getString(R.string.profile_label_tab1)
            else -> context.resources.getString(R.string.profile_label_tab2)
        }
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 2
    }
}