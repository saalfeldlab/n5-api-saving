package org.janelia.saalfeldlab.n5.config;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultiscaleHierarchyWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;

import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class N5ViewerBuilder extends AbstractConfigurationBuilder {

	public N5ViewerBuilder() {

		super();
	}

	@Override
	public N5ViewerMultiscaleHierarchyWriter getWriter() {

		// TODO check that all units are equal?
		return N5ViewerMultiscaleHierarchyWriter.build(
				getParameters().numChannels,
				getBaseDatasetAttributes(),
				getParameters().numScales,
				getParameters().resolution,
				getParameters().units[0],
				getParameters().downsamplingFactors);
	}

	@Override
	public ConfigurationBuilder offset(final double[] offset) {

		throw new N5Exception("N5Viewer does not support offset");
	}

	public static void main(String[] args) {

		final N5ViewerBuilder builder = new N5ViewerBuilder();
		builder.image(
				new CellImgFactory<UnsignedByteType>(new UnsignedByteType()).create(4, 3, 2),
				AxisUtils.buildAxes("x", "y", "z"));
		System.out.println(builder.parameters);

		// throws exception
		// builder.offset(new double[]{2, 3, 4});
	}

}
