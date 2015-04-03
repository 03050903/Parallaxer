package pl.schibsted.parallaxersample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pl.schibsted.parallaxer.Parallaxer;

/**
 * Created by Jacek Kwiecień on 03.04.15.
 */
public class MainFragment extends Fragment {

    private Parallaxer parallaxer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parallaxer = new Parallaxer()
                .actionBarBackground(R.drawable.shape_toolbar_bg)
                .headerLayout(R.layout.view_header)
                .contentLayout(R.layout.view_content);

        View view = parallaxer.createView(getActivity());
        parallaxer.init((ActionBarActivity) getActivity());
        return view;
    }
}
