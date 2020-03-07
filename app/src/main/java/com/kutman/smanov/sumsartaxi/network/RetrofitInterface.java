package com.kutman.smanov.sumsartaxi.network;

import com.kutman.smanov.sumsartaxi.models.Response;
import com.kutman.smanov.sumsartaxi.models.Session;
import com.kutman.smanov.sumsartaxi.models.Transport;
import com.kutman.smanov.sumsartaxi.models.User;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

public interface RetrofitInterface {

    @POST("users")
    Observable<Response> register(@Body User user);

    @POST("authenticate")
    Observable<Response> login();

    @GET("users/{email}")
    Observable<User> getProfile(@Path("email") String email);

    @PUT("users/{email}")
    Observable<Response> changePassword(@Path("email") String email, @Body User user);

    @POST("users/{email}/password")
    Observable<Response> resetPasswordInit(@Path("email") String email);

    @POST("users/{email}/password")
    Observable<Response> resetPasswordFinish(@Path("email") String email, @Body User user);

    @POST("get_user_transport/{email}")
    Observable<Transport> getUserTransport(@Path("email") String email, @Body User user);

    @POST("get_last_state/{email}")
    Observable<Session> getLastState(@Path("email") String email, @Body User user);

    @POST("transport_add/{email}")
    Observable<Response> transportAdd(@Path("email") String email, @Body Transport transport);

    @POST("transport_edit/{email}")
    Observable<Response> transportEdit(@Path("email") String email, @Body Transport transport);
}
