package assistive.com.assistivemacros;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static assistive.com.assistivemacros.AssistiveMacros.PlaceholderFragment.isAccessibilityEnabled;

public class AssistiveMacros extends AppCompatActivity {

    private static final int CREATE=1;
    private static final int MANAGE=2;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistive_macros);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //verify if service is enable
                if(isAccessibilityEnabled(getApplicationContext(),MacroService.SERVICE_ID)) {
                    //add button overlay
                    Intent i = new Intent(getApplicationContext(), OverlayService.class);
                    startService(i);

                    //go to homescreen
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                }else{
                    //send to accessibility settings
                    Toast.makeText(getApplicationContext(),"Need to activate Assistive Macros", Toast.LENGTH_LONG ).show();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_assistive_macros, menu);
        return true;
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Create Macro";
                case 1:
                    return "Manage Macros";
            }
            return null;
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private ExpandableListView macroList;
        private static final String ARG_SECTION_NUMBER = "tabMenu";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView=null;
            Log.d(MacroService.TAG, "SECTION:" + getArguments().getInt(ARG_SECTION_NUMBER));

            switch(getArguments().getInt(ARG_SECTION_NUMBER)){
                case CREATE:
                     rootView = inflater.inflate(R.layout.fragment_create_macros, container, false);

                    break;
                case MANAGE:
                     rootView = inflater.inflate(R.layout.fragment_manage_macros, container, false);
                    macroList = (ExpandableListView) rootView.findViewById(R.id.listView);
                    //populateListView(macroList);
                    break;

            }
            return rootView;
        }
        @Override
        public void onResume() {
            super.onResume();
            if(macroList!=null)
                populateListView(macroList);
        }


        //Populate list view with all available macros
        private void populateListView(ExpandableListView macroList) {
            MacroManagment mm = MacroManagment.sharedInstance();
            ExpandableListAdapter listAdapter;
            HashMap<String, ArrayList<String>> macrosStored = mm.getMacroList();
            ArrayList <String> macroHeading = new ArrayList<String>();
            macroHeading.addAll(macrosStored.keySet());
            listAdapter = new ExpandableListAdapter(getActivity(), macroHeading, macrosStored);

            // setting list adapter
            macroList.setAdapter(listAdapter);

        }

        public static boolean isAccessibilityEnabled(Context context, String id) {

            AccessibilityManager am = (AccessibilityManager) context
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);

            List<AccessibilityServiceInfo> runningServices = am
                    .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
            for (AccessibilityServiceInfo service : runningServices) {
                Log.d(MacroService.TAG, "acc:" +id );
                Log.d(MacroService.TAG, "acc:" +service.getId() );

                if (id.equals(service.getId())) {
                    return true;
                }
            }

            return false;
        }
    }
}
