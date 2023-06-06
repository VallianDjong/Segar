package com.capstoneC23PS274.segar.data

import com.capstoneC23PS274.segar.data.preference.UserPreference
import com.capstoneC23PS274.segar.data.remote.body.LoginBody
import com.capstoneC23PS274.segar.data.remote.body.RegisterBody
import com.capstoneC23PS274.segar.data.remote.response.CheckResponse
import com.capstoneC23PS274.segar.data.remote.response.CheckResult
import com.capstoneC23PS274.segar.data.remote.response.CommonResponse
import com.capstoneC23PS274.segar.data.remote.response.DictDetailItem
import com.capstoneC23PS274.segar.data.remote.response.DictionaryDetailResponse
import com.capstoneC23PS274.segar.data.remote.response.DictionaryItem
import com.capstoneC23PS274.segar.data.remote.response.HistoryItem
import com.capstoneC23PS274.segar.data.remote.response.LoginResponse
import com.capstoneC23PS274.segar.data.remote.response.UserData
import com.capstoneC23PS274.segar.data.remote.retrofit.ApiService
import com.capstoneC23PS274.segar.ui.screen.camera.reduceFileImage
import com.capstoneC23PS274.segar.utils.ConstantValue
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SegarRepository (private val apiService: ApiService, private val userPreference: UserPreference) {

    private val token = ConstantValue.AUTH + userPreference.getToken()

    suspend fun postLogin(loginBody: LoginBody) : Flow<LoginResponse> {
        val response = apiService.postLoginUser(loginBody)
        return if (response.isSuccessful && response.body() != null && response.code() != 401){
            val result : LoginResponse = response.body()!!
            userPreference.login(result.data?.token.toString())
            flowOf(result)
        } else {
            val errResponse = getErrBody(response.errorBody()?.string())
            val result = LoginResponse(
                error = errResponse.error,
                message = errResponse.message
            )
            flowOf(result)
        }
    }

    suspend fun postRegister(registerBody: RegisterBody) : Flow<CommonResponse> {
        val result : CommonResponse = apiService.postRegisterUser(registerBody)
        return  flowOf(result)
    }

    suspend fun getDictionary() : Flow<List<DictionaryItem>>{
        val result : List<DictionaryItem> = apiService.getAllDict(token).data
        return flowOf(result)
    }

    suspend fun getDictionaryDetail(id: String) : Flow<DictionaryDetailResponse>{
        val response = apiService.getDictDetail(token, id)
        return if (response.isSuccessful && response.body() != null) {
            val result : DictionaryDetailResponse = response.body()!!
            flowOf(result)
        } else {
            val errResponse = getErrBody(response.errorBody()?.string())
            val result = DictionaryDetailResponse(
                error = errResponse.error,
                message = errResponse.message
            )
            flowOf(result)
        }
    }

    suspend fun getHistory() : Flow<List<HistoryItem>>{
        val result : List<HistoryItem> = apiService.getHistory(token).data
        return flowOf(result)
    }

    suspend fun getUserDetail() : Flow<UserData> {
        val result : UserData = apiService.getUserProfile(token).data
        return flowOf(result)
    }

    suspend fun postCheckImage(file: File) : Flow<CheckResponse>{
        val uploadFile = reduceFileImage(file)
        val requestImageFile = uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imageMultipart : MultipartBody.Part = MultipartBody.Part.createFormData(
            "image",
            uploadFile.name,
            requestImageFile
        )
        val response = apiService.postCheckImage(token, imageMultipart)
        return if (response.isSuccessful && response.body() != null) {
            val result: CheckResponse = response.body()!!
            flowOf(result)
        } else {
            val errResponse = getErrBody(response.errorBody()?.string())
            val result: CheckResponse = CheckResponse(
                error = errResponse.error,
                message = errResponse.message
            )
            flowOf(result)
        }
    }

    fun logout(){
        userPreference.logout()
    }

    private fun getErrBody(errBody: String?) : CommonResponse{
        val gson = Gson()
        return gson.fromJson(errBody, CommonResponse::class.java)
    }
}