package com.nefta.neftatestbed;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.neftatestbed.databinding.ActivityMainBinding;
import com.nefta.sdk.Placement;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Context _context;
    private NeftaPlugin _plugin;
    private TextView _nuidText;
    private HashMap<Placement, PlacementController> _placementToControllers;

    private String _logDirectory;
    private ArrayList<String> _last3LogNames;
    private static DateTimeFormatter _logFormatter;
    private static FileOutputStream _logStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        _context = getApplicationContext();

        _logDirectory = _context.getApplicationInfo().dataDir +"/Logs";
        File eventsDirectory = new File(_logDirectory);
        if (!eventsDirectory.exists()) {
            eventsDirectory.mkdir();
        }

        try {
            _last3LogNames = new ArrayList<>();

            SharedPreferences preferences = _context.getSharedPreferences("DirectIntegration", Context.MODE_PRIVATE);
            String logs = preferences.getString("logs", null);
            if (logs != null && logs.length() > 0) {
                String[] logArray = logs.split(",");
                int i = logArray.length - 2;
                if (i < 0) {
                    i = 0;
                }
                for ( ; i < logArray.length; i++) {
                    _last3LogNames.add(logArray[i]);
                }
            }

            String logName = "log_" + System.currentTimeMillis() + ".txt";
            _logStream = new FileOutputStream(_logDirectory + "/" + logName);
            _logFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            _last3LogNames.add(logName);
            NeftaPlugin.OnLog = MainActivity::Log;

            logs = "";
            for (int i = 0; i < _last3LogNames.size(); i++) {
                if (i > 0) {
                    logs += ",";
                }
                logs += _last3LogNames.get(i);
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("logs", logs);
            editor.apply();
        } catch (Exception e) {

        }
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        String appId = "5643649824063488";
        _plugin = NeftaPlugin.Init(_context, appId);
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

                Toast.makeText(_context, R.string.app_id_toast, Toast.LENGTH_SHORT).show();
            }
        });


        TextView titleTextView = findViewById(R.id.mainTitle);
        titleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendLogs();
            }
        });

        _nuidText = findViewById(R.id.nuid);
        _nuidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nuid", _nuidText.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(_context, R.string.nuid_toast, Toast.LENGTH_SHORT).show();

                NeftaPlugin.Events.AddReceiveEvent(NeftaEvents.ResourceCategory.CoreItem, NeftaEvents.ReceiveMethod.Other);
            }
        });

        NeftaPlugin.Events.AddProgressionEvent(NeftaEvents.ProgressionStatus.Complete, NeftaEvents.ProgressionType.Task, NeftaEvents.ProgressionSource.OptionalContent);
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
            Log("Error parsing toolbox user: " + e.getMessage());
        }

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

    private void OnLoad(Placement placement) {
        _placementToControllers.get(placement).OnLoad();
    }

    private void OnShow(Placement placement, int width, int height) {
        _placementToControllers.get(placement).OnShow();
    }

    private void OnClose(Placement placement) {
        _placementToControllers.get(placement).OnClose();
    }

    private static void Log(String log) {
        try {
            _logStream.write((_logFormatter.format(LocalDateTime.now()) + " " + log + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {

        }
    }

    private void SendLogs() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Logs");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Logs:");

        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (String logName : _last3LogNames) {
            File file = new File( _logDirectory + "/" + logName);
            Uri contentUri = FileProvider.getUriForFile(_context, "com.nefta.neftatestbed.provider", file);
            uris.add(contentUri);
        }

        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Send..."));
    }

    private static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            Log("EXC t:" + thread.getName() + ", " + throwable.getMessage());

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
}