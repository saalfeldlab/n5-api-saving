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
import org.janelia.steffi.metadata.PlainHdf5N5ZarrScheme;

public class TestPlainHdf5N5Zarr
{
	public static void main( String[] args )
	{
		final long[] dimensions = new long[] { 512, 256, 200 };
		final int[] blockSize = new int[] { 128,128,64 };
		final int[][] downsamplings = new int[][] { { 1,1,1 }, { 2,2,1 }, { 4,4,2 } };

		final Map< Integer, int[] > computeBlockSize = new HashMap<>();
		for ( int s = 0; s < downsamplings.length; ++s )
			computeBlockSize.put( s, blockSize );

		for ( final StorageFormat sf : StorageFormat.values() )
		{
			System.out.println( "\nWriting: " + sf.toString() );

			final N5Writer n5 = new N5Factory().openWriter( sf, URI.create( "/Users/preibischs/test_simple." + sf.toString().toLowerCase() ) );

			final PlainHdf5N5ZarrScheme creator = new PlainHdf5N5ZarrScheme(
					() -> n5,
					(level) -> "myDataset/s" + level,
					computeBlockSize,
					DataType.UINT16,
					dimensions,
					new GzipCompression( 1 ),
					blockSize,
					downsamplings );
	
			final SaveN5Api save = new SaveN5Api(
					creator,
					() -> new RandomIntensityS0Block() );
	
			save.executeMultiThreaded( 16 );

			n5.close();
		}
	}

}
