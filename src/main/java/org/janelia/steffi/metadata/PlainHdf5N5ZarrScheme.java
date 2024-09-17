package org.janelia.steffi.metadata;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.steffi.jobs.Grid;
import org.janelia.steffi.multires.MultiResTools;
import org.janelia.steffi.multires.MultiResTools.MultiResolutionLevelInfo;

public class PlainHdf5N5ZarrScheme extends SchemeCreator implements Serializable
{
	private static final long serialVersionUID = -5512644391299267180L;

	final Supplier< N5Writer > n5WriterSupplier;
	final Function<Integer, String > levelToDataset;
	final DataType dataType;
	final long[] dimensions;
	final Compression compression;
	final int[] blockSize;
	final int[][] downsamplings;

	MultiResolutionLevelInfo[] mrInfo;

	public PlainHdf5N5ZarrScheme(
			final Supplier< N5Writer > n5WriterSupplier,
			final Function<Integer, String > levelToDataset,
			final Map<Integer, int[]> computeBlockSize,
			final DataType dataType,
			final long[] dimensions,
			final Compression compression,
			final int[] blockSize,
			final int[][] downsamplings  )
	{
		super(computeBlockSize);
		this.n5WriterSupplier = n5WriterSupplier;
		this.levelToDataset = levelToDataset;
		this.dataType = dataType;
		this.dimensions = dimensions;
		this.compression = compression;
		this.blockSize = blockSize;
		this.downsamplings = downsamplings;
	}

	@Override
	public Supplier< N5Writer > getN5WriterSupplier() { return n5WriterSupplier; }

	@Override
	public boolean setup()
	{
		final N5Writer n5 = getN5WriterSupplier().get();

		// setup multi-resolution pyramid
		mrInfo = MultiResTools.setupMultiResolutionPyramid(
				n5,
				levelToDataset,
				dataType,
				dimensions,
				compression,
				blockSize,
				downsamplings );

		return true;
	}

	@Override
	public DataType dataType(long[][] gridBlock, int level) { return dataType; }

	@Override
	public String dataset(long[][] gridBlock, int level) { return levelToDataset.apply( level ); }

	@Override
	public int[] blockSize(long[][] gridBlock, int level) { return blockSize; }

	@Override
	public int[] absoluteDownsampling(long[][] gridBlock, int level) { return mrInfo[ level ].absoluteDownsampling; }

	@Override
	public int[] relativeDownsampling(long[][] gridBlock, int level) { return mrInfo[ level ].relativeDownsampling; }

	@Override
	public List<long[][]> assembleJobs(int level)
	{
		return Grid.create(
				mrInfo[ level ].dimensions,
				computeBlockSize.get( level ),
				blockSize);
	}

	@Override
	public int numDownsamplingLevels() { return downsamplings.length; }

	@Override
	public boolean finish() {
		// TODO Auto-generated method stub
		return false;
	}

}
