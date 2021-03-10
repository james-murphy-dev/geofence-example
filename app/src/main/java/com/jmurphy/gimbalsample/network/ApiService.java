package com.jmurphy.gimbalsample.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET(".")
    Call<Response> getMessage();
}
