package org.janelia.steffi.metadata;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.steffi.jobs.Grid;
import org.janelia.steffi.multires.MultiResTools;
import org.janelia.steffi.multires.MultiResTools.MultiResolutionLevelInfo;

import net.imglib2.realtransform.AffineTransform3D;

public class N5ViewerScheme extends SchemeCreator implements Serializable
{

	private static final long serialVersionUID = -5512644391299267180L;

	private static final AffineTransform3D IDENTITY = new AffineTransform3D();

	final Supplier< N5Writer > n5WriterSupplier;
	final Function<Integer, String > levelToDataset;
	final DataType dataType;
	final long[] dimensions;
	final Compression compression;
	final int[] blockSize;
	final double[] baseResolution;
	final int[][] downsamplings;

	MultiResolutionLevelInfo[] mrInfo;

	private String unit;

	public N5ViewerScheme(
			final Supplier< N5Writer > n5WriterSupplier,
			final Function<Integer, String > levelToDataset,
			final Map<Integer, int[]> blockSizePerLevel,
			final DataType dataType,
			final long[] dimensions,
			final Compression compression,
			final int[] blockSize,
			final double[] baseResolution,
			final int[][] downsamplings,
			final String unit)
	{
		super(blockSizePerLevel);
		this.n5WriterSupplier = n5WriterSupplier;
		this.levelToDataset = levelToDataset;
		this.dataType = dataType;
		this.dimensions = dimensions;
		this.compression = compression;
		this.blockSize = blockSize;
		this.baseResolution = baseResolution;
		this.downsamplings = downsamplings;
		this.unit = unit;
	}

	@Override
	public Supplier< N5Writer > getN5WriterSupplier() { return n5WriterSupplier; }

	@Override
	public boolean setup() {

		final int nd = dimensions.length;
		final N5SingleScaleMetadataParser metadataWriter = new N5SingleScaleMetadataParser();
		
		final AffineTransform3D n5vTransform = new AffineTransform3D();
		n5vTransform.scale(baseResolution[0], baseResolution[1], baseResolution[2]);

		final N5Writer n5 = getN5WriterSupplier().get();

		final String dsetS0 = levelToDataset.apply(0);
		final DatasetAttributes s0Attrs = new DatasetAttributes(dimensions, blockSize, dataType, compression);

		n5.createDataset(dsetS0, s0Attrs);
		final N5SingleScaleMetadata metadataS0 = new N5SingleScaleMetadata(
				dsetS0,
				n5vTransform,
				toDouble(downsamplings[0]),
				baseResolution,
				new double[nd], // zero offset
				unit,
				s0Attrs);
		
		try {
			metadataWriter.writeMetadata(metadataS0, n5, dsetS0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		final long[] previousDim = dimensions.clone();

		int[] relativeDownsampling = new int[nd];
		Arrays.fill(relativeDownsampling, 1);
		
		mrInfo = new MultiResolutionLevelInfo[downsamplings.length];
		mrInfo[ 0 ] = new MultiResolutionLevelInfo( 
				dsetS0, dimensions, dataType, relativeDownsampling, downsamplings[ 0 ], blockSize);

		for (int level = 1; level < downsamplings.length; ++level) {

			final long[] dim = new long[previousDim.length];
			for (int d = 0; d < dim.length; ++d)
				dim[d] = (long)Math.ceil(((double)previousDim[d]) / downsamplings[level][d]);

			final String dsetLevel = levelToDataset.apply(level);
			final DatasetAttributes levelAttrs = new DatasetAttributes(dim, blockSize, dataType, compression);

			n5.createDataset(dsetLevel, levelAttrs);

			final N5SingleScaleMetadata metadataLevel = new N5SingleScaleMetadata(
					dsetLevel,
					n5vTransform,
					toDouble(downsamplings[level]),
					baseResolution,
					new double[nd], // zero offset
					unit,
					levelAttrs);
			
			relativeDownsampling = MultiResTools.computeRelativeDownsampling( downsamplings, level );
			mrInfo[ level ] = new MultiResolutionLevelInfo( 
					dsetLevel, dim, dataType, relativeDownsampling, downsamplings[ level ], blockSize);

			try {
				metadataWriter.writeMetadata(metadataLevel, n5, dsetLevel);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return true;
	}
	
	public static double[] computeRelativeDownsampling(
			final double[][] downsamplings,
			final int level) {
		
		assert (level > 0);

		final double[] ds = new double[downsamplings[0].length];
		for (int d = 0; d < ds.length; ++d)
			ds[d] = downsamplings[level][d] / downsamplings[level - 1][d];

		return ds;
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
				blockSizePerLevel.get( level ),
				blockSize);
	}

	@Override
	public int numDownsamplingLevels() { return downsamplings.length; }

	@Override
	public boolean finish() {
		// TODO Auto-generated method stub
		return false;
	}

	private static double[] toDouble(int[] x) {

		return Arrays.stream(x).mapToDouble(i -> i).toArray();
	}

}
