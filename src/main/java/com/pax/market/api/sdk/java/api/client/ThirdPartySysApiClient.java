/*
 * *******************************************************************************
 * COPYRIGHT
 *               PAX TECHNOLOGY, Inc. PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with PAX  Technology, Inc. and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *
 *      Copyright (C) 2017 PAX Technology, Inc. All rights reserved.
 * *******************************************************************************
 */
package com.pax.market.api.sdk.java.api.client;


import com.google.gson.JsonSyntaxException;
import com.pax.market.api.sdk.java.api.BaseThirdPartySysApi;
import com.pax.market.api.sdk.java.api.base.dto.BaseDTO;
import com.pax.market.api.sdk.java.api.base.request.SdkRequest;
import com.pax.market.api.sdk.java.api.constant.Constants;
import com.pax.market.api.sdk.java.api.constant.ResultCode;
import com.pax.market.api.sdk.java.api.util.CryptoUtils;
import com.pax.market.api.sdk.java.api.util.EnhancedJsonUtils;
import com.pax.market.api.sdk.java.api.util.ThirdPartySysHttpUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 客户端
 */
public class ThirdPartySysApiClient {

	private static final Logger logger = LoggerFactory.getLogger(ThirdPartySysApiClient.class.getSimpleName());

    /**
     * The Base url.
     */
    protected String baseUrl;
    /**
     * The Api key.
     */
    protected String apiKey;
    /**
     * The Api secret.
     */
    protected String apiSecret;
    /**
     * The Sign method.
     */
    protected String signMethod = Constants.SIGN_METHOD_HMAC;
    /**
     * The Connect timeout.
     */
    protected int connectTimeout = 30000; 			// 默认连接超时时间为30秒
    /**
     * The Read timeout.
     */
    protected int readTimeout = 30000; 				// 默认响应超时时间为30秒

    protected int retryTimes = 3; //默认重试次数
    
    private boolean isThirdPartySys = false;


    /**
     * Instantiates a new Third party sys api client.
     *
     * @param baseUrl   the base url
     * @param apiKey    the api key
     * @param apiSecret the api secret
     */
    public ThirdPartySysApiClient(String baseUrl, String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.baseUrl = baseUrl;
		this.isThirdPartySys=true;
		if(BaseThirdPartySysApi.connectTimeout>0) {
			this.connectTimeout = BaseThirdPartySysApi.connectTimeout;
		}
		if(BaseThirdPartySysApi.readTimeout>0) {
			this.readTimeout = BaseThirdPartySysApi.readTimeout;
		}
		if(BaseThirdPartySysApi.retryTimes>0) {
		    this.retryTimes = BaseThirdPartySysApi.retryTimes;
        }
	}
    
    public ThirdPartySysApiClient(String baseUrl, String apiKey, String apiSecret, boolean isThirdPartySys) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.baseUrl = baseUrl;
		this.isThirdPartySys = isThirdPartySys;
	}


    /**
     * Instantiates a new Third party sys api client.
     *
     * @param baseUrl        the base url
     * @param apiKey         the api key
     * @param apiSecret      the api secret
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     */
    public ThirdPartySysApiClient(String baseUrl, String apiKey, String apiSecret, int connectTimeout, int readTimeout) {
		this(baseUrl, apiKey, apiSecret);
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.isThirdPartySys = true;
	}

    /**
     * Instantiates a new Third party sys api client.
     *
     * @param baseUrl        the base url
     * @param apiKey         the api key
     * @param apiSecret      the api secret
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     * @param retryTimes     the retry times
     */
    public ThirdPartySysApiClient(String baseUrl, String apiKey, String apiSecret, int connectTimeout, int readTimeout, int retryTimes) {
        this(baseUrl, apiKey, apiSecret, connectTimeout, readTimeout);
        this.retryTimes = retryTimes;
    }

    /**
     * Instantiates a new Default client.
     *
     * @param baseUrl        the base url
     * @param appKey         the app key
     * @param appSecret      the app secret
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     * @param signMethod     the sign method
     */
    public ThirdPartySysApiClient(String baseUrl, String appKey, String appSecret, int connectTimeout, int readTimeout, String signMethod) {
		this(baseUrl, appKey, appSecret, connectTimeout, readTimeout);
		this.signMethod = signMethod;
	}

    /**
     * Execute string.
     *
     * @param request the request
     * @return the string
     */
    public String execute(SdkRequest request) {
    	request.addHeader(Constants.REQ_HEADER_SDK_LANG, Constants.THIRD_PARTY_API_SDK_LANGUAGE);
    	request.addHeader(Constants.REQ_HEADER_SDK_VERSION, Constants.THIRD_PARTY_API_SDK_VERSION);
		try {
			return _execute(request);
		} catch (IOException e) {
			logger.error("IOException occurred when execute request. Details: {}", e.toString());
		} catch (GeneralSecurityException e) {
			logger.error("GeneralSecurityException occurred when execute request. Details: {}", e.toString());
		}
		return EnhancedJsonUtils.getSdkJson(ResultCode.SDK_RQUEST_EXCEPTION);
	}

	protected String _execute(SdkRequest request) throws IOException, GeneralSecurityException {
		String response;

		if(apiKey != null) {
			if(this.isThirdPartySys) {
				request.addRequestParam("sysKey", apiKey);
			}else {
				request.addRequestParam(Constants.APP_KEY, apiKey);
			}
			
		}
		Long timestamp = request.getTimestamp();
		if(timestamp == null){
			timestamp = System.currentTimeMillis();
		}
		request.addRequestParam(Constants.TIMESTAMP, Long.toString(timestamp));

		String query = ThirdPartySysHttpUtils.buildQuery(request.getRequestParams(), Constants.CHARSET_UTF8);
		if(apiSecret != null) {
			String signature = CryptoUtils.signRequest(query, null, apiSecret, signMethod);
			request.addHeader(Constants.SIGNATURE, signature);
		}
		String requestUrl = ThirdPartySysHttpUtils.buildRequestUrl(baseUrl + request.getRequestMappingUrl(), query);
		logger.info(" --> {} {}", request.getRequestMethod().getValue(), requestUrl);

		if(!request.isCompressData()){
			response = ThirdPartySysHttpUtils.request(requestUrl, request.getRequestMethod().getValue(), connectTimeout, readTimeout, request.getRequestBody(), request.getHeaderMap(), retryTimes);
		} else {
			response = ThirdPartySysHttpUtils.compressRequest(requestUrl, request.getRequestMethod().getValue(), connectTimeout, readTimeout, request.getRequestBody(), request.getHeaderMap(), retryTimes);
		}
		try{
			EnhancedJsonUtils.fromJson(response, BaseDTO.class);
		} catch (JsonSyntaxException e){
			return EnhancedJsonUtils.getSdkJson(999,response);
		}

		return response;
	}

    /**
     * 设置API请求的连接超时时间
     *
     * @param connectTimeout the connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

    /**
     * 设置API请求的读超时时间
     *
     * @param readTimeout the read timeout
     */
    public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

    /**
     * Sets retry times.
     *
     * @param retryTimes the retry times
     */
    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

}
