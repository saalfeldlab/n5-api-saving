package org.janelia.saalfeldlab.n5.metadata;

import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

public class MultiscaleHierarchyWriter implements MetadataHierarchyWriter {
	
	protected final DatasetAttributes[] scaleLevelAttributes;
	protected final IntFunction<String> scaleLevelToGroup;
	
	public MultiscaleHierarchyWriter(
			final DatasetAttributes[] scaleLevelAttributes,
			final IntFunction<String> scaleLevelToGroup) {

		this.scaleLevelToGroup = scaleLevelToGroup;
		this.scaleLevelAttributes = scaleLevelAttributes;
	}

	public MultiscaleHierarchyWriter(final String baseDataset, 
			final DatasetAttributes[] scaleLevelAttributes) {

		this(scaleLevelAttributes, i -> String.format("s%d", i));
	}	

	public void write(final N5Writer n5, final String baseDataset ) {

		for (int s = 0; s < scaleLevelAttributes.length; s++) {

			// TODO consider doing this instead, but need an N5KeyValueWriter (does not include hdf5)
			// n5.getKeyValueAccess().compose(baseDataset, scaleLevelToGroup.apply(s))
			n5.createDataset(
					String.format("%s/%s", baseDataset, scaleLevelToGroup.apply(s)),
					scaleLevelAttributes[s]);
		}
	}

	public static MultiscaleHierarchyWriter build(
			final DatasetAttributes baseAttributes,
			final int[][] downsamplingFactors) {

		return build(
				baseAttributes,
				downsamplingFactors.length,
				i -> downsamplingFactors[i],
				s -> String.format("s%d", s));
	}

	public static MultiscaleHierarchyWriter build(
			final DatasetAttributes baseAttributes,
			final int[][] downsamplingFactors,
			final IntFunction<String> scaleLevelToGroup) {

		return build(
				baseAttributes,
				downsamplingFactors.length,
				i -> downsamplingFactors[i],
				scaleLevelToGroup);
	}

	public static MultiscaleHierarchyWriter build(
			final DatasetAttributes baseAttributes,
			final int numScales,
			final IntFunction<int[]> downsamplingFactors) {

		return build(baseAttributes, numScales,
				downsamplingFactors, s -> String.format("s%d", s));
	}

	public static MultiscaleHierarchyWriter build(
			final DatasetAttributes baseAttributes,
			final int numScales,
			final IntFunction<int[]> downsamplingFactors,
			final IntFunction<String> scaleLevelToGroup) {

		return new MultiscaleHierarchyWriter(
				buildAttributes(baseAttributes, numScales, downsamplingFactors),
				scaleLevelToGroup);
	}

	public static DatasetAttributes[] buildAttributes(
			final DatasetAttributes baseAttributes,
			final int numScales,
			final IntFunction<int[]> downsamplingFactors) {

		return IntStream.range(0, numScales)
				.mapToObj(s -> {
					return new DatasetAttributes(
							downsampledDimensions(baseAttributes.getDimensions(), downsamplingFactors.apply(s)),
							baseAttributes.getBlockSize(),
							baseAttributes.getDataType(),
							baseAttributes.getCompression());
				}).toArray(n -> {
					return new DatasetAttributes[n];
				});
	}

	public static long[] downsampledDimensions(final long[] baseDimensions, final int[] downsamplingFactors) {

		final long[] out = new long[baseDimensions.length];
		for (int i = 0; i < baseDimensions.length; i++) {
			out[i] = (long)Math.ceil((double)baseDimensions[i] / downsamplingFactors[i]);
		}
		return out;
	}

}
