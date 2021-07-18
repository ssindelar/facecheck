package de.facecheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import de.facecheck.resource.FaceCheckResource;
import de.facecheck.resource.PingResource;

public class StreamLambdaHandler implements RequestStreamHandler {
	private static final ResourceConfig jerseyApplication = new ResourceConfig()
			.registerClasses(FaceCheckResource.class, PingResource.class)
			.register(JacksonFeature.class);
	private static final JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = JerseyLambdaContainerHandler
			.getAwsProxyHandler(jerseyApplication);

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		handler.proxyStream(inputStream, outputStream, context);
	}
}