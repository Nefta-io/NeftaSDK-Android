package com.nefta.direct;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nefta.sdk.Placement;

public class PlacementController extends Fragment implements IAdUnitCallback {

    private Placement _placement;
    private Activity _activity;

    public PlacementController() { }

    public void Init(Placement placement, Activity activity) {
        _placement = placement;
        _activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.placement_controller, container, false);
        ((TextView)view.findViewById(R.id.id)).setText(_placement._id);
        ((TextView)view.findViewById(R.id.type)).setText(_placement._type.toString());

        Button createButton = (Button)view.findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AdUnitController adUnit = null;
                if (_placement._type == Placement.Types.Banner) {
                    adUnit = new BannerController();
                } else if (_placement._type == Placement.Types.Interstitial) {
                    adUnit = new InterstitialController();
                } else if (_placement._type == Placement.Types.Rewarded) {
                    adUnit = new RewardedController();
                } else {
                    return;
                }
                adUnit.Init(_placement, _activity, PlacementController.this);

                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.add(R.id.adUnits, adUnit);
                ft.commit();
            }
        });

        return view;
    }

    public void OnAdUnitClose(AdUnitController controller) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.remove(controller);
        ft.commitAllowingStateLoss();
    }
}