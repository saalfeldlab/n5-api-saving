package org.janelia.saalfeldlab.n5.config;

import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.MetadataHierarchyWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.type.NativeType;

public interface ConfigurationBuilder {

	public Parameters getParameters();

	public MetadataHierarchyWriter getWriter();

	public default ConfigurationBuilder image(
			final RandomAccessibleInterval<? extends NativeType<?>> img,
			final Axis[] axes) {

		dataType((NativeType<?>)img.getType());
		dimensions(img.dimensionsAsLongArray());
		numChannels(img, axes);
		numTimepoints(img, axes);

		if (img instanceof CellImg) {
			AbstractCellImg<?, ?, ?, ?> cellimg = (AbstractCellImg<?, ?, ?, ?>)img;
			blockSize(cellimg.getCellGrid().getCellDimensions());
		}

		return this;
	}

	public default ConfigurationBuilder dimensions(final long... dimensions) {

		getParameters().dimensions = dimensions;
		return this;
	}

	public default ConfigurationBuilder blockSize(final int... blockSize) {

		getParameters().blockSize = blockSize;
		return this;
	}

	public default ConfigurationBuilder dataType(final DataType dataType ) {

		getParameters().dataType = dataType;
		return this;
	}

	public default ConfigurationBuilder dataType(@SuppressWarnings("rawtypes") final NativeType type ) {

		@SuppressWarnings("unchecked")
		final DataType dtype = N5Utils.dataType(type);
		if (dtype != null)
			dataType(dtype);

		return this;
	}

	public default ConfigurationBuilder compression(final Compression compression ) {

		getParameters().compression = compression;
		return this;
	}

	public default ConfigurationBuilder resolution(final double... resolution) {

		getParameters().resolution = resolution;
		return this;
	}

	public default ConfigurationBuilder offset(final double... offset) {

		getParameters().offset = offset;
		return this;
	}

	public default ConfigurationBuilder unit(final String... units) {

		getParameters().units = units;
		return this;
	}

	public default ConfigurationBuilder numChannels(final int numChannels) {

		getParameters().numChannels = numChannels;
		return this;
	}

	public default ConfigurationBuilder multichannelGroups(final IntFunction<String> relativeChannelGroupName) {

		getParameters().relativeChannelGroupName = relativeChannelGroupName;
		return this;
	}

	public default ConfigurationBuilder multichannelGroups(final String... relativeChannelGroupNames) {

		getParameters().relativeChannelGroupName = c -> relativeChannelGroupNames[c];
		return this;
	}
	
	public default ConfigurationBuilder multichannelGroups(final Map<Integer,String> relativeChannelGroupNames) {

		getParameters().relativeChannelGroupName = c -> relativeChannelGroupNames.get(c);
		return this;
	}

	public default ConfigurationBuilder numScales(final int numScales) {

		getParameters().numScales = numScales;
		return this;
	}

	public default ConfigurationBuilder multiscaleGroups(final IntFunction<String> relativeScaleLevelGroupName) {

		getParameters().relativeScaleLevelGroupName = relativeScaleLevelGroupName;
		return this;
	}

	public default ConfigurationBuilder multiscaleGroups(final String... relativeScaleLevelGroupName) {

		getParameters().relativeScaleLevelGroupName = c -> relativeScaleLevelGroupName[c];
		return this;
	}

	public default ConfigurationBuilder multiscaleGroups(final Map<Integer, String> relativeScaleLevelGroupName) {

		getParameters().relativeScaleLevelGroupName = s -> relativeScaleLevelGroupName.get(s);
		return this;
	}

	public default ConfigurationBuilder downsampleFactors(IntFunction<int[]> downsamplingFactors) {

		getParameters().downsamplingFactors = downsamplingFactors;
		return this;
	}
	
	public default ConfigurationBuilder downsampleFactors(int[][] downsamplingFactors) {

		getParameters().numScales = downsamplingFactors.length;
		getParameters().downsamplingFactors = s -> downsamplingFactors[s];
		return this;
	}

	public default DatasetAttributes getBaseDatasetAttributes() {

		return new DatasetAttributes(
				getParameters().dimensions,
				getParameters().blockSize,
				getParameters().dataType,
				getParameters().compression);
	}

	public static long numChannels(final RandomAccessibleInterval<? extends NativeType<?>> img, final Axis[] axes) {

		return countElementsOfType(img, axes, Axis.CHANNEL);
	}

	public static long numTimepoints(final RandomAccessibleInterval<? extends NativeType<?>> img, final Axis[] axes) {

		return countElementsOfType(img, axes, Axis.SPACE);
	}
	
	public static long countElementsOfType(final RandomAccessibleInterval<? extends NativeType<?>> img, final Axis[] axes, final String type) {

		final int[] indexes = IntStream.range(0, axes.length)
				.filter(i -> axes[i].getType().equals(type))
				.toArray();
		if (indexes.length == 1)
			throw new N5Exception(String.format("Multiple %s axes detected, but only one allowed", type));
		else if (indexes.length == 1)
			return img.dimension(indexes[0]);
		else
			return 1;
	}

}
