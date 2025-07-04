package com.nefta.direct;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nefta.sdk.BidResponse;
import com.nefta.sdk.Insight;
import com.nefta.sdk.NAd;
import com.nefta.sdk.NError;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.sdk.Placement;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AdUnitController extends Fragment {
    private boolean _isActive;
    private boolean _isCloseCallbackPending;

    protected Placement _placement;
    protected IAdUnitCallback _callbacks;
    protected Button _showButton;
    protected TextView _creativeId;
    protected TextView _status;

    protected Activity _activity;
    protected NAd _ad;

    public AdUnitController() { }

    public void Init(Placement placement, Activity activity, IAdUnitCallback callbacks) {
        _placement = placement;
        _activity = activity;
        _callbacks = callbacks;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ad_unit_controller, container, false);

        _ad = CreateInstance();
        ((TextView) view.findViewById(R.id.adUnitId)).setText(String.format("%d", _ad.hashCode()));
        _creativeId = (TextView) view.findViewById(R.id.creativeId);

        _showButton = (Button)view.findViewById(R.id.showButton);

        _status = (TextView) view.findViewById(R.id.status);

        Button bidButton = (Button) view.findViewById(R.id.bidButton);
        bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnBidClick();
            }
        });
        view.findViewById(R.id.loadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnLoadClick();
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnShowClick();
            }
        });
        view.findViewById(R.id.destroyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnDestroyClick();
            }
        });

        _creativeId.setText("");
        _status.setText("");

        return view;
    }

    protected NAd CreateInstance() {
        return null;
    }

    void OnBidClick() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = "example event";
        long randomValue = random.nextLong(0, 101);
        if (_placement._type == Placement.Types.Banner) {
            NeftaPlugin._instance.GetBehaviourInsight(new String[] { "calculated_user_floor_price_banner"}, (HashMap<String, Insight> behaviourInsight) -> {
                double bidFloor = 0;
                Insight insight = behaviourInsight.get("calculated_user_floor_price_banner");
                if (insight != null) {
                    bidFloor = insight._float;
                }
                Log.i("DI", "Banner insight: "+ bidFloor);
            });

            NeftaEvents.ProgressionStatus progressionStatus = NeftaEvents.ProgressionStatus.FromInt(random.nextInt(0, 3));
            NeftaEvents.ProgressionType progressionType = NeftaEvents.ProgressionType.FromInt(random.nextInt(0, 7));
            NeftaEvents.ProgressionSource progressionSource = NeftaEvents.ProgressionSource.FromInt(random.nextInt(0, 7));
            NeftaPlugin.Events.AddProgressionEvent(progressionStatus, progressionType, progressionSource, name, randomValue);
        } else if (_placement._type == Placement.Types.Interstitial) {
            NeftaPlugin._instance.GetBehaviourInsight(new String[] { "calculated_user_floor_price_interstitial" }, (HashMap<String, Insight> behaviourInsight) -> {
                double bidFloor = 0;
                Insight insight = behaviourInsight.get("calculated_user_floor_price_interstitial");
                if (insight != null) {
                    bidFloor = insight._float;
                }
                Log.i("DI", "Interstitial insight: "+ bidFloor);
            });

            NeftaEvents.ResourceCategory resourceCategory = NeftaEvents.ResourceCategory.FromInt(random.nextInt(0, 9));
            NeftaEvents.ReceiveMethod receiveMethod = NeftaEvents.ReceiveMethod.FromInt(random.nextInt(0, 8));
            NeftaPlugin.Events.AddReceiveEvent(resourceCategory, receiveMethod, name, randomValue);
        } else {
            NeftaPlugin._instance.GetBehaviourInsight(new String[] { "calculated_user_floor_price_rewarded" }, (HashMap<String, Insight> behaviourInsight) -> {
                double bidFloor = 0;
                Insight insight = behaviourInsight.get("calculated_user_floor_price_rewarded");
                if (insight != null) {
                    bidFloor = insight._float;
                }
                Log.i("DI", "Rewarded insight: "+ bidFloor);
            });

            NeftaEvents.ResourceCategory resourceCategory = NeftaEvents.ResourceCategory.FromInt(random.nextInt(0, 9));
            NeftaEvents.SpendMethod spendMethod = NeftaEvents.SpendMethod.FromInt(random.nextInt(0, 8));
            NeftaPlugin.Events.AddSpendEvent(resourceCategory, spendMethod, name, randomValue);
        }

        _ad.Bid();
    }

    void OnLoadClick() {
        _ad.Load();
    }

    void OnShowClick() {
        _ad.Show(_activity);
    }

    void OnDestroyClick() {
        _ad.Close();
    }

    public void OnBid(NAd ad, BidResponse bidResponse, NError error) {
        if (bidResponse == null) {
            SetText("OnBid failed: " + error._message);
        } else {
            _creativeId.setText(bidResponse._creativeId);
            SetText("OnBid: " + bidResponse._creativeId + ": " + bidResponse._price);
        }
    }

    public void OnLoadStart(NAd ad) {

    }

    public void OnLoadFail(NAd ad, NError error) {
        _creativeId.setText("");
        SetText("OnLoadFail: " + error._message);
    }

    public void OnLoad(NAd ad, int width, int height) {
        SetText("OnLoad success w:"+ width+ " h:"+ height);

        NeftaPlugin.OnExternalMediationRequest("internal-test", ad._type._code, "recomA"+ad._bid._id, 0.2, 0.3, "seleA", 0.2, "prec", 1, null, null);
    }

    public void OnShowFail(NAd ad, NError error) {
        _creativeId.setText("");
        SetText("OnShowFail " + error._message);
    }

    public void OnShow(NAd ad) {
        SetText("OnShow");

        NeftaPlugin.OnExternalMediationImpression("internal-test", new JSONObject(), ad._type._code, 0.2, "pre");
    }

    public void OnClick(NAd ad) {
        SetText("OnClick");
    }

    public void OnClose(NAd ad) {
        _ad = null;

        if (_isActive) {
            _callbacks.OnAdUnitClose(this);
        } else {
            _isCloseCallbackPending = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        _isActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        _isActive = true;
        if (_isCloseCallbackPending) {
            _isCloseCallbackPending = false;
            _callbacks.OnAdUnitClose(this);
        }
    }

    protected void SetText(String text) {
        Log.i("DI", text);
        _status.setText(text);
    }
}