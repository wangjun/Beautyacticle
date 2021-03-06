package com.nicodelee.beautyarticle.viewhelper;

import android.content.Context;
import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of ImageDownloader which uses {@link com.squareup.okhttp.OkHttpClient} for image stream retrieving.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @author Leo Link (mr[dot]leolink[at]gmail[dot]com)
 */
public class OkHttpImageDownloader extends BaseImageDownloader {

	private OkHttpClient client;

	public OkHttpImageDownloader(Context context, OkHttpClient client) {
		super(context);
		this.client = client;
	}

	@Override
	protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
		Request request = new Request.Builder().url(imageUri).build();
		ResponseBody responseBody = client.newCall(request).execute().body();
		InputStream inputStream = responseBody.byteStream();
		int contentLength = (int) responseBody.contentLength();
		return new ContentLengthInputStream(inputStream, contentLength);
	}
}
