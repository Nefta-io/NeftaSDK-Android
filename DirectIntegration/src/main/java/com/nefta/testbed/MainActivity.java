package com.nefta.testbed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.testbed.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Context _context;
    private NeftaPlugin _plugin;
    private HashMap<Placement, PlacementController> _placementToControllers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        _context = getApplicationContext();

        _plugin = NeftaPlugin.Init(_context, "5643649824063488");

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String root = intent.getStringExtra("override");
            String dmRoot = intent.getStringExtra("dmIp");
            String serial = intent.getStringExtra("serial");
            _plugin.SetOverride(root);

            DebugServer debugServer = new DebugServer(dmRoot, serial);
            NeftaPlugin.OnLog = (String log) -> {
                debugServer.send("log", log);
            };
        }

        _plugin.OnReady = this::OnReady;
        _plugin.OnBid = this::OnBid;
        _plugin.OnLoadStart = this::OnStartLoad;
        _plugin.OnLoadFail = this::OnLoadFail;
        _plugin.OnLoad = this::OnLoad;
        _plugin.OnShow = this::OnShow;
        _plugin.OnClose = this::OnClose;
        _plugin.EnableAds(true);
        _plugin.PrepareRenderer(this);

        _plugin.SetFloorPrice("1",0.5f);
        _plugin.SetCustomParameter("1","applovin-max", "{\"bidfloor\":0.5}");

        _placementToControllers = new HashMap<>();

        NeftaPlugin.Events.AddReceiveEvent(NeftaEvents.ResourceCategory.CoreItem, NeftaEvents.ReceiveMethod.Other);
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

    public void onBackPressed() {
        _plugin.Close();
    }

    private void OnReady(HashMap<String, Placement> placements) {
        _placementToControllers.clear();
        ((LinearLayout)findViewById(R.id.placementContainer)).removeAllViews();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Map.Entry<String, Placement> p : placements.entrySet()) {
            Placement placement = p.getValue();
            PlacementController placementController = new PlacementController();
            placementController.Init(_plugin, placement);
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

    private void OnLoad(Placement placement, int width, int height) {
        _placementToControllers.get(placement).OnLoad();
    }

    private void OnShow(Placement placement) {
        _placementToControllers.get(placement).OnShow();
    }

    private void OnClose(Placement placement) {
        _placementToControllers.get(placement).OnClose();
    }
}