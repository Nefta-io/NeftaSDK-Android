package com.nefta.neftatestbed;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.neftatestbed.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Switch _autoLoadSwitch;
    private NeftaPlugin _plugin;
    private HashMap<Placement, PlacementController> _placementToControllers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        _plugin = NeftaPlugin.Init(getApplicationContext(),"5667525748588544");
        _plugin.OnReady = this::OnReady;
        _plugin.OnBid = this::OnBid;
        _plugin.OnPlacementStartLoad = this::OnPlacementStartLoad;
        _plugin.OnPlacementLoadFail = this::OnPlacementLoadFail;
        _plugin.OnPlacementLoad = this::OnPlacementLoad;
        _plugin.OnPlacementShow = this::OnPlacementShow;
        _plugin.OnPlacementClose = this::OnPlacementClose;
        _plugin.EnableAds(true);
        _plugin.PrepareRenderer(this);

        _placementToControllers = new HashMap<>();

        _autoLoadSwitch = findViewById(R.id.autoLoadSwitch);
        _autoLoadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                for (Map.Entry<Placement, PlacementController> entry : _placementToControllers.entrySet()) {
                    entry.getValue().SetAutoLoad(isChecked);
                }
            }
        });
    }

    protected void onResume() {
        super.onResume();

        _plugin.OnResume();
    }

    protected void onPause() {
        super.onPause();

        _plugin.OnPause();
    }

    public void onBackPressed() {
        for (Map.Entry<Placement, PlacementController> p : _placementToControllers.entrySet()) {
            Placement placement = p.getKey();
            if (placement._renderedBid != null) {
                _plugin.ClosePlacement(placement._id);
                return;
            }
        }
        super.onBackPressed();
    }

    private void OnReady(HashMap<String, Placement> placements) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Map.Entry<String, Placement> p : placements.entrySet()) {
            Placement placement = p.getValue();
            PlacementController placementController = new PlacementController(this, _plugin, placement, _autoLoadSwitch.isChecked());
            ft.add(R.id.placementContainer, placementController);
            _placementToControllers.put(placement, placementController);
        }
        ft.commit();
    }

    private void OnBid(Placement placement, BidResponse bidResponse) {
        _placementToControllers.get(placement).OnBid();
    }

    private void OnPlacementStartLoad(Placement placement) {
        _placementToControllers.get(placement).OnStartLoad();
    }

    private void OnPlacementLoadFail(Placement placement, String error) {
        _placementToControllers.get(placement).OnLoadFail();
    }

    private void OnPlacementLoad(Placement placement) {
        _placementToControllers.get(placement).OnLoad();
    }

    private void OnPlacementShow(Placement placement, int height) {
        _placementToControllers.get(placement).OnShow();
    }

    private void OnPlacementClose(Placement placement) {
        _placementToControllers.get(placement).OnClose();
    }

}