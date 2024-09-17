package org.janelia.steffi.compute;

import java.net.URI;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.steffi.metadata.SchemeCreator;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ResaveS0Block implements ComputeS0Block
{
	final URI xml;

	public ResaveS0Block( final URI xml ) throws SpimDataException
	{
		this.xml = xml;
	}

	@Override
	public <T extends NativeType<T>> RandomAccessibleInterval< T > compute( final long[][] gridBlock, final SchemeCreator creator )
	{
		final ViewId viewId = gridBlockToViewId( gridBlock );
		final DataType dataType = creator.dataType( gridBlock, 0 );

		if ( dataType != DataType.UINT16 && dataType != DataType.UINT8 && dataType != DataType.FLOAT32 )
			throw new RuntimeException("Unsupported pixel type: " + dataType );

		try
		{
			final SpimData spimData = new XmlIoSpimData().load( xml.toString() );

			final SetupImgLoader< ? > imgLoader = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( viewId.getViewSetupId() );
			final RandomAccessibleInterval< T > img = Cast.unchecked( imgLoader.getImage( viewId.getTimePointId() ) );

			// The code below could be generalized, that the compute method just has to fill the RandomAccessibleInterval< T >
			final RandomAccessibleInterval< T > sourceGridBlock = Views.offsetInterval( img, gridBlock[ 0 ], gridBlock[ 1 ] );

			System.out.println( "ViewId (" + viewId.getViewSetupId() + ", " + viewId.getViewSetupId() + "), written block: offset=" + Util.printCoordinates( gridBlock[0] ) + ", dimension=" + Util.printCoordinates( gridBlock[1] ) );

			return sourceGridBlock;
		}
		catch (SpimDataException e)
		{
			e.printStackTrace();
			throw new RuntimeException( "couldn't load xml and/or image." );
		}
	}

	public static ViewId gridBlockToViewId( final long[][] gridBlock )
	{
		if ( gridBlock.length <= 3 )
			throw new RuntimeException( "gridBlockToViewId() needs an extended GridBlock long[][], where Gridblock[3][] encodes the ViewId");

		return new ViewId( (int)gridBlock[ 3 ][ 0 ], (int)gridBlock[ 3 ][ 1 ]);
	}

}
