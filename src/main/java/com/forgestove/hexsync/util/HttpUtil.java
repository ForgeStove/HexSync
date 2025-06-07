package com.forgestove.hexsync.util;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
public class HttpUtil {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	public static <T> HttpResponse<T> sendGet(String url, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build();
		return HTTP_CLIENT.send(request, bodyHandler);
	}
}
