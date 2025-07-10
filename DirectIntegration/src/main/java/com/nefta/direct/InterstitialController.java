package com.nefta.direct;

import com.nefta.sdk.Insights;
import com.nefta.sdk.NAd;
import com.nefta.sdk.NInterstitial;
import com.nefta.sdk.NInterstitialListener;
import com.nefta.sdk.NeftaPlugin;

public class InterstitialController extends AdUnitController implements NInterstitialListener {
    public InterstitialController() {

    }

    @Override
    protected NAd CreateInstance() {
        NeftaPlugin._instance.GetInsights(Insights.INTERSTITIAL, (insights) -> {
            _insights = insights;
        }, 5);

        NInterstitial interstitial = new NInterstitial(_placement._id);
        interstitial._listener = this;

        //interstitial.SetFloorPrice(0.5f);
        //interstitial.SetCustomParameter("applovin-max", "{\"bidfloor\":0.5}");

        return interstitial;
    }
}
