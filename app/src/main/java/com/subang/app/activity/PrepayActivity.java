package com.subang.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.subang.app.util.AppConst;
import com.subang.bean.WeixinPayRequest;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class PrepayActivity extends Activity {

    private IWXAPI wxapi;
    private WeixinPayRequest payRequest;
    private PayReq payReq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wxapi = WXAPIFactory.createWXAPI(this, null);
        payRequest = (WeixinPayRequest) getIntent().getSerializableExtra("payrequest");
        wxapi.registerApp(AppConst.APP_ID);
        genPayReq();
        wxapi.sendReq(payReq);
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void genPayReq() {
        payReq = new PayReq();
        payReq.appId = payRequest.getAppid();
        payReq.partnerId = payRequest.getPartnerid();
        payReq.prepayId = payRequest.getPrepayid();
        payReq.packageValue = payRequest.getPackage_();
        payReq.nonceStr = payRequest.getNoncestr();
        payReq.timeStamp = payRequest.getTimestamp();
        payReq.sign = payRequest.getSign();
    }
}