package com.forgestove.hexsync.util;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
public class HttpUtil {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	public static final String POST = "POST";
	public static final String GET = "GET";
	public static HttpResponse<String> sendPost(String url) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).POST(BodyPublishers.noBody()).build();
		return HTTP_CLIENT.send(request, BodyHandlers.ofString());
	}
	public static HttpResponse<InputStream> sendGetStream(String url) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build();
		return HTTP_CLIENT.send(request, BodyHandlers.ofInputStream());
	}
}
