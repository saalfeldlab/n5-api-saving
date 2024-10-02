package org.janelia.saalfeldlab.n5.metadata;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.checkerframework.checker.units.qual.C;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.GenericMetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerDatasetMetadataWriter;

import net.imglib2.realtransform.AffineTransform3D;

public class N5ViewerMultiscaleHierarchyWriter extends MultichannelHierarchyWriter {

	private static final N5ViewerDatasetMetadataWriter METADATA_WRITER = new N5ViewerDatasetMetadataWriter();

	private static final IntFunction<String> CHANNEL_PATH = c -> String.format("c%d", c);
	private static final IntFunction<String> SCALE_LEVEL_PATH = s -> String.format("s%d", s);

	private N5MetadataGroup<N5MultiScaleMetadata> metadata;

	public N5ViewerMultiscaleHierarchyWriter(
			final int numChannels,
			final DatasetAttributes[] scaleLevelAttributes,
			final N5MetadataGroup<N5MultiScaleMetadata> metadata) {

		super(numChannels, scaleLevelAttributes, CHANNEL_PATH, SCALE_LEVEL_PATH);
		this.metadata = metadata;
	}

	public void write(final N5Writer n5, final String baseDataset) {

		super.write(n5, baseDataset);

//		for( final N5MultiScaleMetadata channelMetadata : metadata.getChildrenMetadata() ) {
//			for (final N5SingleScaleMetadata meta : channelMetadata.getChildrenMetadata()) {
//				try {
//					String p = baseDataset + "/" + meta.getPath();
//					System.out.println(p);
//					METADATA_WRITER.writeMetadata(meta, n5, p);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
	}

	public static N5ViewerMultiscaleHierarchyWriter build(
			final int numChannels,
			final DatasetAttributes baseAttributes,
			final int numScales,
			final double[] baseResolution,
			final String unit,
			final IntFunction<int[]> downsamplingFactors) {

		final DatasetAttributes[] attrs = MultiscaleHierarchyWriter.buildAttributes(baseAttributes, numScales, downsamplingFactors);
		return new N5ViewerMultiscaleHierarchyWriter(
				numChannels, attrs,
				buildChannelMetadata(numChannels, attrs, baseResolution, unit, downsamplingFactors));
	}
	
	public static N5MetadataGroup<N5MultiScaleMetadata> buildChannelMetadata(
			final int numChannels,
			DatasetAttributes[] datasetAttributes,
			final double[] baseResolution,
			final String unit,
			final IntFunction<int[]> downsamplingFactors) {

		// TODO assumes all channels have identical configuration
		final N5MultiScaleMetadata[] channelMetadata = new N5MultiScaleMetadata[numChannels];
		for (int c = 0; c < numChannels; c++)
			channelMetadata[c] = buildMetadata( c, datasetAttributes, baseResolution, unit, downsamplingFactors);

		return new GenericMetadataGroup<N5MultiScaleMetadata>(unit, channelMetadata);
	}

	public static N5MultiScaleMetadata buildMetadata(
			final int channelIndex,
			final DatasetAttributes[] datasetAttributes,
			final double[] baseResolution,
			final String unit,
			final IntFunction<int[]> downsamplingFactors) {

		// TODO assumes all channels have identical configuration

		final int numScales = datasetAttributes.length;

		N5SingleScaleMetadata[] metadata = IntStream.range(0, numScales).mapToObj(s -> {
			return new N5SingleScaleMetadata(
					SCALE_LEVEL_PATH.apply(s),
					new AffineTransform3D(),
					toDouble(downsamplingFactors.apply(s)),
					baseResolution,
					new double[]{0,0,0},
					unit,
					datasetAttributes[s]);
		}).toArray(n -> new N5SingleScaleMetadata[n]);
		return new N5MultiScaleMetadata(CHANNEL_PATH.apply(channelIndex), metadata);
	}

	private static double[] toDouble(int[] in) {

		return Arrays.stream(in).mapToDouble(x -> x).toArray();
	}

}
