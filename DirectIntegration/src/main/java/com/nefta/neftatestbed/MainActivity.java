package com.nefta.neftatestbed;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.neftatestbed.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NeftaPlugin _plugin;
    private HashMap<Placement, PlacementController> _placementToControllers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String appId = "5070114386870272";
        _plugin = NeftaPlugin.Init(getApplicationContext(), appId);
        _plugin.OnReady = this::OnReady;
        _plugin.OnBid = this::OnBid;
        _plugin.OnLoadStart = this::OnStartLoad;
        _plugin.OnLoadFail = this::OnLoadFail;
        _plugin.OnLoad = this::OnLoad;
        _plugin.OnShow = this::OnShow;
        _plugin.OnClose = this::OnClose;
        _plugin.EnableAds(true);
        _plugin.PrepareRenderer(this);

        _placementToControllers = new HashMap<>();

        String appIdText;
        if (appId == null || appId.length() == 0) {
            appIdText = "Demo mode (appId not set)";
        } else {
            appIdText = "AppId: " + appId;
        }
        ((TextView) findViewById(R.id.appId)).setText(appIdText);
    }

    public void onBackPressed() {
        _plugin.Close();
    }

    private void OnReady(HashMap<String, Placement> placements) {
        _placementToControllers.clear();
        ((LinearLayout)findViewById(R.id.placementContainer)).removeAllViews();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Map.Entry<String, Placement> p : placements.entrySet()) {
            Placement placement = p.getValue();
            PlacementController placementController = new PlacementController(this, _plugin, placement);
            ft.add(R.id.placementContainer, placementController);
            _placementToControllers.put(placement, placementController);
        }
        ft.commit();
    }

    private void OnBid(Placement placement, BidResponse bidResponse) {
        _placementToControllers.get(placement).OnBid();
    }

    private void OnStartLoad(Placement placement) {
        _placementToControllers.get(placement).OnStartLoad();
    }

    private void OnLoadFail(Placement placement, String error) {
        _placementToControllers.get(placement).OnLoadFail();
    }

    private void OnLoad(Placement placement) {
        _placementToControllers.get(placement).OnLoad();
    }

    private void OnShow(Placement placement, int width, int height) {
        _placementToControllers.get(placement).OnShow();
    }

    private void OnClose(Placement placement) {
        _placementToControllers.get(placement).OnClose();
    }

}