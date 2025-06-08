package com.forgestove.hexsync.util;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
public class HttpUtil {
	public static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	public static final String DOWNLOAD = "download", LIST = "list";
	public static <T> HttpResponse<T> sendGet(String url, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
		return CLIENT.send(HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(), bodyHandler);
	}
}
