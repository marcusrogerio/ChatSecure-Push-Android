package org.chatsecure.pushsecure;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.chatsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.DeviceList;
import org.chatsecure.pushsecure.response.Message;
import org.chatsecure.pushsecure.response.PushToken;
import org.chatsecure.pushsecure.response.TokenList;

import java.util.Date;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import timber.log.Timber;

/**
 * An API client for the ChatSecure Push Server
 * Created by davidbrodsky on 6/23/15.
 */
public class PushSecureClient {

    private PushSecureApi api;
    private String token;

    public PushSecureClient(@NonNull String apiHost) {
        this(apiHost, null);
    }

    public PushSecureClient(@NonNull String apiHost, @Nullable Account account) {

        if (account != null) token = account.token;

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(chain -> {
            if (token == null) return chain.proceed(chain.request());

            // Log request
            Timber.d(chain.request().toString());

            // If a ChatSecure-Push auth token has been set, attach that to each request
            Request modifiedRequest = chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Token " + token)
                    .build();
            return chain.proceed(modifiedRequest);
        });

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new org.chatsecure.pushsecure.response.typeadapter.DjangoDateTypeAdapter())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(apiHost)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(PushSecureApi.class);
    }

    public void setAccount(@Nullable Account account) {
        this.token = account != null ? account.token : null;
    }

    /**
     * Authenticate an account with the given credentials, creating one if none exists.
     *
     * @return an {@link Account} representing the newly created or existing account matching
     * the passed credentials. This should be passed to {@link #setAccount(Account)} before
     * performing any other operations with this client.
     */
    public Call<Account> authenticateAccount(@NonNull String username,
                                             @NonNull String password,
                                             @Nullable String email) {

        return api.authenticateAccount(username, password, email);
    }

    public Call<Device> createDevice(@NonNull String gcmRegistrationId,
                                     @Nullable String name,
                                     @Nullable String gcmDeviceId) {

        return api.createDevice(gcmRegistrationId, name, gcmDeviceId);
    }

    public Call<PushToken> createToken(@NonNull Device device, @Nullable String name) {
        return api.createToken(name, device.id);
    }

    public Call<Void> deleteToken(@NonNull String token) {
        return api.deleteToken(token);
    }

    public Call<TokenList> getTokens() {
        return api.getTokens();
    }

    public Call<Message> sendMessage(@NonNull String recipientToken,
                                     @Nullable String data) {

        return api.sendMessage(recipientToken, data);
    }

    public Call<DeviceList> getGcmDevices() {

        return api.getGcmDevices();
    }

    public Call<DeviceList> getApnsDevices() {

        return api.getApnsDevices();
    }

//    public Observable<List<Device>> getAllDevices() {
//        return Observable.concat(
//                getGcmDevices()
//                        .flatMap(gcmDeviceList -> Observable.from(gcmDeviceList.results))
//                        .map(gcmDevice -> new Device(gcmDevice, Device.Type.GCM)),
//                getApnsDevices()
//                        .flatMap(apnsDeviceList -> Observable.from(apnsDeviceList.results))
//                        .map(apnsDevice -> new Device(apnsDevice, Device.Type.APNS)))
//                .toList();
//    }

    /**
     * Update properties of the current device. Note that changes to {@link Device#id} will
     * not be respected
     */
    public Call<Device> updateDevice(@NonNull Device device) {
        return api.updateDevice(device.id, device);
    }

    public Call<Void> deleteDevice(@NonNull String id) {
        return api.deleteDevice(id);
    }
}
