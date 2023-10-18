package com.nefta.neftatestbed;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nefta.sdk.NeftaPlugin;
import com.nefta.sdk.Placement;

public class PlacementController extends Fragment {

    private Activity _activity;
    private NeftaPlugin _plugin;
    private Placement _placement;

    private TextView _availableBid;
    private Button _bidButton;
    private Button _loadButton;
    private TextView _bufferedBid;
    private Button _showButton;
    private TextView _renderedBid;
    private Button _closeButton;
    private boolean _isAutoLoad;

    public PlacementController(Activity activity, NeftaPlugin plugin, Placement placement, boolean isAutoLoad) {
        _activity = activity;
        _plugin = plugin;
        _placement = placement;
        _isAutoLoad = isAutoLoad;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placement_controller, container, false);
        ((TextView)view.findViewById(R.id.id)).setText(_placement._id);
        ((TextView)view.findViewById(R.id.type)).setText(_placement._type.toString());

        _availableBid = (TextView) view.findViewById(R.id.availableBid);
        _bidButton = (Button)view.findViewById(R.id.bidButton);
        _loadButton = (Button)view.findViewById(R.id.loadButton);
        _bufferedBid = (TextView) view.findViewById(R.id.bufferedBid);
        _showButton = (Button)view.findViewById(R.id.showButton);
        _renderedBid = (TextView) view.findViewById(R.id.renderedBid);
        _closeButton = (Button)view.findViewById(R.id.closeButton);

        _bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _plugin.Bid(_placement._id, _isAutoLoad);
            }
        });
        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _plugin.Load(_placement._id);
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _plugin.Show(_placement._id);
            }
        });
        _closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _plugin.Close(_placement._id);
            }
        });

        SyncUi();

        return view;
    }

    public void SetAutoLoad(boolean isAutoLoad) {
        _isAutoLoad = isAutoLoad;
    }

    public void OnBid() {
        SyncUi();
    }

    public void OnStartLoad() {
        SyncUi();
    }

    public void OnLoadFail() {
        SyncUi();
    }

    public void OnLoad() {
        SyncUi();
    }

    public void OnShow() {
        SyncUi();
    }

    public void OnClose() {
        SyncUi();
    }

    private void SyncUi() {
        String bid = "Available Bid: ";
        if (_placement._availableBid != null) {
            bid += _placement._availableBid._id + " (" + _placement._availableBid._price + ")";
        }
        _availableBid.setText(bid);
        _bidButton.setEnabled(!_placement._isBidding);
        _loadButton.setText(_placement._isLoading ? "Loading" : "Load");
        _loadButton.setEnabled(_placement.CanLoad());

        bid = "Buffer Bid: ";
        if (_placement._bufferBid != null) {
            bid += _placement._bufferBid._id;
        }
        _bufferedBid.setText(bid);
        _showButton.setEnabled(_placement.CanShow());

        bid = "Rendered Bid: ";
        if (_placement._renderedBid != null) {
            bid += _placement._renderedBid._id;
        }
        _renderedBid.setText(bid);
        _closeButton.setEnabled(_placement._renderedBid != null);
    }
}