package org.janelia.saalfeldlab.n5.examples;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.steffi.SaveN5Api;
import org.janelia.steffi.compute.RandomIntensityS0Block;
import org.janelia.steffi.metadata.SchemeCreatorBdvN5;

import mpicbg.spim.data.sequence.ViewId;

public class TestRandomData
{
	public static void main( String[] args )
	{
		final URI uri = URI.create( "/Users/preibischs/testdataset.n5" );

		final ArrayList< ViewId > viewIds = new ArrayList<>();
		viewIds.add( new ViewId(0, 0) );
		viewIds.add( new ViewId(0, 1) );

		final Map< Integer, long[] > dimensions = new HashMap<>();
		dimensions.put( viewIds.get( 0 ).getViewSetupId(), new long[] { 512, 512, 200 } );
		dimensions.put( viewIds.get( 1 ).getViewSetupId(), new long[] { 512, 256, 200 } );

		final int[] blockSize = new int[] { 128,128,64 };
		final int[][] downsamplings = new int[][] { { 1,1,1 }, { 2,2,1 }, { 4,4,2 } };

		final Map< Integer, int[] > computeBlockSize = new HashMap<>();
		for ( int s = 0; s < downsamplings.length; ++s )
			computeBlockSize.put( s, blockSize );

		final SchemeCreatorBdvN5 creator = new SchemeCreatorBdvN5(
				() -> new N5Factory().openWriter(StorageFormat.N5, uri ),
				computeBlockSize,
				viewIds,
				DataType.UINT16,
				dimensions,
				new GzipCompression( 1 ),
				blockSize,
				downsamplings );

		final SaveN5Api save = new SaveN5Api(
				creator,
				() -> new RandomIntensityS0Block() );

		save.executeMultiThreaded( 16 );
	}
}
