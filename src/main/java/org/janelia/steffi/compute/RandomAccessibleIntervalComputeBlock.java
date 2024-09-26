package org.janelia.steffi.compute;

import org.janelia.steffi.metadata.SchemeCreator;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.view.Views;

public class RandomAccessibleIntervalComputeBlock<T extends NativeType<T>> implements ComputeS0Block {

	private final RandomAccessibleInterval<T> img;

	public RandomAccessibleIntervalComputeBlock(RandomAccessibleInterval<T> img) {

		this.img = img;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RandomAccessibleInterval<T> compute(long[][] gridBlock, SchemeCreator creator) {

		return Views.interval(img, FinalInterval.createMinSize(gridBlock[0], gridBlock[1]));
	}

}
