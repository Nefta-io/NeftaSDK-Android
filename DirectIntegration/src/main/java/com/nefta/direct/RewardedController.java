package com.nefta.direct;

import com.nefta.sdk.Insights;
import com.nefta.sdk.NAd;
import com.nefta.sdk.NRewarded;
import com.nefta.sdk.NRewardedListener;
import com.nefta.sdk.NeftaPlugin;

public class RewardedController extends AdUnitController implements NRewardedListener {

    public RewardedController() {

    }

    @Override
    protected NAd CreateInstance() {
        NeftaPlugin._instance.GetInsights(Insights.REWARDED, null, (insights) -> {
            _insights = insights;
        }, 5);

        NRewarded rewarded = new NRewarded(_placement._id);
        rewarded._listener = this;
        return rewarded;
    }

    public void OnReward(NAd ad) {
        _status.setText("OnReward");
    }
}
