package org.janelia.steffi.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.steffi.compute.ResaveS0Block;
import org.janelia.steffi.jobs.Grid;
import org.janelia.steffi.multires.MultiResTools;
import org.janelia.steffi.multires.MultiResTools.MultiResolutionLevelInfo;

import mpicbg.spim.data.sequence.ViewId;

public class SchemeCreatorBdvN5 extends SchemeCreator implements Serializable
{
	private static final long serialVersionUID = 8250445911164333692L;

	final Supplier< N5Writer > n5WriterSupplier;
	final List<ViewId> viewIds;
	final DataType dataType;
	final Map< Integer, long[] > dimensions;
	final Compression compression;
	final int[] blockSize;
	final int[][] downsamplings;

	final HashMap< ViewId, MultiResolutionLevelInfo[] > viewIdToMrInfo;

	public SchemeCreatorBdvN5(
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
			viewIdToMrInfo.put( viewId, setupBdvDatasetsN5(n5, viewId, dataType, dimensions.get( viewId.getViewSetupId() ), compression, blockSize, downsamplings));

		n5.close();

		return true;
	}

	@Override
	public DataType dataType( final long[][] gridBlock, final int level ) { return dataType; }

	@Override
	public String dataset( long[][] gridBlock, final int level ) { return gridToDatasetBdv( level ).apply( gridBlock ); }

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
					computeBlockSize.get( level ),
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

	public static MultiResolutionLevelInfo[] setupBdvDatasetsN5(
			final N5Writer driverVolumeWriter,
			final ViewId viewId,
			final DataType dataType,
			final long[] dimensions,
			final Compression compression,
			final int[] blockSize,
			int[][] downsamplings )
	{
		final String s0Dataset = createBDVPathN5( viewId, 0 );
		final String setupDataset = s0Dataset.substring(0, s0Dataset.indexOf( "/timepoint" ));
		final String timepointDataset = s0Dataset.substring(0, s0Dataset.indexOf("/s0" ));

		final MultiResolutionLevelInfo[] mrInfo = MultiResTools.setupMultiResolutionPyramid(
				driverVolumeWriter,
				viewId,
				viewIdToDatasetBdv(),
				dataType,
				dimensions,
				compression,
				blockSize,
				downsamplings);

		final Map<String, Class<?>> attribs = driverVolumeWriter.listAttributes( setupDataset );

		// if viewsetup does not exist
		if ( !attribs.containsKey( DatasetAttributes.DATA_TYPE_KEY ) || !attribs.containsKey( DatasetAttributes.BLOCK_SIZE_KEY ) || !attribs.containsKey( DatasetAttributes.DIMENSIONS_KEY ) || !attribs.containsKey( DatasetAttributes.COMPRESSION_KEY ) || !attribs.containsKey( "downsamplingFactors" ) )
		{
			// set N5 attributes for setup
			// e.g. {"compression":{"type":"gzip","useZlib":false,"level":1},"downsamplingFactors":[[1,1,1],[2,2,1]],"blockSize":[128,128,32],"dataType":"uint16","dimensions":[512,512,86]}
			System.out.println( "setting attributes for '" + "setup" + viewId.getViewSetupId() + "'");

			final HashMap<String, Object > attribs2 = new HashMap<>();
			attribs2.put(DatasetAttributes.DATA_TYPE_KEY, dataType );
			attribs2.put(DatasetAttributes.BLOCK_SIZE_KEY, blockSize );
			attribs2.put(DatasetAttributes.DIMENSIONS_KEY, dimensions );
			attribs2.put(DatasetAttributes.COMPRESSION_KEY, compression );

			if ( downsamplings == null || downsamplings.length == 0 )
				attribs2.put( "downsamplingFactors", new int[][] {{1,1,1}} );
			else
				attribs2.put( "downsamplingFactors", downsamplings );

			driverVolumeWriter.setAttributes (setupDataset, attribs2 );
		}
		else
		{
			// TODO: test that the values are consistent?
		}

		// set N5 attributes for timepoint
		// e.g. {"resolution":[1.0,1.0,3.0],"saved_completely":true,"multiScale":true}
		driverVolumeWriter.setAttribute(timepointDataset, "resolution", new double[] {1,1,1} );
		driverVolumeWriter.setAttribute(timepointDataset, "saved_completely", true );
		driverVolumeWriter.setAttribute(timepointDataset, "multiScale", downsamplings != null && downsamplings.length != 0 );

		if ( downsamplings == null || downsamplings.length == 0 )
		{
			downsamplings = new int[1][ dimensions.length ];
			Arrays.setAll( downsamplings[ 0 ], i -> 1 );
		}

		// set additional N5 attributes for s0 ... sN datasets
		for ( int level = 0; level < downsamplings.length; ++level )
			driverVolumeWriter.setAttribute( mrInfo[ level ].dataset, "downsamplingFactors", mrInfo[ level ].absoluteDownsampling );

		return mrInfo;
	}

	public static String createBDVPathN5( final ViewId viewId, final int level )
	{
		return "setup" + viewId.getViewSetupId() + "/" + "timepoint" + viewId.getTimePointId() + "/s" + level;
			//path = "t" + String.format("%05d", viewId.getTimePointId()) + "/" + "s" + String.format("%02d", viewId.getViewSetupId()) + "/" + level + "/cells";
	}

	public static ViewId gridBlockToViewId( final long[][] gridBlock )
	{
		if ( gridBlock.length <= 3 )
			throw new RuntimeException( "gridBlockToViewId() needs an extended GridBlock long[][], where Gridblock[3][] encodes the ViewId");

		return new ViewId( (int)gridBlock[ 3 ][ 0 ], (int)gridBlock[ 3 ][ 1 ]);
	}

	/**
	 * @param level - the downsampling level
	 * @return a Function that maps the gridBlock to a N5 dataset name
	 */
	public static Function<long[][], String> gridToDatasetBdv( final int level )
	{
		return (gridBlock) -> viewIdToDatasetBdv( level ).apply( gridBlockToViewId( gridBlock ) );
	}

	/**
	 * @param level - the downsampling level
	 * @return a Function that maps the ViewId to a N5 dataset name
	 */
	public static Function<ViewId, String> viewIdToDatasetBdv( final int level )
	{
		return (viewId) -> createBDVPathN5( viewId, level );
	}

	/**
	 * @return a Function that maps (ViewId, level) to a N5 dataset name
	 */
	public static BiFunction<ViewId, Integer, String> viewIdToDatasetBdv()
	{
		return (viewId, level) -> viewIdToDatasetBdv( level ).apply( viewId );
	}

	/**
	 * @return a Function that maps (gridBlock, level) to a N5 dataset name
	 */
	public static BiFunction<long[][], Integer, String> gridToDatasetBdv()
	{
		return (gridBlock, level) -> gridToDatasetBdv( level ).apply( gridBlock );
	}
}
