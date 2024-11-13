package com.nefta.direct;

import com.nefta.sdk.NAd;
import com.nefta.sdk.NRewarded;
import com.nefta.sdk.NRewardedListener;

public class RewardedController extends AdUnitController implements NRewardedListener {

    public RewardedController() {

    }

    @Override
    protected NAd CreateInstance() {
        NRewarded rewarded = new NRewarded(_placement._id);
        rewarded._listener = this;
        return rewarded;
    }

    public void OnReward(NAd ad) {
        _status.setText("OnReward");
    }
}
