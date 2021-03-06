package com.subang.app.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.subang.api.InfoAPI;
import com.subang.api.UserAPI;
import com.subang.app.activity.AddrActivity;
import com.subang.app.activity.BalanceActivity;
import com.subang.app.activity.FeedbackActivity;
import com.subang.app.activity.MallActivity;
import com.subang.app.activity.MoreActivity;
import com.subang.app.activity.PromoteActivity;
import com.subang.app.activity.R;
import com.subang.app.activity.RechargeActivity;
import com.subang.app.activity.SalaryActivity;
import com.subang.app.activity.TicketActivity;
import com.subang.app.activity.WebActivity;
import com.subang.app.fragment.face.OnFrontListener;
import com.subang.app.util.AppConf;
import com.subang.app.util.AppConst;
import com.subang.app.util.AppShare;
import com.subang.app.util.AppUtil;
import com.subang.domain.Info;
import com.subang.domain.User;
import com.subang.util.WebConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MineFragment extends Fragment implements OnFrontListener {

    private static final int NUM_ACTION = 7;
    private static final int NO_LINE = 0;
    private static final int YES_LINE = 1;

    private AppShare appShare;

    private RelativeLayout rl_money, rl_salary, rl_score;
    private TextView tv_cellnum, tv_recharge, tv_money, tv_score, tv_salary, tv_phone;
    private ListView lv_action;

    private SimpleAdapter actionSimpleAdapter;

    private Thread thread;
    private User user;
    private Info info;
    private List<Map<String, Object>> actionItems;

    private boolean isLoaded = false;

    private View.OnClickListener rechargeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isLoaded) {
                return;
            }
            Intent intent = new Intent(getActivity(), RechargeActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
        }
    };

    private View.OnClickListener moneyOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isLoaded) {
                return;
            }
            Intent intent = new Intent(getActivity(), BalanceActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
        }
    };

    private View.OnClickListener salaryOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isLoaded) {
                return;
            }
            Intent intent = new Intent(getActivity(), SalaryActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
        }
    };

    private View.OnClickListener scoreOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), WebActivity.class);
            intent.putExtra("title", "如何获得积分");
            intent.putExtra("url", WebConst.HOST_URI + "content/weixin/user/scoreintro.htm");
            startActivity(intent);
        }
    };

    private AdapterView.OnItemClickListener actionOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0: {
                    Intent intent = new Intent(getActivity(), AddrActivity.class);
                    startActivity(intent);
                    break;
                }
                case 1: {
                    Intent intent = new Intent(getActivity(), TicketActivity.class);
                    startActivity(intent);
                    break;
                }
                case 2: {
                    Intent intent = new Intent(getActivity(), PromoteActivity.class);
                    startActivity(intent);
                    break;
                }
                case 3: {
                    if (!isLoaded) {
                        return;
                    }
                    Intent intent = new Intent(getActivity(), MallActivity.class);
                    intent.putExtra("user", user);
                    startActivity(intent);
                    break;
                }
                case 4: {
                    Intent intent = new Intent(getActivity(), WebActivity.class);
                    intent.putExtra("title", "常见问题");
                    intent.putExtra("url", WebConst.HOST_URI + "weixin/info/faq.html");
                    startActivity(intent);
                    break;
                }
                case 5: {
                    Intent intent = new Intent(getActivity(), FeedbackActivity.class);
                    startActivity(intent);
                    break;
                }
                case 6: {
                    Intent intent = new Intent(getActivity(), MoreActivity.class);
                    startActivity(intent);
                    break;
                }
            }
        }
    };

    private SimpleAdapter.ViewBinder actionViewBinder = new SimpleAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            if (view.getId() == R.id.v_line) {
                int line = (int) data;
                if (line == YES_LINE) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
                return true;
            }
            return false;
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConst.WHAT_NETWORK_ERR: {
                    AppUtil.networkTip(getActivity());
                    break;
                }
                case AppConst.WHAT_SUCC_LOAD: {
                    tv_score.setText(user.getScore().toString());
                    tv_money.setText(user.getMoney().toString() + "元");
                    tv_salary.setText(user.getSalary().toString() + "元");
                    tv_phone.setText("客服热线：" + info.getPhone());
                    isLoaded = true;
                    break;
                }
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            AppUtil.confApi(getActivity());
            user = UserAPI.get();
            if (user == null) {
                handler.sendEmptyMessage(AppConst.WHAT_NETWORK_ERR);    //提示用户，停留此界面
                return;
            }
            info = InfoAPI.get(null);
            if (info == null) {
                handler.sendEmptyMessage(AppConst.WHAT_NETWORK_ERR);    //提示用户，停留此界面
                return;
            }
            handler.sendEmptyMessage(AppConst.WHAT_SUCC_LOAD);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appShare = (AppShare) getActivity().getApplication();
        createItems();
        actionSimpleAdapter = new SimpleAdapter(getActivity(), actionItems, R.layout.item_action,
                new String[]{"icon", "text", "line"}, new int[]{R.id.iv_icon, R.id.tv_text, R.id.v_line});
        actionSimpleAdapter.setViewBinder(actionViewBinder);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        findView(view);
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(runnable);
            thread.start();
        }

        AppUtil.conf(getActivity());
        tv_cellnum.setText(AppConf.cellnum);
        tv_recharge.setOnClickListener(rechargeOnClickListener);
        rl_money.setOnClickListener(moneyOnClickListener);
        rl_salary.setOnClickListener(salaryOnClickListener);
        rl_score.setOnClickListener(scoreOnClickListener);

        lv_action.setAdapter(actionSimpleAdapter);
        lv_action.setOnItemClickListener(actionOnItemClickListener);

        return view;
    }

    @Override
    public void onFront() {
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean refresh;
        if (appShare.map.containsKey("mine.refresh")) {
            refresh = (boolean) appShare.map.get("mine.refresh");
            appShare.map.remove("mine.refresh");
            if (refresh) {
                isLoaded = false;
                if (thread == null || !thread.isAlive()) {
                    thread = new Thread(runnable);
                    thread.start();
                }
            }
        }
    }

    private void findView(View view) {
        rl_money = (RelativeLayout) view.findViewById(R.id.rl_money);
        rl_salary = (RelativeLayout) view.findViewById(R.id.rl_salary);
        rl_score = (RelativeLayout) view.findViewById(R.id.rl_score);
        tv_cellnum = (TextView) view.findViewById(R.id.tv_cellnum);
        tv_recharge = (TextView) view.findViewById(R.id.tv_recharge);
        tv_money = (TextView) view.findViewById(R.id.tv_money);
        tv_salary = (TextView) view.findViewById(R.id.tv_salary);
        tv_score = (TextView) view.findViewById(R.id.tv_score);
        lv_action = (ListView) view.findViewById(R.id.lv_action);
        tv_phone = (TextView) view.findViewById(R.id.tv_phone);
    }

    private void createItems() {
        actionItems = new ArrayList<Map<String, Object>>(NUM_ACTION);
        int[] icons = {R.drawable.mine_address_icon, R.drawable.mine_ticket_icon, R.drawable.mine_promote_icon, R.drawable.mine_score_icon,
                R.drawable.mine_faq_icon, R.drawable.mine_feedback_icon, R.drawable.mine_more_icon};
        String[] texts = {"常用地址", "优惠券", "推荐有奖", "积分商城", "常见问题", "意见反馈", "更多"};
        Map<String, Object> actionItem;
        for (int i = 0; i < NUM_ACTION; i++) {
            actionItem = new HashMap<String, Object>();
            actionItem.put("icon", icons[i]);
            actionItem.put("text", texts[i]);
            actionItem.put("line", NO_LINE);
            actionItems.add(actionItem);
        }
        actionItems.get(3).put("line", YES_LINE);
        actionItems.get(6).put("line", YES_LINE);
    }

}
