package com.nefta.testbed;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.nefta.sdk.NeftaPlugin;
import com.nefta.sdk.Placement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class DebugServer {
    private final String TAG = "DebugServer";
    private final int _port = 12012;
    private Socket _socket;
    private PrintWriter _out;
    private BufferedInputStream _in;
    private String _serial;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public DebugServer(String ip, String serial) {
        _serial = serial;

        backgroundThread = new HandlerThread("NetworkThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        new Thread(() -> {
            try {
                _socket = new Socket(ip, _port);

                _out = new PrintWriter(new OutputStreamWriter(_socket.getOutputStream()), true);
                _in = new BufferedInputStream(_socket.getInputStream());

                send("log", "Debug server connected");
                Log.i(TAG, "DS:Connection established");

                byte[] buffer = new byte[2048];
                int length;
                while ((length = _in.read(buffer)) != -1) {
                    String message = new String (buffer, 0, length);

                    String control = message;
                    int controlEnd = message.indexOf(" ");
                    if (controlEnd != -1) {
                        control = message.substring(0, controlEnd);
                    }
                    if ("get_nuid".equals(control)) {
                        NeftaPlugin._instance.GetNuid(true);
                    } else if ("ad_units".equals(control)) {
                        try {
                            JSONObject json = new JSONObject();
                            JSONArray adUnits = new JSONArray();
                            json.put("ad_units", adUnits);

                            for (Map.Entry<String, Placement> p : NeftaPlugin._instance._publisher._placements.entrySet()) {
                                JSONObject placementJson = new JSONObject();
                                placementJson.put("id", p.getKey());
                                placementJson.put("type", Placement.Types.ToString(p.getValue()._type));
                                adUnits.put(placementJson);
                            }
                            send("return ad_units", json.toString());
                        } catch (Exception ignored) {

                        }
                    } else if ("getOverride".equals(control)) {
                        NeftaPlugin._instance.GetNuid(false);
                    } else if ("setOverride".equals(control)) {
                        String pId = message.substring(controlEnd + 1);
                        NeftaPlugin._instance.Load(pId);
                        String root = control;
                        NeftaPlugin.Restart(root == null || root.equals("null") ? null : root);
                    } else if ("partial_bid".equals(control)) {
                        String pId = message.substring(controlEnd + 1);
                        JSONObject bidRequest = NeftaPlugin._instance.GetPartialBidRequest(pId);
                        send("return partial_bid", bidRequest.toString());
                    } else if ("bid".equals(control)) {
                        String pId = message.substring(controlEnd + 1);
                        NeftaPlugin._instance.Bid(pId);
                    } else if ("custom_load".equals(control)) {
                        int pIdEnd = message.indexOf(" ", controlEnd + 1);
                        String pId = message.substring(controlEnd + 1, pIdEnd);
                        String bidResponse = message.substring(pIdEnd + 1);
                        NeftaPlugin._instance.LoadWithBidResponse(pId, bidResponse);
                        send("return", "custom_load");
                    } else if ("load".equals(control)) {
                        String pId = message.substring(controlEnd + 1);
                        NeftaPlugin._instance.Load(pId);
                        send("return", "load");
                    } else if ("show".equals(control)) {
                        String pId = message.substring(controlEnd + 1);
                        NeftaPlugin._instance.Show(pId);
                        send("return", "show");
                    } else {
                        Log.i(TAG, "Unrecognized command: " + control);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stopServer() {
        try {
            if (_in != null) {
                _in.close();
            }
            if (_out != null) {
                _out.close();
            }
            if (_socket != null) {
                _socket.close();
            }
            System.out.println("Server stopped");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String type, String message) {
        if (_out == null) {
            return;
        }

        final String data = _serial + " " + type + " " + message;
        if (Looper.getMainLooper().isCurrentThread()) {
            backgroundHandler.post(() -> send(data));
        } else {
            send(data);
        }
     }
    private void send(String data) {
        _out.print(data);
        _out.flush();
    }
}
