// Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior University
// Copyright (c) 2011, 2012 Open Networking Foundation
// Copyright (c) 2012, 2013 Big Switch Networks, Inc.
// This library was generated by the LoxiGen Compiler.
// See the file LICENSE.txt which should have been included in the source distribution

// Automatically generated by LOXI from template unit_test.java
// Do not modify

package org.projectfloodlight.openflow.protocol.ver13;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.actionid.*;
import org.projectfloodlight.openflow.protocol.bsntlv.*;
import org.projectfloodlight.openflow.protocol.errormsg.*;
import org.projectfloodlight.openflow.protocol.meterband.*;
import org.projectfloodlight.openflow.protocol.instruction.*;
import org.projectfloodlight.openflow.protocol.instructionid.*;
import org.projectfloodlight.openflow.protocol.match.*;
import org.projectfloodlight.openflow.protocol.oxm.*;
import org.projectfloodlight.openflow.protocol.queueprop.*;
import org.projectfloodlight.openflow.types.*;
import org.projectfloodlight.openflow.util.*;
import org.projectfloodlight.openflow.exceptions.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.hamcrest.CoreMatchers;



public class OFActionIdOutputVer13Test {
    OFActionIds factory;

    final static byte[] ACTION_ID_OUTPUT_SERIALIZED =
        new byte[] { 0x0, 0x0, 0x0, 0x4 };

    @Before
    public void setup() {
        factory = OFActionIdsVer13.INSTANCE;
    }

    @Test
    public void testWrite() {
        OFActionIdOutput actionIdOutput = factory.output();
        ChannelBuffer bb = ChannelBuffers.dynamicBuffer();
        actionIdOutput.writeTo(bb);
        byte[] written = new byte[bb.readableBytes()];
        bb.readBytes(written);

        assertThat(written, CoreMatchers.equalTo(ACTION_ID_OUTPUT_SERIALIZED));
    }

    @Test
    public void testRead() throws Exception {
        OFActionIdOutput actionIdOutputBuilt = factory.output();

        ChannelBuffer input = ChannelBuffers.copiedBuffer(ACTION_ID_OUTPUT_SERIALIZED);

        // FIXME should invoke the overall reader once implemented
        OFActionIdOutput actionIdOutputRead = OFActionIdOutputVer13.READER.readFrom(input);
        assertEquals(ACTION_ID_OUTPUT_SERIALIZED.length, input.readerIndex());

        assertEquals(actionIdOutputBuilt, actionIdOutputRead);
   }

   @Test
   public void testReadWrite() throws Exception {
       ChannelBuffer input = ChannelBuffers.copiedBuffer(ACTION_ID_OUTPUT_SERIALIZED);

       // FIXME should invoke the overall reader once implemented
       OFActionIdOutput actionIdOutput = OFActionIdOutputVer13.READER.readFrom(input);
       assertEquals(ACTION_ID_OUTPUT_SERIALIZED.length, input.readerIndex());

       // write message again
       ChannelBuffer bb = ChannelBuffers.dynamicBuffer();
       actionIdOutput.writeTo(bb);
       byte[] written = new byte[bb.readableBytes()];
       bb.readBytes(written);

       assertThat(written, CoreMatchers.equalTo(ACTION_ID_OUTPUT_SERIALIZED));
   }

}