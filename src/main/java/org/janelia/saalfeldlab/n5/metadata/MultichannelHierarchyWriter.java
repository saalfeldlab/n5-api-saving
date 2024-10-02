package org.janelia.saalfeldlab.n5.metadata;

import java.util.function.IntFunction;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

public class MultichannelHierarchyWriter implements MetadataHierarchyWriter {
	
	protected final DatasetAttributes[] scaleLevelAttributes;
	protected final IntFunction<String> channelToGroup;
	protected final IntFunction<String> scaleLevelToGroup;
	
	protected int numChannels;
	
	public MultichannelHierarchyWriter(
			final int numChannels,
			final DatasetAttributes[] scaleLevelAttributes,
			final IntFunction<String> channelToGroup,
			final IntFunction<String> scaleLevelToGroup) {

		this.numChannels = numChannels;
		this.channelToGroup = channelToGroup;
		this.scaleLevelToGroup = scaleLevelToGroup;
		this.scaleLevelAttributes = scaleLevelAttributes;
	}

	public void write(final N5Writer n5, final String baseDataset ) {

		for (int c = 0; c < numChannels; c++) {
			for (int s = 0; s < scaleLevelAttributes.length; s++) {

				// TODO consider doing this instead, but need an
				// N5KeyValueWriter (does not include hdf5)
				// n5.getKeyValueAccess().compose(baseDataset,
				// scaleLevelToGroup.apply(s))
				n5.createDataset(
						String.format("%s/%s/%s", 
								baseDataset, 
								channelToGroup.apply(c),
								scaleLevelToGroup.apply(s)),
						scaleLevelAttributes[s]);
			}
		}
	}

}
