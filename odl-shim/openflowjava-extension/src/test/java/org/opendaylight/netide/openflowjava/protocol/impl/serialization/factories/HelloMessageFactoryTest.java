/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.openflowjava.protocol.impl.serialization.factories;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.netide.openflowjava.protocol.impl.serialization.NetIdeSerializerRegistryImpl;
import org.opendaylight.netide.openflowjava.protocol.impl.util.BufferHelper;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloMessageBuilder;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class HelloMessageFactoryTest {
    private static final byte MESSAGE_TYPE = 0;
    HelloMessage message;

    @Before
    public void startUp() throws Exception {
        HelloMessageBuilder builder = new HelloMessageBuilder();
        BufferHelper.setupHeader(builder, EncodeConstants.OF13_VERSION_ID);
        message = builder.build();
    }

    @Test
    public void testSerialize() {
        HelloMessageFactory serializer = new HelloMessageFactory();
        SerializerRegistry registry = new NetIdeSerializerRegistryImpl();
        registry.init();
        ByteBuf serializedBuffer = UnpooledByteBufAllocator.DEFAULT.buffer();
        serializer.serialize(message, serializedBuffer);
        BufferHelper.checkHeaderV13(serializedBuffer, MESSAGE_TYPE, 8);
    }

}
