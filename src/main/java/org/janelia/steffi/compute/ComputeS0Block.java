package org.janelia.steffi.compute;

import org.janelia.steffi.metadata.SchemeCreator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;

@FunctionalInterface
public interface ComputeS0Block
{
	<T extends NativeType<T>> RandomAccessibleInterval< T > compute( long[][] gridBlock, SchemeCreator creator );
}
