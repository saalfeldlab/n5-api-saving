package org.janelia.saalfeldlab.n5.examples;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.steffi.SaveN5Api;
import org.janelia.steffi.compute.RandomAccessibleIntervalComputeBlock;
import org.janelia.steffi.compute.RandomIntensityS0Block;
import org.janelia.steffi.metadata.N5ViewerScheme;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class TestPlainN5Viewer {

	public static void main(String[] args) {

		// testN5VRandom();
		testN5VFromRaiMultichannel();
	}

	public static void testN5VFromRaiMultichannel() {

		final int CHANNEL_DIM = 2;

		final ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs.unsignedBytes(32, 32, 3, 16);
		Views.hyperSlice(img, CHANNEL_DIM, 1).forEach(x -> x.set(1));
		Views.hyperSlice(img, CHANNEL_DIM, 2).forEach(x -> x.set(2));

		final long nChannels = img.dimension(CHANNEL_DIM);
		final long[] dimensions = Views.hyperSlice(img, CHANNEL_DIM, 0).dimensionsAsLongArray();
		final int[] blockSize = new int[]{128, 128, 64};

		final double[] baseResolution = new double[]{1.1, 2.2, 3.3};
		final int[][] downsamplings = new int[][]{{1, 1, 1}, {2, 2, 1}, {4, 4, 2}};
		final String unit = "um";

		final Map< Integer, int[] > blockSizePerLevel = new HashMap<>();
		for ( int s = 0; s < downsamplings.length; ++s )
			blockSizePerLevel.put( s, blockSize );

		final StorageFormat sf = StorageFormat.N5;
		final N5Writer n5 = new N5Factory().openWriter( sf,
				URI.create( "/home/john/tests/n5-api-saving/test-n5v-mc." + sf.toString().toLowerCase() ) );


		Function<Integer,N5ViewerScheme> creatorForChannel = (c) ->  {
			return new N5ViewerScheme(
				() -> n5,
				(level) -> String.format("myDataset/c%d/s%d", c, level),
				blockSizePerLevel,
				DataType.UINT8,
				dimensions,
				new GzipCompression(),
				blockSize,
				baseResolution,
				downsamplings,
				unit);
		};

		final Function<Integer,RandomAccessibleIntervalComputeBlock<UnsignedByteType>> blockPerChannel = (c) ->  {
			return new RandomAccessibleIntervalComputeBlock<UnsignedByteType>(
				Views.dropSingletonDimensions(Views.hyperSlice(img, 2, c)));
		};

		for( int i = 0; i < nChannels; i++ )
		{
			final int c = i;
			final SaveN5Api save = new SaveN5Api(
					creatorForChannel.apply(c),
					() -> blockPerChannel.apply(c));

			save.executeMultiThreaded(8);
		}

	}

	public static void testN5VRandom() {

		final long[] dimensions = new long[] { 512, 256, 200 };
		final int[] blockSize = new int[] { 128,128,64 };

		final double[] baseResolution = new double[] { 1.1, 2.2, 3.3 };
		final int[][] downsamplings = new int[][] { { 1,1,1 }, { 2,2,1 }, { 4,4,2 } };
		final String unit = "um";

		final Map< Integer, int[] > blockSizePerLevel = new HashMap<>();
		for ( int s = 0; s < downsamplings.length; ++s )
			blockSizePerLevel.put( s, blockSize );

		final StorageFormat sf = StorageFormat.N5;
		final N5Writer n5 = new N5Factory().openWriter( sf, 
				URI.create( "/home/john/tests/n5-api-saving/test-n5v." + sf.toString().toLowerCase() ) );

		final N5ViewerScheme creator = new N5ViewerScheme(
				() -> n5,
				(level) -> "myDataset/c0/s" + level,
				blockSizePerLevel,
				DataType.UINT16,
				dimensions,
				new GzipCompression( 1 ),
				blockSize,
				baseResolution,
				downsamplings,
				unit);
			
		final SaveN5Api save = new SaveN5Api(
				creator,
				() -> new RandomIntensityS0Block());

		save.executeMultiThreaded(16);
	}

}
