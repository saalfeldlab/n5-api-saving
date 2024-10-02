package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.N5Writer;

public interface MetadataHierarchyWriter {

	public void write(final N5Writer n5, final String baseDataset);

}
