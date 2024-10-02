package org.janelia.saalfeldlab.n5.config;

import org.janelia.saalfeldlab.n5.metadata.MetadataHierarchyWriter;

public abstract class AbstractConfigurationBuilder implements ConfigurationBuilder {

	protected Parameters parameters;

	public AbstractConfigurationBuilder() {

		parameters = new Parameters();
	}

	@Override
	public Parameters getParameters() {

		return parameters;
	}

	@Override
	public abstract MetadataHierarchyWriter getWriter();

}
