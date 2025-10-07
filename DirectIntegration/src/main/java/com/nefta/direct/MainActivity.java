package com.nefta.direct;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.InitConfiguration;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.direct.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final static String _tag = "NeftaPluginDI";

    private final static String preferences = "preferences";
    private final static String trackingKey = "tracking";
    private NeftaPlugin _plugin;
    private DebugServer _debugServer;
    private static boolean _isTablet;
    private static FrameLayout _bannerPlaceholder;
    private static RelativeLayout _leaderPlaceholder;
    private HashMap<Placement, PlacementController> _placementToControllers;

    public static ViewGroup GetBannerPlaceholder() {
        return _isTablet ? _leaderPlaceholder : _bannerPlaceholder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        AdjustForTablet(view);

        _debugServer = new DebugServer(this, getIntent());

        NeftaPlugin.EnableLogging(true);
        NeftaPlugin.SetExtraParameter(NeftaPlugin.ExtParam_TestGroup, "split-direct");
        _plugin = NeftaPlugin.Init(this, "5643649824063488");
        _plugin.SetContentRating(NeftaPlugin.ContentRating_Teen);
        SetTracking();

        _plugin.OnReady = this::OnReady;

        _placementToControllers = new HashMap<>();
    }

    @Override
    public void onDestroy() {
        _bannerPlaceholder = null;
        _leaderPlaceholder = null;
        _debugServer.Destroy();

        super.onDestroy();
    }

    private void OnReady(InitConfiguration initConfig) {
        _placementToControllers.clear();
        ((LinearLayout)findViewById(R.id.placementContainer)).removeAllViews();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Map.Entry<String, Placement> p : initConfig._placements.entrySet()) {
            Placement placement = p.getValue();
            PlacementController placementController = new PlacementController();
            placementController.Init(placement, this);
            ft.add(R.id.placementContainer, placementController);
            _placementToControllers.put(placement, placementController);
        }
        ft.commit();
    }

    private void OnInsights(Insights insights) {
        Log("OnInsights");
        if (insights._churn != null) {
            Log("Insight churn d1: " + insights._churn._d1_probability);
        }
        if (insights._interstitial != null) {
            Log("Insight interstitial floor: " + insights._interstitial._floorPrice);
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

    private void AdjustForTablet(View view) {
        _bannerPlaceholder = (FrameLayout) view.findViewById(R.id.bannerView);
        _leaderPlaceholder = (RelativeLayout) view.findViewById(R.id.leaderView);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(displayMetrics);
        Point point = new Point();
        display.getRealSize(point);
        double diagonalInInches = Math.sqrt(Math.pow((double)point.x / displayMetrics.xdpi, 2) + Math.pow((double)point.y / displayMetrics.ydpi, 2));
        _isTablet = diagonalInInches >= 6.5 && (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        _bannerPlaceholder.setVisibility(_isTablet ? View.GONE : View.VISIBLE);
        _leaderPlaceholder.setVisibility(_isTablet ? View.VISIBLE : View.GONE);
    }

    private void Log(String log) {
        Log.i(_tag, log);
    }
}