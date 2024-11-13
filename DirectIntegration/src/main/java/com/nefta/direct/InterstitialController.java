package com.nefta.direct;

import com.nefta.sdk.NAd;
import com.nefta.sdk.NInterstitial;
import com.nefta.sdk.NInterstitialListener;

public class InterstitialController extends AdUnitController implements NInterstitialListener {
    public InterstitialController() {

    }

    @Override
    protected NAd CreateInstance() {
        NInterstitial interstitial = new NInterstitial(_placement._id);
        interstitial._listener = this;
        return interstitial;
    }
}
