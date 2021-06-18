package com.motorbesitzen.statuswatcher.bot.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorbesitzen.statuswatcher.bot.scraper.entity.ProductStatus;
import com.motorbesitzen.statuswatcher.bot.service.EnvSettings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to perform product status API requests.
 */
@Service
public class ProductStatusApiRequest {

	private static final int REQUEST_TIMEOUT_MS = 10000;
	private final EnvSettings envSettings;
	private final ObjectMapper objectMapper;

	@Autowired
	private ProductStatusApiRequest(final EnvSettings envSettings, final ObjectMapper objectMapper) {
		this.envSettings = envSettings;
		this.objectMapper = objectMapper;
	}

	/**
	 * Requests the status list from the API.
	 * @return A list of all the matching statuses available in the API.
	 * @throws IOException if the API times out or if the response can not be understood.
	 * @throws IllegalStateException if the API URL is not set.
	 */
	public List<ProductStatus> getStatusList() throws IOException {
		final String statusApiUrl = envSettings.getProductStatusApiUrl();
		if (statusApiUrl.isBlank()) {
			throw new IllegalStateException("Product status API URL not set!");
		}

		return getProductStatusList(statusApiUrl);
	}

	/**
	 * Requests the product status from the API and transforms the JSON response to a list of product statuses.
	 * @param statusApiUrl The URL of the product status API.
	 * @return A list of all the matching statuses available in the API.
	 * @throws IOException if the API times out or if the response can not be understood.
	 */
	private List<ProductStatus> getProductStatusList(String statusApiUrl) throws IOException {
		final String json = getProductStatus(statusApiUrl);
		final JsonNode rootNode = objectMapper.readTree(json);
		return processJsonNode(rootNode, new ArrayList<>());
	}

	/**
	 * Executes the GET request to the API and parses the response body to text.
	 * @param statusApiUrl The URL of the product status API.
	 * @return The response in JSON.
	 * @throws IOException if the API times out or if the response can not be understood.
	 */
	private String getProductStatus(final String statusApiUrl) throws IOException {
		final RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(REQUEST_TIMEOUT_MS)
				.setConnectionRequestTimeout(REQUEST_TIMEOUT_MS)
				.setSocketTimeout(REQUEST_TIMEOUT_MS)
				.build();
		final HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		final HttpGet request = new HttpGet(statusApiUrl);
		final HttpResponse response = httpClient.execute(request);
		final HttpEntity entity = response.getEntity();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
		final String line = reader.readLine();
		reader.close();
		return line;
	}

	/**
	 * Recursively traverses the JSON node tree to find every object that can be parsed to a product status object.
	 * @param node The current node.
	 * @param statusList The current list of product statuses.
	 * @return The list of product statuses.
	 */
	private List<ProductStatus> processJsonNode(final JsonNode node, final List<ProductStatus> statusList) {
		if (node == null) {
			return statusList;
		} else if (node.isValueNode()) {
			return statusList;
		} else if (node.isArray()) {
			for (JsonNode arrayContent : node) {
				processJsonNode(arrayContent, statusList);
			}
		} else if (node.isObject()) {
			try {
				final ProductStatus productStatus = objectMapper.treeToValue(node, ProductStatus.class);
				statusList.add(productStatus);
			} catch (JsonProcessingException e) {
				for (JsonNode objectContent : node) {
					processJsonNode(objectContent, statusList);
				}
			}
		}

		return statusList;
	}
}
