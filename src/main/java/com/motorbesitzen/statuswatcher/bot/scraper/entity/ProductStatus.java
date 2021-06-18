package com.motorbesitzen.statuswatcher.bot.scraper.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The entity that contains the needed status information as received by the API.
 */
public class ProductStatus {

	@JsonAlias("name")
	private String productName;
	@JsonAlias("status")
	private String productStatus;

	public ProductStatus(@JsonProperty(value = "productName", required = true) String productName,
						 @JsonProperty(value = "productStatus", required = true) String productStatus) {
		this.productName = productName;
		this.productStatus = productStatus;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductStatus() {
		return productStatus;
	}

	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}
}
