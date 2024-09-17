package org.janelia.steffi.compute;

import java.util.Random;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.steffi.metadata.SchemeCreator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

public class RandomIntensityS0Block implements ComputeS0Block
{
	@Override
	public <T extends NativeType<T>> RandomAccessibleInterval<T> compute(long[][] gridBlock, SchemeCreator creator)
	{
		final DataType dataType = creator.dataType( gridBlock, 0 );

		if ( dataType != DataType.UINT16 )
			throw new RuntimeException( "only UINT16 supported." );

		final Random rnd = new Random( System.currentTimeMillis() );
		RandomAccessibleInterval<UnsignedShortType> img = ArrayImgs.unsignedShorts( gridBlock[ 1 ] );
		Views.iterable( img ).forEach( v -> v.set( rnd.nextInt( 255 ) ) );

		// TODO: this is not efficient
		img = Views.translate( img, gridBlock[0] );
		return Cast.unchecked( Views.offsetInterval(img, gridBlock[0], gridBlock[1]) );
	}
}
