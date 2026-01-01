package com.example.tvapp.cms

import com.example.tvapp.model.* 
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CmsService {
    @POST("api/devices/register")
    suspend fun registerDevice(@Body payload: DeviceRegisterRequest)

    @GET("api/devices/status/{device_code}")
    suspend fun getDeviceStatus(@Path("device_code") deviceCode: String): DeviceStatusResponse

    @GET("api/devices/config/{device_code}")
    suspend fun getScreenConfig(@Path("device_code") deviceCode: String): ScreenConfigResponse

    // UPDATED: Now sends the full HeartbeatPayload
    @PUT("api/devices/heartbeat/{device_code}")
    suspend fun postHeartbeat(@Path("device_code") deviceCode: String, @Body payload: HeartbeatPayload)

    // ADDED: Endpoint for sending specific playback events
    @POST("api/devices/events/{device_code}")
    suspend fun postEvent(@Path("device_code") deviceCode: String, @Body payload: PlaybackEventPayload)
}