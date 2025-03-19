package com.nefta.direct;

import com.nefta.sdk.NAd;
import com.nefta.sdk.NBanner;
import com.nefta.sdk.NBannerListener;

public class BannerController extends AdUnitController implements NBannerListener {
    private NBanner _banner;
    private final int FLOW_NONE = 0;
    private final int FLOW_MANUAL = 1;
    private final int FLOW_AUTO_LOAD = 2;
    private final int FLOW_AUTO_SHOWN = 3;
    private final int FLOW_AUTO_HIDDEN = 4;
    private int _flowState;

    public BannerController() {
    }

    @Override
    protected NAd CreateInstance() {
        _banner = new NBanner(_placement._id, MainActivity._bannerPlaceholder);
        _banner._listener = this;
        return _banner;
    }

    @Override
    void OnBidClick() {
        if (_flowState == FLOW_NONE) {
            _flowState = FLOW_MANUAL;
        }
        super.OnBidClick();
    }

    @Override
    void OnLoadClick() {
        if (_flowState == FLOW_NONE) {
            _flowState = FLOW_MANUAL;
        }
        super.OnLoadClick();
    }

    @Override
    void OnShowClick() {
        switch (_flowState) {
            case FLOW_NONE:
                _banner.SetAutoRefresh(true);
                _showButton.setText("Loading");
                _flowState = FLOW_AUTO_LOAD;
                _banner.Show(_activity);
                break;
            case FLOW_MANUAL:
                _banner.Show(_activity);
                break;
            case FLOW_AUTO_LOAD:

                break;
            case FLOW_AUTO_SHOWN:
                _flowState = FLOW_AUTO_HIDDEN;
                _showButton.setText("Show");
                _banner.Hide();
                break;
            case FLOW_AUTO_HIDDEN:
                _flowState = FLOW_AUTO_SHOWN;
                _showButton.setText("Hide");
                _banner.Show(_activity);
                break;
        }
    }

    @Override
    public void OnShow(NAd ad) {
        if (_flowState == FLOW_AUTO_LOAD) {
            _showButton.setText("Hide");
            _flowState = FLOW_AUTO_SHOWN;
        }
        super.OnShow(ad);
    }

    @Override
    public void OnClose(NAd ad) {
        _banner = null;
        super.OnClose(ad);
    }
}
