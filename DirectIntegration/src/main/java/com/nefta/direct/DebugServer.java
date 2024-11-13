package com.nefta.direct;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.nefta.sdk.NAd;
import com.nefta.sdk.NBanner;
import com.nefta.sdk.NInterstitial;
import com.nefta.sdk.NRewarded;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.sdk.Placement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DebugServer {
    private final String TAG = "DebugServer";
    private final int _port = 12012;

    private String _ip;
    private Socket _socket;
    private PrintWriter _out;
    private BufferedInputStream _in;
    private String _serial;
    private Handler mainHandler;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public DebugServer(String ip, String serial) {
        _ip = ip;
        _serial = serial;

        mainHandler = new Handler(Looper.getMainLooper());

        backgroundThread = new HandlerThread("NetworkThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        startServer();
    }

    public void restartServer() {
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

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startServer();
            }
        }, 5000);
    }

    public void send(String message) {
        if (_out == null) {
            return;
        }

        final String data = _serial + " " + message;
        if (Looper.getMainLooper().isCurrentThread()) {
            backgroundHandler.post(() -> sendInternal(data));
        } else {
            sendInternal(data);
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                _socket = new Socket(_ip, _port);

                _out = new PrintWriter(new OutputStreamWriter(_socket.getOutputStream()), true);
                _in = new BufferedInputStream(_socket.getInputStream());

                send("log Debug server connected");
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
                    String pId;
                    String aId;
                    switch (control) {
                        case "get_nuid":
                            NeftaPlugin._instance.GetNuid(true);
                            break;
                        case "set_time_offset":
                            String offsetString = message.substring(controlEnd + 1);
                            NeftaPlugin.SetDebugTime(Long.parseLong(offsetString));
                            send("return set_time_offset");
                            break;
                        case "ad_units":
                            try {
                                JSONObject json = new JSONObject();
                                JSONArray adUnits = new JSONArray();
                                json.put("ad_units", adUnits);

                                for (Map.Entry<String, Placement> p : NeftaPlugin._instance._placements.entrySet()) {
                                    Placement placement = p.getValue();
                                    JSONObject placementJson = new JSONObject();
                                    placementJson.put("id", p.getKey());
                                    placementJson.put("type", Placement.Types.ToString(placement._type));
                                    adUnits.put(placementJson);

                                    JSONArray ads = new JSONArray();
                                    for (NAd ad : NeftaPlugin._instance._ads) {
                                        if (ad._placement == placement) {
                                            JSONObject adJson = new JSONObject();
                                            adJson.put("id", ad);
                                            adJson.put("state", ad._state);
                                            String crid = "";
                                            if (ad._bid != null) {
                                                crid = ad._bid._creativeId;
                                            }
                                            adJson.put("crid", crid);
                                            ads.put(adJson);
                                        }
                                    }
                                    placementJson.put("ads", ads);
                                }
                                send("return ad_units " + json);
                            } catch (Exception ignored) {

                            }
                            break;
                        case "getOverride":
                            NeftaPlugin._instance.GetNuid(false);
                            break;
                        case "setOverride":
                            String root = message.substring(controlEnd + 1);
                            NeftaPlugin.Restart(root.isEmpty() || "null".equals(root) ? null : root);
                            break;
                        case "create":
                            pId = message.substring(controlEnd + 1);
                            String adId = null;
                            for (Map.Entry<String, Placement> p : NeftaPlugin._instance._placements.entrySet()) {
                                if (p.getKey().equals(pId)) {
                                    Placement placement = p.getValue();
                                    if (placement._type == Placement.Types.Banner) {
                                        NBanner banner = new NBanner(pId, NBanner.Position.TOP);
                                        adId = banner.toString();
                                    } else if (placement._type == Placement.Types.Interstitial) {
                                        NInterstitial interstitial = new NInterstitial(pId);
                                        adId = interstitial.toString();
                                    } else if (placement._type == Placement.Types.Rewarded) {
                                        NRewarded rewarded = new NRewarded(pId);
                                        adId = rewarded.toString();
                                    }
                                }
                            }
                            send("return create "+ adId);
                            break;
                        case "partial_bid":
                            aId = message.substring(controlEnd + 1);
                            String bidResponse = null;
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    bidResponse = ad.GetPartialBidRequest().toString();
                                    break;
                                }
                            }
                            send("return partial_bid "+ aId +" "+ bidResponse);
                            break;
                        case "bid":
                            aId = message.substring(controlEnd + 1);
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    ad.Bid();
                                }
                            }
                            send("return bid "+ aId);
                            break;
                        case "custom_load":
                            int pIdEnd = message.indexOf(" ", controlEnd + 1);
                            aId = message.substring(controlEnd + 1, pIdEnd);
                            String bid = message.substring(pIdEnd + 1);
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    ad.LoadWithBidResponse(bid);
                                }
                            }
                            send("return custom_load "+ aId);
                            break;
                        case "load":
                            aId = message.substring(controlEnd + 1);
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    ad.Load();
                                }
                            }
                            send("return load "+ aId);
                            break;
                        case "show":
                            aId = message.substring(controlEnd + 1);
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ad.Show();
                                        }
                                    });
                                    break;
                                }
                            }
                            send("return show " + aId);
                            break;
                        case "close":
                            aId = message.substring(controlEnd + 1);
                            for (NAd ad : NeftaPlugin._instance._ads) {
                                if (ad.toString().equals(aId)) {
                                    ad.Close();
                                }
                            }
                            send("return show " + aId);
                            break;
                        case "add_event":
                            String[] parameters = message.substring(controlEnd + 1).split(" ");
                            String name = null;
                            long value = 0;
                            String customPayload = null;

                            if ("progression".equals(parameters[0])) {
                                NeftaEvents.ProgressionStatus status = ToProgressionStatus(parameters[1]);
                                NeftaEvents.ProgressionType type = ToProgressionType(parameters[2]);
                                NeftaEvents.ProgressionSource source = ToProgressionSource(parameters[3]);
                                if (parameters.length > 4) {
                                    name = parameters[4];
                                }
                                if (parameters.length > 5) {
                                    value = Long.parseLong(parameters[5]);
                                }
                                if (parameters.length > 6) {
                                    customPayload = parameters[6];
                                }
                                NeftaPlugin.Events.AddProgressionEvent(status, type, source, name, value, customPayload);
                            } else if ("receive".equals(parameters[0])) {
                                NeftaEvents.ResourceCategory category = ToResourceCategory(parameters[1]);
                                NeftaEvents.ReceiveMethod method = ToReceiveMethod(parameters[2]);
                                if (parameters.length > 3) {
                                    name = parameters[3];
                                }
                                if (parameters.length > 4) {
                                    value = Long.parseLong(parameters[4]);
                                }
                                if (parameters.length > 5) {
                                    customPayload = parameters[5];
                                }
                                NeftaPlugin.Events.AddReceiveEvent(category, method, name, value, customPayload);
                            } else if ("spend".equals(parameters[0])) {
                                NeftaEvents.ResourceCategory category = ToResourceCategory(parameters[1]);
                                NeftaEvents.SpendMethod method = ToSpendMethod(parameters[2]);
                                if (parameters.length > 3) {
                                    name = parameters[3];
                                }
                                if (parameters.length > 4) {
                                    value = Long.parseLong(parameters[4]);
                                }
                                if (parameters.length > 5) {
                                    customPayload = parameters[5];
                                }
                                NeftaPlugin.Events.AddSpendEvent(category, method, name, value, customPayload);
                            }
                            send("return add_event");
                            break;
                        case "set_override":
                            String override = message.substring(controlEnd + 1);
                            NeftaPlugin._instance.SetOverride(override.isEmpty() || override.equals("null") ? null : override);
                            send("return set_override");
                            break;
                        default:
                            Log.i(TAG, "Unrecognized command: " + control);
                            break;
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "Disconnect or error: " + e.getMessage());
            } finally {
                restartServer();
            }
        }).start();
    }

    private void sendInternal(String data) {
        _out.print(data);
        _out.flush();
    }

    private NeftaEvents.ProgressionStatus ToProgressionStatus(String name) {
        switch (name) {
            case "start":
                return NeftaEvents.ProgressionStatus.Start;
            case "complete":
                return NeftaEvents.ProgressionStatus.Complete;
            default:
                return NeftaEvents.ProgressionStatus.Fail;
        }
    }

    private NeftaEvents.ProgressionType ToProgressionType(String name) {
        switch (name) {
            case "achievement":
                return NeftaEvents.ProgressionType.Achievement;
            case "gameplay_unit":
                return NeftaEvents.ProgressionType.GameplayUnit;
            case "item_level":
                return NeftaEvents.ProgressionType.ItemLevel;
            case "unlock":
                return NeftaEvents.ProgressionType.Unlock;
            case "player_level":
                return NeftaEvents.ProgressionType.PlayerLevel;
            case "task":
                return NeftaEvents.ProgressionType.Task;
            default:
                return NeftaEvents.ProgressionType.Other;
        }
    }

    private NeftaEvents.ProgressionSource ToProgressionSource(String name) {
        for (NeftaEvents.ProgressionSource c : NeftaEvents.ProgressionSource.values()) {
            if (name.equals(c._name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Non existing ProgressionSource: " + name);
    }

    private NeftaEvents.ResourceCategory ToResourceCategory(String name) {
        for (NeftaEvents.ResourceCategory c : NeftaEvents.ResourceCategory.values()) {
            if (c._name.equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Non existing ResourceCategory: " + name);
    }

    private NeftaEvents.ReceiveMethod ToReceiveMethod(String name) {
        for (NeftaEvents.ReceiveMethod m : NeftaEvents.ReceiveMethod.values()) {
            if (name.equals(m._name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Non existing ReceiveMethod: " + name);
    }

    private NeftaEvents.SpendMethod ToSpendMethod(String name) {
        for (NeftaEvents.SpendMethod m : NeftaEvents.SpendMethod.values()) {
            if (name.equals(m._name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Non existing SpendMethod: " + name);
    }
}
