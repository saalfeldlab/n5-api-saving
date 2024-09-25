package org.janelia.saalfeldlab.n5.examples;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.steffi.SaveN5Api;
import org.janelia.steffi.compute.RandomIntensityS0Block;
import org.janelia.steffi.metadata.N5ViewerScheme;

public class TestPlainN5Viewer
{
	public static void main( String[] args )
	{
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
