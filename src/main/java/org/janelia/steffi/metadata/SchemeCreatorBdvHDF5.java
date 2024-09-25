package org.janelia.steffi.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.steffi.compute.ResaveS0Block;
import org.janelia.steffi.jobs.Grid;
import org.janelia.steffi.multires.MultiResTools;
import org.janelia.steffi.multires.MultiResTools.MultiResolutionLevelInfo;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;

public class SchemeCreatorBdvHDF5 extends SchemeCreator implements Serializable
{
	private static final long serialVersionUID = 7632546753134637245L;

	final Supplier< N5Writer > n5WriterSupplier;
	final List<ViewId> viewIds;
	final DataType dataType;
	final Map< Integer, long[] > dimensions;
	final Compression compression;
	final int[] blockSize;
	final int[][] downsamplings;

	final HashMap< ViewId, MultiResolutionLevelInfo[] > viewIdToMrInfo;

	public SchemeCreatorBdvHDF5(
			final Supplier< N5Writer > n5WriterSupplier,
			final Map< Integer, int[] > computeBlockSize,
			final List<ViewId> viewIds,
			final DataType dataType,
			final Map< Integer, long[] > dimensions,
			final Compression compression,
			final int[] blockSize,
			final int[][] downsamplings )
	{
		super( computeBlockSize );
		this.n5WriterSupplier = n5WriterSupplier;
		this.viewIds = viewIds;
		this.dataType = dataType;
		this.dimensions = dimensions;
		this.compression = compression;
		this.blockSize = blockSize;
		this.downsamplings = downsamplings;

		this.viewIdToMrInfo = new HashMap<>();
	}

	@Override
	public Supplier< N5Writer > getN5WriterSupplier() { return n5WriterSupplier; }

	@Override
	public boolean setup()
	{
		final N5Writer n5 = getN5WriterSupplier().get();

		for ( final ViewId viewId : viewIds )
			viewIdToMrInfo.put( viewId, setupBdvDatasetsHDF5(n5, viewId, dataType, dimensions.get( viewId.getViewSetupId() ), compression, blockSize, downsamplings) );

		return true;
	}

	@Override
	public DataType dataType( long[][] gridBlock, final int level ) { return dataType; }

	@Override
	public String dataset( long[][] gridBlock, final int level )
	{
		return gridToDatasetBdv( level ).apply( gridBlock );
	}

	@Override
	public int numDownsamplingLevels() { return downsamplings.length; }

	@Override
	public int[] blockSize( long[][] gridBlock, int level ) { return blockSize; }

	@Override
	public int[] absoluteDownsampling( long[][] gridBlock, int level )
	{
		return viewIdToMrInfo.get( ResaveS0Block.gridBlockToViewId( gridBlock ) )[ level ].absoluteDownsampling;
	}

	@Override
	public int[] relativeDownsampling( long[][] gridBlock, int level )
	{
		return viewIdToMrInfo.get( ResaveS0Block.gridBlockToViewId( gridBlock ) )[ level ].relativeDownsampling;
	}

	@Override
	public ArrayList<long[][]> assembleJobs( final int level )
	{
		// all blocks (a.k.a. grids)
		final ArrayList<long[][]> allBlocks = new ArrayList<>();
	
		for ( final ViewId viewId : viewIds )
		{
			final List<long[][]> grid = Grid.create(
					viewIdToMrInfo.get( viewId )[ level ].dimensions,
					blockSizePerLevel.get( level ),
					blockSize);
	
			// add timepointId and ViewSetupId & dimensions to the gridblock
			for ( final long[][] gridBlock : grid )
				allBlocks.add( new long[][]{
					gridBlock[ 0 ].clone(),
					gridBlock[ 1 ].clone(),
					gridBlock[ 2 ].clone(),
					new long[] { viewId.getTimePointId(), viewId.getViewSetupId() }
				});
		}

		return allBlocks;
	}

	@Override
	public boolean finish()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public static MultiResolutionLevelInfo[] setupBdvDatasetsHDF5(
			final N5Writer driverVolumeWriter,
			final ViewId viewId,
			final DataType dataType,
			final long[] dimensions,
			final Compression compression,
			final int[] blockSize,
			final int[][] downsamplings )
	{
		final MultiResolutionLevelInfo[] mrInfo = MultiResTools.setupMultiResolutionPyramid(
				driverVolumeWriter,
				viewId,
				viewIdToDatasetBdv(),
				dataType,
				dimensions,
				compression,
				blockSize,
				downsamplings);

		final String subdivisionsDatasets = "s" + String.format("%02d", viewId.getViewSetupId()) + "/subdivisions";
		final String resolutionsDatasets = "s" + String.format("%02d", viewId.getViewSetupId()) + "/resolutions";

		if ( !driverVolumeWriter.datasetExists( subdivisionsDatasets ) || !driverVolumeWriter.datasetExists( resolutionsDatasets ) )
		{
			final Img<IntType> subdivisions;
			final Img<DoubleType> resolutions;
	
			if ( downsamplings == null || downsamplings.length == 0 )
			{
				subdivisions = ArrayImgs.ints( blockSize, new long[] { 3, 1 } ); // blocksize
				resolutions = ArrayImgs.doubles( new double[] { 1,1,1 }, new long[] { 3, 1 } ); // downsampling
			}
			else
			{
				final int[] blocksizes = new int[ 3 * downsamplings.length ];
				final double[] downsamples = new double[ 3 * downsamplings.length ];
	
				int i = 0;
				for ( int level = 0; level < downsamplings.length; ++level )
				{
					downsamples[ i ] = downsamplings[ level ][ 0 ];
					blocksizes[ i++ ] = blockSize[ 0 ];
					downsamples[ i ] = downsamplings[ level ][ 1 ];
					blocksizes[ i++ ] = blockSize[ 1 ];
					downsamples[ i ] = downsamplings[ level ][ 2 ];
					blocksizes[ i++ ] = blockSize[ 2 ];
				}
	
				subdivisions = ArrayImgs.ints( blocksizes, new long[] { 3, downsamplings.length } ); // blocksize
				resolutions = ArrayImgs.doubles( downsamples, new long[] { 3, downsamplings.length } ); // downsampling
			}
			
			driverVolumeWriter.createDataset(
					subdivisionsDatasets,
					subdivisions.dimensionsAsLongArray(),// new long[] { 3, 1 },
					new int[] { (int)subdivisions.dimension( 0 ), (int)subdivisions.dimension( 1 ) }, //new int[] { 3, 1 },
					DataType.INT32,
					new RawCompression() );
	
			driverVolumeWriter.createDataset(
					resolutionsDatasets,
					resolutions.dimensionsAsLongArray(),// new long[] { 3, 1 },
					new int[] { (int)resolutions.dimension( 0 ), (int)resolutions.dimension( 1 ) },//new int[] { 3, 1 },
					DataType.FLOAT64,
					new RawCompression() );
	
			N5Utils.saveBlock(subdivisions, driverVolumeWriter, "s" + String.format("%02d", viewId.getViewSetupId()) + "/subdivisions", new long[] {0,0,0} );
			N5Utils.saveBlock(resolutions, driverVolumeWriter, "s" + String.format("%02d", viewId.getViewSetupId()) + "/resolutions", new long[] {0,0,0} );
		}

		return mrInfo;
	}

	public static ViewId gridBlockToViewId( final long[][] gridBlock )
	{
		if ( gridBlock.length <= 3 )
			throw new RuntimeException( "gridBlockToViewId() needs an extended GridBlock long[][], where Gridblock[3][] encodes the ViewId");

		return new ViewId( (int)gridBlock[ 3 ][ 0 ], (int)gridBlock[ 3 ][ 1 ]);
	}

	/**
	 * @param level - the downsampling level
	 * @return a Function that maps the gridBlock to a HDF5 dataset name
	 */
	public static Function<long[][], String> gridToDatasetBdv( final int level )
	{
		return (gridBlock) -> viewIdToDatasetBdv( level ).apply( gridBlockToViewId( gridBlock ) );
	}

	/**
	 * @param level - the downsampling level
	 * @return a Function that maps the ViewId to a HDF5 dataset name
	 */
	public static Function<ViewId, String> viewIdToDatasetBdv( final int level )
	{
		return (viewId) -> createBDVPathHdf5( viewId, level );
	}

	/**
	 * @return a Function that maps (ViewId, level) to a HDF5 dataset name
	 */
	public static BiFunction<ViewId, Integer, String> viewIdToDatasetBdv()
	{
		return (viewId, level) -> viewIdToDatasetBdv( level ).apply( viewId );
	}

	/**
	 * @return a Function that maps (gridBlock, level) to a HDF5 dataset name
	 */
	public static BiFunction<long[][], Integer, String> gridToDatasetBdv()
	{
		return (gridBlock, level) -> gridToDatasetBdv( level ).apply( gridBlock );
	}

	public static String createBDVPathHdf5( final ViewId viewId, final int level )
	{
		return "t" + String.format("%05d", viewId.getTimePointId()) + "/" + "s" + String.format("%02d", viewId.getViewSetupId()) + "/" + level + "/cells";
	}
}
