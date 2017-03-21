package com.wzgiceman.rxretrofitlibrary.retrofit_rx.http;

import android.util.Log;

import com.trello.rxlifecycle.android.ActivityEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.Api.BaseApi;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.exception.FactoryException;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.exception.HttpTimeException;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.exception.RetryWhenNetworkException;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.listener.HttpOnNextListener;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.listener.HttpOnNextSubListener;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.subscribers.ProgressSubscriber;

import java.lang.ref.SoftReference;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * http交互处理类
 * Created by WZG on 2016/7/16.
 */
public class HttpManager {
    /*软引用對象*/
    private SoftReference<HttpOnNextListener> onNextListener;
    private SoftReference<HttpOnNextSubListener> onNextSubListener;
    private SoftReference<RxAppCompatActivity> appCompatActivity;

    public HttpManager(HttpOnNextListener onNextListener, RxAppCompatActivity appCompatActivity) {
        this.onNextListener = new SoftReference(onNextListener);
        this.appCompatActivity = new SoftReference(appCompatActivity);
    }

    public HttpManager(HttpOnNextSubListener onNextSubListener, RxAppCompatActivity appCompatActivity) {
        this.onNextSubListener = new SoftReference(onNextSubListener);
        this.appCompatActivity = new SoftReference(appCompatActivity);
    }


    /**
     * 处理http请求
     *
     * @param basePar 封装的请求数据
     */
    public void doHttpDeal(final BaseApi basePar) {
        //手动创建一个OkHttpClient并设置超时时间缓存等设置
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(basePar.getConnectionTime(), TimeUnit.SECONDS);

        /*创建retrofit对象*/
        final Retrofit retrofit = new Retrofit.Builder()
                .client(builder.build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(basePar.getBaseUrl())
                .build();

        Observable observable = basePar.getObservable(retrofit)
                /*失败后的retry配置*/
                .retryWhen(new RetryWhenNetworkException())
                /*异常处理*/
                .onErrorResumeNext(funcException)
                /*生命周期管理*/
                //.compose(appCompatActivity.get().bindToLifecycle())
                //Note:手动设置在activity onDestroy的时候取消订阅
                .compose(appCompatActivity.get().bindUntilEvent(ActivityEvent.DESTROY))
                /*http请求线程*/
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .flatMap(new Func1() {
                    @Override
                    public Observable call(Object o) {
                        /*这里是判断错误tokean之类过期的错误，假设所有接口全部失败*/
                        /*这里简单的以tokean为空来判断*/
                        if (basePar.getTokean() == null || "".equals(basePar.getTokean())) {
                            return basePar.getObservableTokean(retrofit)
                                    .flatMap(new Func1() {
                                        @Override
                                        public Observable call(Object o) {
                                        /*成功返回tokean数据*/
                                            if (o instanceof String) {
                                            /*解析出tokean传入到当前请求的api接口类中*/
                                                String tokean = "aaaa";
                                                Log.e("tag", "tokean获取成功");
                                                basePar.setTokean(tokean);
                                                return basePar.getObservable(retrofit);
                                            }
                                            throw new HttpTimeException("获取tokean失败");
                                        }
                                    });
                        }
                        Log.e("tag","正常处理");
                        return Observable.just(o);
                    }
                })
                /*回调线程*/
                .observeOn(AndroidSchedulers.mainThread());

        /*ober回调，链接式返回*/
        if (onNextSubListener!=null&&null != onNextSubListener.get()) {
            onNextSubListener.get().onNext(observable, basePar.getMethod());
        }

        /*数据String回调*/
        if (onNextListener!=null&&null != onNextListener.get()) {
            ProgressSubscriber subscriber = new ProgressSubscriber(basePar, onNextListener, appCompatActivity);
            observable.subscribe(subscriber);
        }

    }


    /**
     * 异常处理
     */
    Func1 funcException = new Func1<Throwable, Observable>() {
        @Override
        public Observable call(Throwable throwable) {
            return Observable.error(FactoryException.analysisExcetpion(throwable));
        }
    };


}
