package org.janelia.saalfeldlab.n5.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.MetadataHierarchyWriter;
import org.junit.Before;
import org.junit.Test;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class ConfigurationBuilderTests {

    private ConfigurationBuilderImpl builder;
    private Parameters parameters;

    // Simple implementation for testing purposes
    public class ConfigurationBuilderImpl implements ConfigurationBuilder {
        private final Parameters parameters = new Parameters();

        @Override
        public Parameters getParameters() {
            return parameters;
        }

		@Override
		public MetadataHierarchyWriter getWriter() {
			// noop
			return null;
		}
    }

    @Before
    public void setUp() {
        builder = new ConfigurationBuilderImpl();
        parameters = builder.getParameters();
    }

    @Test
    public void testDimensions() {
        long[] dimensions = {100, 200, 300};
        builder.dimensions(dimensions);

        assertArrayEquals(dimensions, parameters.dimensions);
    }

    @Test
    public void testBlockSize() {
        int[] blockSize = {64, 64, 64};
        builder.blockSize(blockSize);

        assertArrayEquals(blockSize, parameters.blockSize);
    }

    @Test
    public void testDataType() {
        DataType dataType = DataType.UINT8;
        builder.dataType(dataType);

        assertEquals(dataType, parameters.dataType);
    }

    @Test
    public void testNativeTypeDataType() {
        NativeType nativeType = new UnsignedByteType();
        DataType expectedDataType = N5Utils.dataType(nativeType);
        
        builder.dataType(nativeType);

        assertEquals(expectedDataType, parameters.dataType);
    }

    @Test
    public void testResolution() {
        double[] resolution = {0.5, 0.5, 0.5};
        builder.resolution(resolution);

        assertArrayEquals(resolution, parameters.resolution, 0.0);
    }

    @Test
    public void testOffset() {
        double[] offset = {10.0, 20.0, 30.0};
        builder.offset(offset);

        assertArrayEquals(offset, parameters.offset, 0.0);
    }

    @Test
    public void testNumChannels() {
        int numChannels = 3;
        builder.numChannels(numChannels);

        assertEquals(numChannels, parameters.numChannels);
    }

    @Test
    public void testMultichannelGroups() {
        String[] channelGroups = {"channel1", "channel2", "channel3"};
        builder.multichannelGroups(channelGroups);

        assertEquals("channel1", parameters.relativeChannelGroupName.apply(0));
        assertEquals("channel2", parameters.relativeChannelGroupName.apply(1));
        assertEquals("channel3", parameters.relativeChannelGroupName.apply(2));
    }

    @Test
    public void testNumScales() {
        int numScales = 4;
        builder.numScales(numScales);

        assertEquals(numScales, parameters.numScales);
    }

    @Test
    public void testMultiscaleGroups() {
        String[] scaleGroups = {"scale1", "scale2"};
        builder.multiscaleGroups(scaleGroups);

        assertEquals("scale1", parameters.relativeScaleLevelGroupName.apply(0));
        assertEquals("scale2", parameters.relativeScaleLevelGroupName.apply(1));
    }
}