package com.nefta.direct;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.direct.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NeftaPlugin _plugin;
    private HashMap<Placement, PlacementController> _placementToControllers;

    public static FrameLayout _bannerPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        _bannerPlaceholder = (FrameLayout) view.findViewById(R.id.bannerView);

        _plugin = NeftaPlugin.Init(this, "5702146766929920");

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String override = intent.getStringExtra("override");
            if (override != null && override.length() > 2) {
                _plugin.SetOverride(override);
            }
        }
        new DebugServer(this);

        _plugin.EnableAds(true);
        _plugin.OnReady = this::OnReady;

        _placementToControllers = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        _plugin.OnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        _plugin.OnPause();
    }

    private void OnReady(HashMap<String, Placement> placements) {
        _placementToControllers.clear();
        ((LinearLayout)findViewById(R.id.placementContainer)).removeAllViews();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Map.Entry<String, Placement> p : placements.entrySet()) {
            Placement placement = p.getValue();
            PlacementController placementController = new PlacementController();
            placementController.Init(placement);
            ft.add(R.id.placementContainer, placementController);
            _placementToControllers.put(placement, placementController);
        }
        ft.commit();
    }
}