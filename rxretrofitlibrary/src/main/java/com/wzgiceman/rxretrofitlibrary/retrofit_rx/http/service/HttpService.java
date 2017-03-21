package com.wzgiceman.rxretrofitlibrary.retrofit_rx.http.service;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

/**
 * service-统一接口
 * Created by WZG on 2016/12/19.
 */

public interface HttpService {


    /**
     * 假设现在这个接口是获取tokean的接口
     *
     * @param once_no
     * @param tokean
     * @return
     */

    @FormUrlEncoded
    @POST("AppFiftyToneGraph/videoLink")
    Observable<String> getAllVedioBy(@Field("once_no") boolean once_no,@Field("tokean") String tokean);
}
