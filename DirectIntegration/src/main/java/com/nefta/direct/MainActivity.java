package com.nefta.direct;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.Insight;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.direct.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final static String preferences = "preferences";
    private final static String trackingKey = "tracking";
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

        NeftaPlugin.EnableLogging(true);
        _plugin = NeftaPlugin.Init(this, "5643649824063488");
        SetTracking();
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String override = intent.getStringExtra("override");
            if (override != null && override.length() > 2) {
                _plugin.SetOverride(override);
            }
        }
        new DebugServer(this);

        _plugin.OnReady = this::OnReady;
        _plugin.OnBehaviourInsight = this::OnBehaviourInsight;

        String[] insightList = {"p_churn_14d", "p_churn_1d", "p_churn_30d", "nonE"};
        _plugin.GetBehaviourInsight(insightList);

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

    private void OnBehaviourInsight(HashMap<String, Insight> behaviourInsight) {
        for (Map.Entry<String, Insight> entry : behaviourInsight.entrySet()) {
            String name = entry.getKey();
            Insight insight = entry.getValue();
            Log.i("DI", "Behaviour insight "+ name + " status: "+ insight._status + " i:"+ insight._int + " f:"+ insight._float + " s:"+ insight._string);
        }
    }

    private void SetTracking() {
        SharedPreferences sharedPreferences = getSharedPreferences(preferences, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(trackingKey)) {
            boolean isTrackingAllowed = sharedPreferences.getBoolean(trackingKey, false);
            _plugin.SetTracking(isTrackingAllowed);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener trackingHandler = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean isAllowed = which == -1;
                    _plugin.SetTracking(isAllowed);

                    SharedPreferences sharedPreferences = getSharedPreferences(preferences, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(trackingKey, isAllowed);
                    editor.apply();
                }
            };
            builder.setTitle("Advertising id access")
                    .setMessage("Is tracking allowed")
                    .setPositiveButton("Yes", trackingHandler)
                    .setNegativeButton("No", trackingHandler);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}