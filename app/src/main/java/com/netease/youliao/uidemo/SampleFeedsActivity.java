package com.netease.youliao.uidemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.netease.youliao.newsfeeds.core.NNewsFeedsSDK;
import com.netease.youliao.newsfeeds.model.NNFNewsInfo;
import com.netease.youliao.newsfeeds.ui.base.activity.BaseNavigationBarActivity;
import com.netease.youliao.newsfeeds.ui.core.NNewsFeedsUI;
import com.netease.youliao.newsfeeds.ui.core.callbacks.NNFOnFeedsCallback;
import com.netease.youliao.newsfeeds.ui.core.NNFeedsFragment;
import com.netease.youliao.newsfeeds.ui.core.callbacks.NNFOnShareCallback;
import com.netease.youliao.newsfeeds.ui.core.details.DefaultMoreVideosActivity;
import com.netease.youliao.newsfeeds.ui.utils.NNFUIConstants;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.Map;

public class SampleFeedsActivity extends BaseNavigationBarActivity {

    private final static String TAG = "SampleFeedsActivity";
    // 高德定位
    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;
    private NNFeedsFragment mFeedsFragment;
    public static SampleFeedsActivity sInstance;

    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRealContentView(R.layout.activity_main);

        api = WXAPIFactory.createWXAPI(this, BuildConfig.SHARE_WX_APP_ID, true);
        api.registerApp(BuildConfig.SHARE_WX_APP_ID);

        setTitle("网易有料");
        // 不显示左侧返回按钮
        setLeftViewVisible(false);
        getNavigationBarView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeedsFragment != null) {
                    // 点击导航栏返回到顶部，模仿ios点击状态栏返回到顶部的功能
                    mFeedsFragment.scrollToTop();
                }
            }
        });

        sInstance = SampleFeedsActivity.this;


        /********* 集成方式请二选一 *********/

        // 快速集成
//        initFeedsByOneStep();

        // 自定义集成
        initFeedsStepByStep();

        /********* 集成方式请二选一 *********/

        initLocation();

        parseIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        parseIntent(intent);
    }

    /**
     * 分享回流处理
     *
     * @param intent
     */
    private void parseIntent(Intent intent) {
        if (null == intent) return;

        Uri uri = intent.getData();

        if (null != uri) {
            String infoId = uri.getQueryParameter("infoId");

            if (!TextUtils.isEmpty(infoId)) {
                NNFNewsInfo newsInfo = new NNFNewsInfo();
                newsInfo.infoId = infoId;
                newsInfo.source = uri.getQueryParameter("source");
                String infoType = uri.getQueryParameter("infoType");
                newsInfo.infoType = TextUtils.isEmpty(infoType) ? NNFUIConstants.INFO_TYPE_ARTICLE : infoType;
                String producer = uri.getQueryParameter("producer");
                newsInfo.producer = TextUtils.isEmpty(producer) ? "recommendation" : producer;

                switch (newsInfo.infoType) {
                    case NNFUIConstants.INFO_TYPE_ARTICLE:
                        SampleArticleActivity.start(this, newsInfo);
                        break;
                    case NNFUIConstants.INFO_TYPE_PICSET:
                        SamplePicSetGalleryActivity.start(this, newsInfo);
                        break;
                    case NNFUIConstants.INFO_TYPE_VIDEO:
                        DefaultMoreVideosActivity.start(this, newsInfo.infoId, newsInfo.infoType, newsInfo.producer);
                        break;
                }

            }
        }
    }

    /**
     * 第一步：接入信息流UI SDK，快速集成信息流主页 NNFeedsFragment
     */
    private void initFeedsByOneStep() {
        // 设置全局分享回调
        NNewsFeedsUI.setShareCallback(new NNFOnShareCallback() {
            @Override
            public void onWebShareClick(Map<String, String> shareInfo, int index) {
                // context不能传入activity，这是因为会发生发生页面跳转后，activity onStop，图片加载也会随之stop
                ShareUtil.shareImp(getApplicationContext(), api, shareInfo, index);
            }
        });
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFeedsFragment = NNewsFeedsUI.createFeedsFragment(null, null);
        ft.replace(R.id.fragment_container, mFeedsFragment);
        ft.commitAllowingStateLoss();
    }

    /**
     * 第一步：接入信息流UI SDK，自定义集成信息流主页 NNFeedsFragment
     */
    private void initFeedsStepByStep() {
        // 设置全局分享回调
//        NNewsFeedsUI.setShareCallback(new NNFOnShareCallback() {
//            @Override
//            public void onWebShareClick(Map<String, String> shareInfo, int index) {
//                // context不能传入activity，这是因为会发生发生页面跳转后，activity onStop，图片加载也会随之stop
//                ShareUtil.shareImp(getApplicationContext(), api, shareInfo, index);
//            }
//        });
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFeedsFragment = NNewsFeedsUI.createFeedsFragment(new FeedsCallbackSample(), null);
        ft.replace(R.id.fragment_container, mFeedsFragment);
        ft.commitAllowingStateLoss();
    }

    /**
     * 第二步：可选，为信息流主页 NNFeedsFragment 设置点击事件回调；如不设置，使用SDK内部的默认回调
     */
    private class FeedsCallbackSample extends NNFOnFeedsCallback {
        @Override
        public void onNewsClick(Context context, NNFNewsInfo newsInfo, Object extraData) {
            if (null != newsInfo && null != newsInfo.infoType) {
                if (NNFUIConstants.INFO_TYPE_ARTICLE.equals(newsInfo.infoType)) {
                    /**
                     * 第三步：自定义文章类新闻展示页面
                     */
                    SampleArticleActivity.start(context, newsInfo);
                } else if (NNFUIConstants.INFO_TYPE_PICSET.equals(newsInfo.infoType)) {
                    /**
                     * 第四步：自定义图集类新闻展示页面
                     */
                    SamplePicSetGalleryActivity.start(context, newsInfo);
                } else if (NNFUIConstants.INFO_TYPE_VIDEO.equals(newsInfo.infoType)) {
                    DefaultMoreVideosActivity.start(context, newsInfo);
                }
            }
        }
    }

    public NNFeedsFragment getFeedsFragment() {
        return mFeedsFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocation();
        destroyLocation();
    }

    /***************接入定位SDK获取经纬度****************/

    /**
     * 为了准确推荐本地新闻，NNewsFeedsSDK 内部需要获取经纬度。用户可以通过调用以下接口传入最新的经纬度：
     *
     * NNewsFeedsSDK.getInstance().setLocation(double longitude, double latitude);
     *
     * 由于经纬度的获取涉及面较广，所以我们推荐App层自己实现，SDK内部不主动读取经纬度。
     * 作为演示，本DEMO接入的是高德定位SDK，用户也可以使用其他定位SDK。
     */

    /**
     * 初始化定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void initLocation() {
        //初始化client
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        mLocationOption = getDefaultOption();
        //设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 设置定位监听
        mLocationClient.setLocationListener(locationListener);
    }

    /**
     * 默认的定位参数
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    /**
     * 开始定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startLocation() {
        // 设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 启动定位
        mLocationClient.startLocation();
    }

    /**
     * 停止定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void stopLocation() {
        // 停止定位
        mLocationClient.stopLocation();
    }

    /**
     * 销毁定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void destroyLocation() {
        if (null != mLocationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            mLocationClient.onDestroy();
            mLocationClient = null;
            mLocationOption = null;
        }
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (null != location) {
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if (location.getErrorCode() == 0) {
                    /**
                     * 将实时变化的经纬度传入 NNewsFeedsSDK
                     */
                    NNewsFeedsSDK.getInstance().setLocation(location.getLongitude(), location.getLatitude());
                    Log.v(TAG, "定位成功" + "\n");
                    Log.v(TAG, "定位类型: " + location.getLocationType() + "\n");
//                    Toast.makeText(SampleFeedsActivity.this, "经    度    : " + location.getLongitude() + "\n纬    度    : " + location.getLatitude(), Toast.LENGTH_LONG).show();
                    Log.v(TAG, "经    度    : " + location.getLongitude() + "\n");
                    Log.v(TAG, "纬    度    : " + location.getLatitude() + "\n");
                    Log.v(TAG, "精    度    : " + location.getAccuracy() + "米" + "\n");
                    Log.v(TAG, "提供者    : " + location.getProvider() + "\n");

                    Log.v(TAG, "速    度    : " + location.getSpeed() + "米/秒" + "\n");
                    Log.v(TAG, "角    度    : " + location.getBearing() + "\n");
                    // 获取当前提供定位服务的卫星个数
                    Log.v(TAG, "星    数    : " + location.getSatellites() + "\n");
                    Log.v(TAG, "国    家    : " + location.getCountry() + "\n");
                    Log.v(TAG, "省            : " + location.getProvince() + "\n");
                    Log.v(TAG, "市            : " + location.getCity() + "\n");
                    Log.v(TAG, "城市编码 : " + location.getCityCode() + "\n");
                    Log.v(TAG, "区            : " + location.getDistrict() + "\n");
                    Log.v(TAG, "区域 码   : " + location.getAdCode() + "\n");
                    Log.v(TAG, "地    址    : " + location.getAddress() + "\n");
                    Log.v(TAG, "兴趣点    : " + location.getPoiName() + "\n");
                } else {
                    //定位失败
                    Log.e(TAG, "定位失败" + "\n");
                    Log.e(TAG, "错误码:" + location.getErrorCode() + "\n");
                    Log.e(TAG, "错误信息:" + location.getErrorInfo() + "\n");
                    Log.e(TAG, "错误描述:" + location.getLocationDetail() + "\n");
                }

            } else {
                Log.e(TAG, "定位失败，loc is null");
            }
        }
    };

    /***************接入定位SDK获取经纬度****************/
}
