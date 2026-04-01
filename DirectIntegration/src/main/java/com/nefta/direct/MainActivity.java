package com.nefta.direct;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.nefta.sdk.InitConfiguration;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.debug.DebugServer;
import com.nefta.direct.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private final static String _tag = "NeftaPluginDI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        DebugServer.Init(this, getIntent());

        NeftaPlugin.EnableLogging(true);
        NeftaPlugin.SetExtraParameter(NeftaPlugin.ExtParam_TestGroup, "split-direct");
        NeftaPlugin.NativeInit(this, "5643649824063488", null, (InitConfiguration initConfig) -> {
            Log.i(_tag, "OnReady: "+ initConfig._skipOptimization + " for: " + initConfig._nuid);
        }, "direct", "/");
    }
}