package com.shiftsaver.network

import com.shiftsaver.model.DownloadRequest
import com.shiftsaver.model.DownloadResponse
import com.shiftsaver.model.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ShiftSaverApi {
    @GET("/status")
    suspend fun getStatus(): Response<StatusResponse>

    @POST("/download")
    suspend fun download(@Body request: DownloadRequest): Response<DownloadResponse>
}
