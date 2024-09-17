package org.janelia.steffi.multires;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.steffi.metadata.SchemeCreator;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class MultiResTools
{
	public static class MultiResolutionLevelInfo implements Serializable
	{
		private static final long serialVersionUID = 5392269335394869108L;

		final public int[] relativeDownsampling, absoluteDownsampling, blockSize;
		final public long[] dimensions;
		final public String dataset;
		final public DataType dataType;

		public MultiResolutionLevelInfo(
				final String dataset,
				final long[] dimensions,
				final DataType dataType,
				final int[] relativeDownsampling,
				final int[] absoluteDownsampling,
				final int[] blockSize )
		{
			this.dataset = dataset;
			this.dimensions = dimensions;
			this.dataType = dataType;
			this.relativeDownsampling = relativeDownsampling;
			this.absoluteDownsampling = absoluteDownsampling;
			this.blockSize = blockSize;
		}
	}

	public static MultiResolutionLevelInfo[] setupMultiResolutionPyramid(
			final N5Writer driverVolumeWriter,
			final Function<Integer, String> levelToDataset,
			final DataType dataType,
			final long[] dimensionsS0,
			final Compression compression,
			final int[] blockSize,
			final int[][] downsamplings )
	{
		return setupMultiResolutionPyramid(
				driverVolumeWriter,
				null,
				(viewId, level) -> levelToDataset.apply( level ),
				dataType,
				dimensionsS0,
				compression,
				blockSize,
				downsamplings);
	}

	public static MultiResolutionLevelInfo[] setupMultiResolutionPyramid(
			final N5Writer driverVolumeWriter,
			final ViewId viewId,
			final BiFunction<ViewId, Integer, String> viewIdToDataset,
			final DataType dataType,
			final long[] dimensionsS0,
			final Compression compression,
			final int[] blockSize,
			final int[][] downsamplings )
	{
		final MultiResolutionLevelInfo[] mrInfo = new MultiResolutionLevelInfo[ downsamplings.length];

		// set up s0
		int[] relativeDownsampling = downsamplings[ 0 ].clone();
		Arrays.setAll( relativeDownsampling, i -> 1 );

		mrInfo[ 0 ] = new MultiResolutionLevelInfo(
				viewIdToDataset.apply( viewId, 0 ), dimensionsS0.clone(), dataType, relativeDownsampling, downsamplings[ 0 ], blockSize );

		driverVolumeWriter.createDataset(
				viewIdToDataset.apply( viewId, 0 ),
				dimensionsS0,
				blockSize,
				dataType,
				compression );

		long[] previousDim = dimensionsS0.clone();

		// set up s1 ... sN
		for ( int level = 1; level < downsamplings.length; ++level )
		{
			relativeDownsampling = computeRelativeDownsampling( downsamplings, level );

			final String datasetLevel = viewIdToDataset.apply( viewId, level );

			final long[] dim = new long[ previousDim.length ];
			for ( int d = 0; d < dim.length; ++d )
				dim[ d ] = previousDim[ d ] / relativeDownsampling[ d ];

			mrInfo[ level ] = new MultiResolutionLevelInfo(
					datasetLevel, dim.clone(), dataType, relativeDownsampling, downsamplings[ level ], blockSize );

			driverVolumeWriter.createDataset(
					datasetLevel,
					dim,
					blockSize,
					dataType,
					compression );

			previousDim = dim;
		}

		return mrInfo;
	}

	public static int[] computeRelativeDownsampling(
			final int[][] downsamplings,
			final int level )
	{
		final int[] ds = new int[ downsamplings[ 0 ].length ];

		for ( int d = 0; d < ds.length; ++d )
			ds[ d ] = downsamplings[ level ][ d ] / downsamplings[ level - 1 ][ d ];

		return ds;
	}

	public static void writeDownsampledBlock(
			final SchemeCreator creator,
			final int level,
			final int previousLevel,
			final long[][] gridBlock )
	{
		final N5Writer n5 = creator.getN5WriterSupplier().get();

		final String dataset = creator.dataset( gridBlock, level );
		final String datasetPreviousScale = creator.dataset( gridBlock, previousLevel );

		final DataType dataType = creator.dataType( gridBlock, level );

		final int[] computeBlockSize = new int[ gridBlock[ 1 ].length ];
		for ( int d = 0; d < computeBlockSize.length; ++d )
			computeBlockSize[ d ] = (int)gridBlock[ 1 ][ d ];

		if ( dataType == DataType.UINT16 )
		{
			RandomAccessibleInterval<UnsignedShortType> downsampled = N5Utils.open(n5, datasetPreviousScale);

			for ( int d = 0; d < downsampled.numDimensions(); ++d )
				if ( creator.relativeDownsampling( gridBlock, level )[ d ] > 1 )
					downsampled = LazyHalfPixelDownsample2x.init(
						downsampled,
						new FinalInterval( downsampled ),
						new UnsignedShortType(),
						computeBlockSize,
						d);

			final RandomAccessibleInterval<UnsignedShortType> sourceGridBlock = Views.offsetInterval(downsampled, gridBlock[0], gridBlock[1]);
			N5Utils.saveNonEmptyBlock(sourceGridBlock, n5, dataset, gridBlock[2], new UnsignedShortType());
		}
		else if ( dataType == DataType.UINT8 )
		{
			RandomAccessibleInterval<UnsignedByteType> downsampled = N5Utils.open(n5, datasetPreviousScale);

			for ( int d = 0; d < downsampled.numDimensions(); ++d )
				if ( creator.relativeDownsampling( gridBlock, level )[ d ] > 1 )
					downsampled = LazyHalfPixelDownsample2x.init(
						downsampled,
						new FinalInterval( downsampled ),
						new UnsignedByteType(),
						computeBlockSize,
						d);

			final RandomAccessibleInterval<UnsignedByteType> sourceGridBlock = Views.offsetInterval(downsampled, gridBlock[0], gridBlock[1]);
			N5Utils.saveNonEmptyBlock(sourceGridBlock, n5, dataset, gridBlock[2], new UnsignedByteType());
		}
		else if ( dataType == DataType.FLOAT32 )
		{
			RandomAccessibleInterval<FloatType> downsampled = N5Utils.open(n5, datasetPreviousScale);;

			for ( int d = 0; d < downsampled.numDimensions(); ++d )
				if ( creator.relativeDownsampling( gridBlock, level )[ d ] > 1 )
					downsampled = LazyHalfPixelDownsample2x.init(
						downsampled,
						new FinalInterval( downsampled ),
						new FloatType(),
						computeBlockSize,
						d);

			final RandomAccessibleInterval<FloatType> sourceGridBlock = Views.offsetInterval(downsampled, gridBlock[0], gridBlock[1]);
			N5Utils.saveNonEmptyBlock(sourceGridBlock, n5, dataset, gridBlock[2], new FloatType());
		}
		else
		{
			n5.close();
			throw new RuntimeException("Unsupported pixel type: " + dataType );
		}
	}
}
