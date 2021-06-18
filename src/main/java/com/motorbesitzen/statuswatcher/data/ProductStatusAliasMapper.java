package com.motorbesitzen.statuswatcher.data;

import java.util.HashMap;

/**
 * The object that holds the aliases of the mapped status names.
 */
public class ProductStatusAliasMapper {

	private HashMap<String, String> statusAliasMapping;

	// jackson
	protected ProductStatusAliasMapper() {
	}

	public ProductStatusAliasMapper(HashMap<String, String> statusAliasMapping) {
		this.statusAliasMapping = statusAliasMapping;
	}

	/**
	 * Gets the Discord alias of a specific status name the API would respond with.
	 * @param realName The real status name the product status API uses.
	 * @return The alias of the {@param realName}
	 */
	public String getAliasOf(final String realName) {
		final String alias = statusAliasMapping.get(realName);
		if (alias == null) {
			return realName;
		}

		return alias;
	}

	public HashMap<String, String> getStatusAliasMapping() {
		return statusAliasMapping;
	}

	public void setStatusAliasMapping(HashMap<String, String> statusAliasMapping) {
		this.statusAliasMapping = statusAliasMapping;
	}
}
