package com.digitald4.common.server.service;

import static java.util.stream.Collectors.joining;

// import com.google.api.server.spi.auth.EspAuthenticator;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.UnauthorizedException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.IntStream;

/** The Echo API which Endpoints will be exposing. */
@Api(
		name = "echo",
		namespace = @ApiNamespace(
				ownerDomain = "common.digitald4.com",
				ownerName = "common.digitald4.com",
				packagePath = ""
		),
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
		}
)

public class Echo {
	private final Provider<HttpServletRequest> requestProvider;

	@Inject
	public Echo(Provider<HttpServletRequest> requestProvider) {
		this.requestProvider = requestProvider;
	}

	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod
	public EchoMessage echo(EchoMessage message, @Named("n") @Nullable Integer n) {
		return doEcho(message, n);
	}
	// [END echo_method

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "hello")
	public EchoMessage echoHello(@Named("n") @Nullable Integer n) {
		return doEcho(new EchoMessage().setMessage("hello"), n);
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "info")
	public EchoMessage info() {
		HttpServletRequest request = requestProvider.get();
		return new EchoMessage().setMessage(
				String.format("Method: %s, URL: %s, URI: %s, Server Name: %s",
						request.getMethod(), request.getRequestURL(), request.getRequestURI(), request.getServerName()));
	}

	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod(name = "echo_path_parameter", path = "{n}")
	public EchoMessage echoPathParameter(EchoMessage message, @Named("n") int n) {
		return doEcho(message, n);
	}

	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod(name = "echo_api_key", path = "echo_api_key", apiKeyRequired = AnnotationBoolean.TRUE)
	public EchoMessage echoApiKey(EchoMessage message, @Named("n") @Nullable Integer n) {
		return doEcho(message, n);
	}

	private EchoMessage doEcho(EchoMessage message, Integer n) {
		if (n != null && n > 0) {
			return message.setMessage(IntStream.range(0, n).mapToObj(num -> message.getMessage()).collect(joining(" ")));
		}
		return message;
	}

	/**
	 * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
	 * 401.
	 *
	 * Note that name is not specified. This will default to "{class name}.{method name}". For
	 * example, the default is "echo.getUserEmail".
	 *
	 * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
	 * to the API method name. httpMethod is added here for example purposes.
	 */
	@ApiMethod(
			path = "email",
			httpMethod = ApiMethod.HttpMethod.GET,
			// authenticators = {EspAuthenticator.class},
			audiences = {"YOUR_OAUTH_CLIENT_ID"},
			clientIds = {"YOUR_OAUTH_CLIENT_ID"}
	)
	public Email getUserEmail(User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		return new Email().setEmail(user.getEmail());
	}

	/**
	 * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
	 * 401.
	 *
	 * Note that name is not specified. This will default to "{class name}.{method name}". For
	 * example, the default is "echo.getUserEmail".
	 *
	 * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
	 * to the API method name. httpMethod is added here for example purposes.
	 */
	@ApiMethod(
			path = "firebase_user",
			httpMethod = ApiMethod.HttpMethod.GET,
			// authenticators = {EspAuthenticator.class},
			issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"YOUR-PROJECT-ID"})}
	)
	public Email getUserEmailFirebase(User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		return new Email().setEmail(user.getEmail());
	}

	public static class EchoMessage {
		private String message;

		public String getMessage() {
			return message;
		}

		public EchoMessage setMessage(String message) {
			this.message = message;
			return this;
		}
	}

	public static class Email {
		private String email;

		public String getEmail() {
			return email;
		}

		public Email setEmail(String email) {
			this.email = email;
			return this;
		}
	}
}
