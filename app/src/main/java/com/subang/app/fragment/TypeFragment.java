package com.subang.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.subang.api.OrderAPI;
import com.subang.app.activity.R;
import com.subang.app.adapter.OrderAdapter;
import com.subang.app.fragment.face.OnFrontListener;
import com.subang.app.util.AppConst;
import com.subang.app.util.AppShare;
import com.subang.app.util.AppUtil;
import com.subang.applib.view.XListView;
import com.subang.bean.OrderDetail;
import com.subang.bean.Result;
import com.subang.domain.Order;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;


public class TypeFragment extends Fragment implements OnFrontListener {

    private AppShare appShare;

    private int type;
    private XListView xlv_order;
    private OrderAdapter orderAdapter;

    private Thread thread, operaThread;
    List<OrderDetail> orderDetails;
    private OrderAdapter.DataHolder dataHolder;
    private OrderDetail filter;

    private boolean isLoaded = false;

    AdapterView.OnItemClickListener orderOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }
    };

    XListView.IXListViewListener orderListViewListener = new XListView.IXListViewListener() {
        @Override
        public void onRefresh() {
            if (thread == null || !thread.isAlive()) {
                thread = new Thread(runnable);
                thread.start();
            }
        }

        @Override
        public void onLoadMore() {
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConst.WHAT_NETWORK_ERR: {
                    xlv_order.stopRefresh();
                    AppUtil.networkTip(getActivity());
                    break;
                }
                case AppConst.WHAT_SUCC_LOAD: {
                    xlv_order.stopRefresh();
                    dataHolder.orderDetails = orderDetails;
                    orderAdapter.notifyDataSetChanged();
                    if (dataHolder.orderDetails.isEmpty()) {
                        xlv_order.setBackgroundResource(R.drawable.order_listview_bg);
                    } else {
                        xlv_order.setBackgroundResource(android.R.color.transparent);
                    }
                    isLoaded = true;
                    break;
                }
                case AppConst.WHAT_SUCC_SUBMIT: {
                    Bundle bundle = msg.getData();
                    OperaData operaData = (OperaData) bundle.get("operaData");
                    String info = bundle.getString("info");
                    if (thread == null || !thread.isAlive()) {
                        thread = new Thread(runnable);
                        thread.start();
                    }
                    Toast toast = Toast.makeText(getActivity(), info, Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                }
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            AppUtil.confApi(getActivity());
            orderDetails = OrderAPI.userList(type, filter);
            if (orderDetails == null) {
                handler.sendEmptyMessage(AppConst.WHAT_NETWORK_ERR);           //加载数据失败
                return;
            }
            handler.sendEmptyMessage(AppConst.WHAT_SUCC_LOAD);                //加载数据成功
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appShare = (AppShare) getActivity().getApplication();
        if (getArguments().containsKey("type")) {
            type = getArguments().getInt("type");
        }
        dataHolder = new OrderAdapter.DataHolder();
        filter = new OrderDetail();
        filter.setId(0);
        filter.setOrderno("");
        filter.setState(Order.State.accepted);
        filter.setDate(new Date(System.currentTimeMillis()));
        filter.setTime(0);
        filter.setMoney(0.0);
        filter.setFreight(0.0);
        filter.setCategoryname("");

        orderAdapter = new OrderAdapter(getActivity(), dataHolder);
        orderAdapter.setOperationOnClickListener(operationOnClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_type, container, false);
        findView(view);
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(runnable);
            thread.start();
        }

        xlv_order.setAdapter(orderAdapter);
        xlv_order.setOnItemClickListener(orderOnItemClickListener);
        xlv_order.setXListViewListener(orderListViewListener);
        xlv_order.setPullLoadEnable(false);
        xlv_order.setPullRefreshEnable(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean refresh;
        if (appShare.map.containsKey("refresh")) {
            refresh = (boolean) appShare.map.get("refresh");
            appShare.map.remove("refresh");
            if (refresh) {
                if (thread == null || !thread.isAlive()) {
                    thread = new Thread(runnable);
                    thread.start();
                }
            }
        }
    }

    @Override
    public void onFront() {
    }

    private void findView(View view) {
        xlv_order = (XListView) view.findViewById(R.id.xlv_order);
    }

    private View.OnClickListener operationOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            OperaData operaData = new OperaData();
            operaData.operation = (com.subang.domain.Order.State) v.getTag(R.id.key_operation);
            operaData.orderid = (Integer) v.getTag(R.id.key_orderid);
            switch (operaData.operation) {
                case canceled: {
                    if (operaThread == null || !operaThread.isAlive()) {
                        operaThread = new OperaThread(operaData);
                        operaThread.start();
                    }
                    break;
                }
                case paid: {
                    break;
                }
                case delivered: {
                    break;
                }
                case remarked: {
                    break;
                }
            }
        }
    };

    private static class OperaData implements Serializable {
        public Order.State operation;
        public Integer orderid;
    }

    private class OperaThread extends Thread {

        OperaData operaData;

        public OperaThread(OperaData operaData) {
            super();
            this.operaData = operaData;
        }

        @Override
        public void run() {
            Result result;
            Message msg;
            Bundle bundle;
            switch (operaData.operation) {
                case canceled: {
                    result = OrderAPI.cancel(operaData.orderid);
                    if (result == null) {
                        handler.sendEmptyMessage(AppConst.WHAT_NETWORK_ERR);
                        return;
                    }
                    msg = new Message();
                    msg.what = AppConst.WHAT_SUCC_SUBMIT;
                    bundle = new Bundle();
                    bundle.putSerializable("operaData", operaData);
                    bundle.putString("info", "订单取消成功。");
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                    break;
                }
                case paid: {
                    break;
                }
                case delivered: {
                    break;
                }
                case remarked: {
                    break;
                }
            }
        }
    }
}
