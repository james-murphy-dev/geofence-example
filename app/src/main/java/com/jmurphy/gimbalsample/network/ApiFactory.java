package com.jmurphy.gimbalsample.network;

import android.content.Context;

import com.jmurphy.gimbalsample.R;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFactory {

    public static ApiService create(Context context){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        return retrofit.create(ApiService.class);
    }
}
