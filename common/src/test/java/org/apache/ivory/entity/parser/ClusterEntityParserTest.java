/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ivory.entity.parser;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.ivory.IvoryException;
import org.apache.ivory.entity.AbstractTestBase;
import org.apache.ivory.entity.ClusterHelper;
import org.apache.ivory.entity.v0.EntityType;
import org.apache.ivory.entity.v0.cluster.Cluster;
import org.apache.ivory.entity.v0.cluster.Interface;
import org.apache.ivory.entity.v0.cluster.Interfacetype;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ClusterEntityParserTest extends AbstractTestBase {

    private final ClusterEntityParser parser = (ClusterEntityParser) EntityParserFactory.getParser(EntityType.CLUSTER);

    @Test
    public void testParse() throws IOException, IvoryException, JAXBException {

        InputStream stream = this.getClass().getResourceAsStream(CLUSTER_XML);

        Cluster cluster = (Cluster) parser.parse(stream);
        ClusterHelper.getInterface(cluster, Interfacetype.WRITE).setEndpoint(conf.get("fs.default.name"));

        Assert.assertNotNull(cluster);
        assertEquals(cluster.getName(), "testCluster");

        Interface execute = ClusterHelper.getInterface(cluster, Interfacetype.EXECUTE);

        assertEquals(execute.getEndpoint(), "localhost:8021");
        assertEquals(execute.getVersion(), "0.20.2");

        Interface readonly = ClusterHelper.getInterface(cluster, Interfacetype.READONLY);
        assertEquals(readonly.getEndpoint(), "hftp://localhost:50010");
        assertEquals(readonly.getVersion(), "0.20.2");

        Interface write = ClusterHelper.getInterface(cluster, Interfacetype.WRITE);
        //assertEquals(write.getEndpoint(), conf.get("fs.default.name"));
        assertEquals(write.getVersion(), "0.20.2");

        Interface workflow = ClusterHelper.getInterface(cluster, Interfacetype.WORKFLOW);
        assertEquals(workflow.getEndpoint(), "http://localhost:11000/oozie/");
        assertEquals(workflow.getVersion(), "3.1");

        assertEquals(ClusterHelper.getLocation(cluster, "staging"), "/projects/ivory/staging");

        StringWriter stringWriter = new StringWriter();
        Marshaller marshaller = EntityType.CLUSTER.getMarshaller();
        marshaller.marshal(cluster, stringWriter);
        System.out.println(stringWriter.toString());
        parser.parseAndValidate(stringWriter.toString());
    }
    
    @BeforeClass
    public void init() throws Exception {
        conf.set("hadoop.log.dir", "/tmp");
        this.dfsCluster = new MiniDFSCluster(conf, 1, true, null);
    }
    
	@AfterClass
	public void tearDown() {
		this.dfsCluster.shutdown();
	}
}