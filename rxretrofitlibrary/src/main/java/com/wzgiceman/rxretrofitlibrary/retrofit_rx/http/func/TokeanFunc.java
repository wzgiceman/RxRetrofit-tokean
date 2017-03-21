package com.wzgiceman.rxretrofitlibrary.retrofit_rx.http.func;

import android.util.Log;

import com.wzgiceman.rxretrofitlibrary.retrofit_rx.Api.BaseApi;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.exception.HttpTimeException;

import retrofit2.Retrofit;
import rx.Observable;
import rx.functions.Func1;

/**
 * toekan失效统一处理
 * Created by WZG on 2017/3/21.
 */

public class TokeanFunc implements Func1<Object, Observable> {
    private BaseApi basePar;
    private Retrofit retrofit;

    public TokeanFunc(BaseApi basePar, Retrofit retrofit) {
        this.basePar = basePar;
        this.retrofit = retrofit;
    }

    @Override
    public Observable call(Object o) {
             /*这里是判断错误tokean之类过期的错误，假设所有接口全部失败*/
            /*这里简单的以tokean为空来判断，实际运用中需要通过服务器错误标示来判断启用tokean机制*/
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
                                                /*继续当前接口请求*/
                                return basePar.getObservable(retrofit);
                            }
                            throw new HttpTimeException("获取tokean失败");
                        }
                    });
        }
        Log.e("tag", "正常处理");
        return Observable.just(o);
    }
}
