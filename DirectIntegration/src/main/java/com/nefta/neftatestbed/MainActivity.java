package com.nefta.neftatestbed;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.neftatestbed.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NeftaPlugin _plugin;
    private TextView _nuidText;
    private HashMap<Placement, PlacementController> _placementToControllers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Context context = getApplicationContext();

        String appId = "5630785994358784";
        _plugin = NeftaPlugin.Init(context, appId);
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
        TextView appIdTextView = findViewById(R.id.appId);
        appIdTextView.setText(appIdText);
        appIdTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("appId", appId);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(context, R.string.app_id_toast, Toast.LENGTH_SHORT).show();
            }
        });

        _nuidText = findViewById(R.id.nuid);
        _nuidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nuid", _nuidText.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(context, R.string.nuid_toast, Toast.LENGTH_SHORT).show();
            }
        });
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
        String userString = _plugin.GetToolboxUser();
        try {
            JSONObject user = new JSONObject(userString);
            _nuidText.setText(user.getString("user_id"));
        } catch (JSONException e) {
            Log.i("DemoApp", "Error parsing toolbox user: " + e.getMessage());
        }

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